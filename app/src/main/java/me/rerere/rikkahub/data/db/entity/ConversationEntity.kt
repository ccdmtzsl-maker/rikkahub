package me.rerere.rikkahub.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo("assistant_id", defaultValue = "0950e2dc-9bd5-4801-afa3-aa887aa36b4e")
    val assistantId: String,
    @ColumnInfo("title")
    val title: String,
    @ColumnInfo("nodes")
    val nodes: String,
    @ColumnInfo("create_at")
    val createAt: Long,
    @ColumnInfo("update_at")
    val updateAt: Long,
    @ColumnInfo("suggestions", defaultValue = "[]")
    val chatSuggestions: String,
    @ColumnInfo("is_pinned", defaultValue = "0")
    val isPinned: Boolean,
    @ColumnInfo("receive_proactive_messages", defaultValue = "0")
    val receiveProactiveMessages: Boolean = false,
    @ColumnInfo(name = "last_active_time", defaultValue = "0")
    val lastActiveTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "min_proactive_interval", defaultValue = "3600000")
    val minProactiveInterval: Long = 3600000,
    @ColumnInfo(name = "max_proactive_interval", defaultValue = "10800000")
    val maxProactiveInterval: Long = 10800000,
    @ColumnInfo(name = "next_proactive_time", defaultValue = "0")
    val nextProactiveTime: Long = 0,
    @ColumnInfo(name = "proactive_prompt", defaultValue = "")
    val proactivePrompt: String = "",
)
