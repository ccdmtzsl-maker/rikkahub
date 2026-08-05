package me.rerere.rikkahub.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScheduledProactiveMessageDao {

    // 插入一条新的约定提醒
    @Insert
    suspend fun insert(message: ScheduledProactiveMessageEntity): Long

    // 查某条会话所有未触发的提醒
    @Query("SELECT * FROM scheduled_proactive_messages WHERE conversationId = :conversationId AND triggerTime > :now ORDER BY triggerTime ASC")
    suspend fun getPendingByConversation(conversationId: String, now: Long = System.currentTimeMillis()): List<ScheduledProactiveMessageEntity>

    // 按 id 查一条
    @Query("SELECT * FROM scheduled_proactive_messages WHERE id = :id")
    suspend fun getById(id: Long): ScheduledProactiveMessageEntity?

    // 按 messageId 查（删消息时用）
    @Query("SELECT * FROM scheduled_proactive_messages WHERE messageId = :messageId")
    suspend fun getByMessageId(messageId: String): ScheduledProactiveMessageEntity?

    // 查到期该触发的（triggerTime <= now）
    @Query("SELECT * FROM scheduled_proactive_messages WHERE triggerTime <= :now")
    suspend fun getDueMessages(now: Long = System.currentTimeMillis()): List<ScheduledProactiveMessageEntity>

    // 按 id 删除（手动取消 / 触发后清理）
    @Query("DELETE FROM scheduled_proactive_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    // 按 messageId 删除（删聊天消息时联动取消）
    @Query("DELETE FROM scheduled_proactive_messages WHERE messageId = :messageId")
    suspend fun deleteByMessageId(messageId: String)

    // 清空某个会话的所有提醒
    @Query("DELETE FROM scheduled_proactive_messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: String)
}
