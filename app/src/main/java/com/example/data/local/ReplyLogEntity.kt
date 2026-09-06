package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reply_logs")
data class ReplyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderName: String,
    val incomingMessage: String,
    val replyText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String,
    val provider: String
) {
    companion object {
        const val STATUS_RECEIVED = "RECEIVED"
        const val STATUS_PARSED = "PARSED"
        const val STATUS_IGNORED = "IGNORED"
        const val STATUS_PROCESSING = "PROCESSING"
        const val STATUS_GENERATED = "GENERATED"
        const val STATUS_APPROVAL_REQUIRED = "APPROVAL_REQUIRED"
        const val STATUS_AUTO_SENT = "AUTO_SENT"
        const val STATUS_MANUAL_SENT = "MANUAL_SENT"
        const val STATUS_SENSITIVE = "SENSITIVE_ALERT"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_RATE_LIMITED = "RATE_LIMITED"
        const val STATUS_NO_ACTION = "NO_REPLY_ACTION"
    }
}
