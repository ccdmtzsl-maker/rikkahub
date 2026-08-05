package me.rerere.rikkahub.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import androidx.core.app.NotificationCompat
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import kotlin.uuid.Uuid
import kotlinx.coroutines.withTimeout
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID
import me.rerere.rikkahub.RouteActivity
import me.rerere.rikkahub.data.db.dao.ConversationDAO
import me.rerere.rikkahub.data.model.toMessageNode
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.utils.sendNotification
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import me.rerere.rikkahub.utils.applyPlaceholders
import me.rerere.ai.core.MessageRole

private const val TAG = "ProactiveRunner"

/**
 * 主动消息执行器：
 * - AlarmReceiver 和 Worker 都可以调用它
 * - 里面是完整的一轮：概率判定 → 生成 → 写库 → 通知 → 安排下一次闹钟
 */
object ProactiveGenerationRunner : KoinComponent {

    private val conversationDao: ConversationDAO by inject()
    private val conversationRepo: ConversationRepository by inject()
    private val chatService: ChatService by inject()

    private val settingsStore: SettingsStore by inject()   // ✅ 新增

    // 主动消息默认模板（用户没填自定义时用这个）
// 支持占位符：{currentTime} {timeSinceLastAssistantMessage} {timeSinceLastUserMessage} {batteryLevel}
    private val defaultProactiveTemplate = """
现在时间：{currentTime}
距离你上次回复用户：{timeSinceLastAssistantMessage}
距离用户上次发消息给你：{timeSinceLastUserMessage}
当前设备电量：{batteryLevel}
请根据这些信息，主动给用户发一条自然、亲切的消息，不要解释你是如何知道这些信息的。
""".trimIndent()

    /**
     * 执行一次主动消息流程。
     *
     * @return true 表示正常结束（不管这次有没有发消息），false 表示执行中抛异常。
     */
    suspend fun run(
        context: Context,
        conversationId: String,
    ): Boolean {
        val entity = conversationDao.getConversationById(conversationId)
        if (entity == null || !entity.receiveProactiveMessages) {
            Log.i(TAG, "Proactive disabled for $conversationId, skip")
            return true
        }

        // 快照当前配置，整轮都用它
        val minIntervalSnapshot = entity.minProactiveInterval
        val maxIntervalSnapshot = entity.maxProactiveInterval

        return try {
            Log.i(
                TAG,
                "Entity state for $conversationId: " +
                    "min=${minIntervalSnapshot / 60_000}min, " +
                    "max=${maxIntervalSnapshot / 60_000}min, " +
                    "lastActive=${entity.lastActiveTime}, " +
                    "next=${entity.nextProactiveTime}"
            )

            val now = System.currentTimeMillis()
            val sinceLast = now - entity.lastActiveTime

            // 基础间隔：用快照 min/max 的平均值
            val baseIntervalMs = when {
                minIntervalSnapshot > 0L && maxIntervalSnapshot > 0L ->
                    (minIntervalSnapshot + maxIntervalSnapshot) / 2
                minIntervalSnapshot > 0L -> minIntervalSnapshot
                maxIntervalSnapshot > 0L -> maxIntervalSnapshot
                else -> 60L * 60_000L // 默认 1 小时
            }

            // 1) 还没到基础间隔：本次不发，只排到恰好到 base 的时刻再检查
            if (sinceLast < baseIntervalMs) {
                val remaining = baseIntervalMs - sinceLast
                val remainingMin = remaining / 60_000L
                Log.i(
                    TAG,
                    "Too early for $conversationId, ${remainingMin} minutes remaining until base interval"
                )
                scheduleNext(
                    context = context,
                    conversationId = conversationId,
                    minInterval = minIntervalSnapshot,
                    maxInterval = maxIntervalSnapshot,
                    delayMs = remaining
                )
                return true
            }

            // 2) 超过基础间隔：概率递增
            val extra = (sinceLast - baseIntervalMs).coerceAtLeast(0L)
            val extraClamped = extra.coerceAtMost(baseIntervalMs)
            val progress = extraClamped.toDouble() / baseIntervalMs.toDouble()
            val minProb = 0.3
            val maxProb = 1.0
            val prob = (minProb + (maxProb - minProb) * progress).coerceIn(0.0, 1.0)

            val roll = Random.nextDouble()
            Log.i(
                TAG,
                "Prob check for $conversationId: sinceLast=${sinceLast / 60_000}min, " +
                    "base=${baseIntervalMs / 60_000}min, prob=$prob, roll=$roll"
            )

            if (roll > prob) {
                // 没“忍不住”来找你，这次只排下一次检查
                val checkIntervalMs = (baseIntervalMs / 2).coerceAtLeast(1L * 60_000L)
                Log.i(
                    TAG,
                    "Skip trigger for $conversationId this time, " +
                        "schedule next check in ${checkIntervalMs / 60_000} minutes"
                )
                scheduleNext(
                    context = context,
                    conversationId = conversationId,
                    minInterval = minIntervalSnapshot,
                    maxInterval = maxIntervalSnapshot,
                    delayMs = checkIntervalMs
                )
                return true
            }

            // 3) 掷中：真的来找你
            Log.i(TAG, "Runner started for conversation: $conversationId")
            processConversation(context, conversationId)

            // 聊完后按当前配置安排下一次
            scheduleNext(
                context = context,
                conversationId = conversationId,
                minInterval = minIntervalSnapshot,
                maxInterval = maxIntervalSnapshot
            )

            Log.i(TAG, "Runner completed for conversation: $conversationId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Runner failed for conversation: $conversationId", e)
            false
        }
    }

    private suspend fun processConversation(
        context: Context,
        conversationId: String
    ) {
        Log.i(TAG, "=== START processConversation (runner): $conversationId ===")

        val uuid = Uuid.parse(conversationId)
        val conversation = conversationRepo.getConversationById(uuid)
        if (conversation == null) {
            Log.e(TAG, "Conversation not found!")
            return
        }

        val entity = conversationDao.getConversationById(conversationId)

        // ① 取模板：优先用用户自定义的 proactivePrompt，留空则用默认模板
        val template = entity
            ?.proactivePrompt
            ?.ifBlank { defaultProactiveTemplate }
            ?: defaultProactiveTemplate

        // ② 准备占位符需要的数据

        // 当前时间
        val nowMs = System.currentTimeMillis()
        val currentTimeText = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
        )

        val allMessages = conversation.currentMessages

        // 最近助手消息时间
        val lastAssistantCreatedAt = allMessages
            .filter { it.role == MessageRole.ASSISTANT }
            .maxByOrNull { it.createdAt }
            ?.createdAt

        // 最近用户消息时间
        val lastUserCreatedAt = allMessages
            .filter { it.role == MessageRole.USER }
            .maxByOrNull { it.createdAt }
            ?.createdAt

        fun toSinceText(createdAt: kotlinx.datetime.LocalDateTime?): String {
            if (createdAt == null) return "无记录"
            val ts = createdAt
                .toInstant(TimeZone.currentSystemDefault())
                .toEpochMilliseconds()
            val ms = (nowMs - ts).coerceAtLeast(0L)
            return formatSinceDuration(ms)
        }

        val sinceAssistantText = toSinceText(lastAssistantCreatedAt)
        val sinceUserText = toSinceText(lastUserCreatedAt)


        // 电量
        val battery = getBatteryLevel(context)
        val batteryText = when {
            battery == null -> "未知"
            else -> "${battery}%"
        }

        // ③ 应用占位符，得到最终 Prompt
        val finalPrompt = template.applyPlaceholders(
            "currentTime" to currentTimeText,
            "timeSinceLastAssistantMessage" to sinceAssistantText,
            "timeSinceLastUserMessage" to sinceUserText,
            "batteryLevel" to batteryText,
        )

        // ④ 给模型调用加个超时，比如 20 秒，防止 Receiver 被卡死太久
        val finalMessage: UIMessage = try {
            withTimeout(20_000L) {
                chatService.generateProactiveReply(uuid, finalPrompt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateProactiveReply timeout or error", e)
            null
        } ?: run {
            Log.e(TAG, "No proactive message generated (runner)!")
            return
        }

        val messageText = finalMessage.toText()
        if (messageText.isBlank()) {
            Log.e(TAG, "Message text is blank!")
            return
        }

        val updatedConversation = conversation.copy(
            messageNodes = conversation.messageNodes + finalMessage.toMessageNode(),
            updateAt = Instant.now()
        )

        // 1. 更新数据库
        conversationRepo.updateConversation(updatedConversation)

        // 2. 同步内存状态（当前界面会立刻刷新）
        chatService.updateConversationState(uuid) {
            updatedConversation
        }

        // 3. 更新时间（用于间隔计算）
        conversationDao.updateLastActiveTime(conversationId, System.currentTimeMillis())

        // 4. 拿到助手名称用于通知标题
        val settings = settingsStore.settingsFlow.first()
        val assistant = settings.assistants.firstOrNull { it.id == conversation.assistantId }
        val assistantName = assistant?.name ?: "AI助手"

        // 5. 发主动消息通知（用助手名称当标题）
        sendProactiveNotification(context, conversationId, assistantName, messageText)

        Log.i(TAG, "=== END processConversation (runner) ===")
    }

    private suspend fun scheduleNext(
        context: Context,
        conversationId: String,
        minInterval: Long,
        maxInterval: Long,
        delayMs: Long? = null,
    ) {
        val entity = conversationDao.getConversationById(conversationId) ?: return
        if (!entity.receiveProactiveMessages) return

        val effectiveMin = when {
            minInterval > 0L -> minInterval
            maxInterval > 0L -> maxInterval
            else -> 60L * 60_000L
        }
        val effectiveMax = when {
            maxInterval > 0L -> maxInterval
            minInterval > 0L -> minInterval
            else -> effectiveMin
        }

        Log.i(
            TAG,
            "scheduleNext (runner) for $conversationId: " +
                "min=${effectiveMin / 60_000}min, " +
                "max=${effectiveMax / 60_000}min, " +
                "delayMs=${delayMs?.div(60_000)}min"
        )

        val interval = delayMs ?: (effectiveMin..effectiveMax).random()
        val nextTime = System.currentTimeMillis() + interval

        conversationDao.updateNextProactiveTime(conversationId, nextTime)
        ProactiveMessageScheduler.scheduleAlarm(context, conversationId, nextTime)

        Log.i(TAG, "Next alarm (runner) scheduled in ${interval / 1000 / 60} minutes")
    }

    private fun sendProactiveNotification(
        context: Context,
        conversationId: String,
        title: String,
        message: String
    ) {
        val intent = Intent(context, RouteActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("conversationId", conversationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationId = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()

        context.sendNotification(
            channelId = CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID,
            notificationId = notificationId
        ) {
            this.title = title.ifBlank { "AI助手" }
            this.content = message.take(100)
            this.autoCancel = true
            this.useDefaults = true
            this.category = NotificationCompat.CATEGORY_MESSAGE
            this.contentIntent = pendingIntent
        }
    }

    private fun getBatteryLevel(context: Context): Int? {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level =
                bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: return null
            if (level in 0..100) level else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get battery level", e)
            null
        }
    }

    private fun formatSinceDuration(ms: Long): String {
        val minutes = ms / 60_000L
        return when {
            minutes < 1 -> "不到 1 分钟"
            minutes < 60 -> "${minutes} 分钟"
            else -> {
                val hours = minutes / 60
                val remain = minutes % 60
                if (remain == 0L) "${hours} 小时" else "${hours} 小时 ${remain} 分钟"
            }
        }
    }
}
