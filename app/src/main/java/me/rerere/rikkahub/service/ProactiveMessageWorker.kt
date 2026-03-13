package me.rerere.rikka.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import me.rerere.rikka.R
import me.rerere.rikka.di.mcpClient
import me.rerere.rikka.ui.screen.main.MainActivity
import me.rerere.rikka.utils.mcp.McpCall

class ProactiveMessageWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            // 调用我们之前在服务器上创建的 "proactive_ping" 工具
            val response = mcpClient.call(
                McpCall("proactive_ping", emptyMap())
            )
            val message = response.result.content.firstOrNull()?.text ?: ""

            // 如果服务器返回了消息（不是空字符串），就弹出一个通知
            if (message.isNotBlank()) {
                sendNotification(message)
            }

            return Result.success()
        } catch (e: Exception) {
            // 如果出错了，就静默失败，等下次再试
            e.printStackTrace()
            return Result.failure()
        }
    }

    private fun sendNotification(message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "PROACTIVE_MESSAGE_CHANNEL"

        // 为高版本安卓创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AI 主动消息",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "接收来自 AI 的主动问候和提醒"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 创建点击通知后要打开的意图（打开App主界面）
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // 构建通知
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 使用App自己的图标
            .setContentTitle("你的AI助手")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // 点击后自动消失
            .build()

        // 发送通知
        notificationManager.notify(1, notification)
    }
}
