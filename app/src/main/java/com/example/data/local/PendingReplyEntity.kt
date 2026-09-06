package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_replies")
data class PendingReplyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderName: String,
    val conversationKey: String,
    val incomingMessage: String,
    val suggestedReply: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = STATUS_PENDING,
    val sensitiveReason: String? = null,
    val providerUsed: String = "gemini"
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENT = "SENT"
        const val STATUS_EDITED = "EDITED"
        const val STATUS_CANCELLED = "CANCELLED"
        const val STATUS_SENSITIVE_SKIPPED = "SENSITIVE_SKIPPED"
    }
}
