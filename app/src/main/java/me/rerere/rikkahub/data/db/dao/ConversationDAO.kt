package me.rerere.rikkahub.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.rerere.rikkahub.data.db.entity.ConversationEntity
import me.rerere.rikkahub.data.repository.LightConversationEntity

@Dao
interface ConversationDAO {
    @Query("SELECT * FROM conversationentity ORDER BY is_pinned DESC, update_at DESC")
    fun getAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversationentity ORDER BY is_pinned DESC, update_at DESC")
    fun getAllPaging(): PagingSource<Int, ConversationEntity>

    @Query("SELECT * FROM conversationentity WHERE assistant_id = :assistantId ORDER BY is_pinned DESC, update_at DESC")
    fun getConversationsOfAssistant(assistantId: String): Flow<List<ConversationEntity>>

    @Query("SELECT id, assistant_id as assistantId, title, is_pinned as isPinned, create_at as createAt, update_at as updateAt, last_active_time as lastActiveTime, min_proactive_interval as minProactiveInterval, max_proactive_interval as maxProactiveInterval, next_proactive_time as nextProactiveTime, proactive_prompt as proactivePrompt FROM conversationentity WHERE assistant_id = :assistantId ORDER BY is_pinned DESC, update_at DESC")
    fun getConversationsOfAssistantPaging(assistantId: String): PagingSource<Int, LightConversationEntity>

    @Query("SELECT * FROM conversationentity WHERE assistant_id = :assistantId ORDER BY is_pinned DESC, update_at DESC LIMIT :limit")
    suspend fun getRecentConversationsOfAssistant(assistantId: String, limit: Int): List<ConversationEntity>

    @Query("SELECT * FROM conversationentity WHERE title LIKE '%' || :searchText || '%' ORDER BY is_pinned DESC, update_at DESC")
    fun searchConversations(searchText: String): Flow<List<ConversationEntity>>

    @Query("SELECT id, assistant_id as assistantId, title, is_pinned as isPinned, create_at as createAt, update_at as updateAt, last_active_time as lastActiveTime, min_proactive_interval as minProactiveInterval, max_proactive_interval as maxProactiveInterval, next_proactive_time as nextProactiveTime, proactive_prompt as proactivePrompt FROM conversationentity WHERE title LIKE '%' || :searchText || '%' ORDER BY is_pinned DESC, update_at DESC")
    fun searchConversationsPaging(searchText: String): PagingSource<Int, LightConversationEntity>

    @Query("SELECT * FROM conversationentity WHERE assistant_id = :assistantId AND title LIKE '%' || :searchText || '%' ORDER BY is_pinned DESC, update_at DESC")
    fun searchConversationsOfAssistant(assistantId: String, searchText: String): Flow<List<ConversationEntity>>

    @Query("SELECT id, assistant_id as assistantId, title, is_pinned as isPinned, create_at as createAt, update_at as updateAt, last_active_time as lastActiveTime, min_proactive_interval as minProactiveInterval, max_proactive_interval as maxProactiveInterval, next_proactive_time as nextProactiveTime, proactive_prompt as proactivePrompt FROM conversationentity WHERE assistant_id = :assistantId AND title LIKE '%' || :searchText || '%' ORDER BY is_pinned DESC, update_at DESC")
    fun searchConversationsOfAssistantPaging(assistantId: String, searchText: String): PagingSource<Int, LightConversationEntity>

    @Query("SELECT * FROM conversationentity WHERE id = :id")
    fun getConversationFlowById(id: String): Flow<ConversationEntity?>

    @Query("SELECT id FROM conversationentity")
    suspend fun getAllIds(): List<String>

    @Query("SELECT * FROM conversationentity WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM conversationentity WHERE id = :id)")
    suspend fun existsById(id: String): Boolean

    @Insert
    suspend fun insert(conversation: ConversationEntity)

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Delete
    suspend fun delete(conversation: ConversationEntity)

    @Query("UPDATE conversationentity SET nodes = '[]' WHERE id = :id")
    suspend fun resetConversationNodes(id: String)

    @Query("DELETE FROM conversationentity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM conversationentity")
    suspend fun deleteAll()

    @Query("SELECT * FROM conversationentity WHERE is_pinned = 1 ORDER BY update_at DESC")
    fun getPinnedConversations(): Flow<List<ConversationEntity>>

    @Query("UPDATE conversationentity SET is_pinned = :isPinned WHERE id = :id")
    suspend fun updatePinStatus(id: String, isPinned: Boolean)

    @Query("SELECT COUNT(*) FROM conversationentity")
    suspend fun countAll(): Int

    @Query(
        "SELECT strftime('%Y-%m-%d', create_at/1000, 'unixepoch', 'localtime') AS day, " +
            "COUNT(*) AS count " +
            "FROM conversationentity " +
            "WHERE create_at >= :startMillis " +
            "GROUP BY day"
    )
    suspend fun getConversationCountPerDay(startMillis: Long): List<ConversationDayCount>

    @Query("UPDATE conversationentity SET receive_proactive_messages = :enabled WHERE id = :id")
    suspend fun updateProactiveMessages(id: String, enabled: Boolean)

    @Query("SELECT * FROM conversationentity WHERE receive_proactive_messages = 1")
    suspend fun getProactiveConversations(): List<ConversationEntity>

    @Query("UPDATE conversationentity SET last_active_time = :time WHERE id = :id")
    suspend fun updateLastActiveTime(id: String, time: Long)

    @Query("UPDATE conversationentity SET next_proactive_time = :nextTime WHERE id = :id")
    suspend fun updateNextProactiveTime(id: String, nextTime: Long)

    @Query(
        "UPDATE conversationentity " +
            "SET min_proactive_interval = :interval, max_proactive_interval = :interval " +
            "WHERE id = :id"
    )
    suspend fun updateProactiveIntervalForTest(id: String, interval: Long)

    @Query("UPDATE conversationentity SET last_active_time = :now, next_proactive_time = 0")
    suspend fun resetAllProactiveState(now: Long)

    @Query("UPDATE conversationentity SET proactive_prompt = :prompt WHERE id = :id")
    suspend fun updateProactivePrompt(id: String, prompt: String)

    @Query("""
    UPDATE conversationentity
    SET min_proactive_interval = :minInterval,
        max_proactive_interval = :maxInterval
    WHERE id = :id
""")
    suspend fun updateProactiveIntervalRange(
        id: String,
        minInterval: Long,
        maxInterval: Long,
    )

}

data class ConversationDayCount(val day: String, val count: Int)
