package me.rerere.rikkahub.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_proactive_messages")
data class ScheduledProactiveMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: String,       // 属于哪条会话
    val triggerTime: Long,            // 什么时候触发（时间戳）
    val messagePrompt: String,        // 到时候大概想说啥
    val messageId: String = "",       // 关联的那条 assistant 消息的 id（用于删消息时取消闹钟）
    val createdAt: Long = System.currentTimeMillis(),
)
