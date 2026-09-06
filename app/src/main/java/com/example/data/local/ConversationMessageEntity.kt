package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores small local conversation context per conversationKey (e.g. last 5 messages).
 * Strictly limited context for AI disambiguation.
 */
@Entity(tableName = "conversation_context")
data class ConversationMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationKey: String,
    val sender: String,
    val messageText: String,
    val isFromMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
