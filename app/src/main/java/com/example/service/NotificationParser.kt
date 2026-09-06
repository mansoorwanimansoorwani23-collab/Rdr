package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

data class ParsedWhatsAppNotification(
    val conversationKey: String,
    val senderName: String,
    val messageText: String,
    val isGroup: Boolean,
    val timestamp: Long,
    val replyAction: Notification.Action?,
    val remoteInput: RemoteInput?
)

object NotificationParser {

    fun parse(sbn: StatusBarNotification): ParsedWhatsAppNotification? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null

        // 1. Detect if it is a WhatsApp notification
        val pkg = sbn.packageName
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") {
            return null
        }

        // Ignore summary notifications or empty notifications
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return null
        }

        // 2. Extract title / sender
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()

        val isGroup = !conversationTitle.isNullOrBlank() || title.contains(":") || title.contains("@")

        // 3. Extract message text
        var messageText = ""

        // Try MessagingStyle first
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            val latestMessage = messagingStyle.messages.last()
            messageText = latestMessage.text?.toString()?.trim() ?: ""
        }

        if (messageText.isBlank()) {
            messageText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
                ?: ""
        }

        if (messageText.isBlank()) {
            return null
        }

        // Ignore system WhatsApp messages like "Checking for new messages", "Backup in progress", "Calling..."
        val lowerText = messageText.lowercase()
        if (lowerText.contains("checking for new messages") ||
            lowerText.contains("backup in progress") ||
            lowerText.contains("missed voice call") ||
            lowerText.contains("missed video call") ||
            lowerText.contains("incoming voice call")
        ) {
            return null
        }

        val senderName = if (conversationTitle.isNullOrBlank()) {
            if (title.isBlank()) "WhatsApp Contact" else title
        } else {
            conversationTitle
        }

        val conversationKey = sbn.tag?.takeIf { it.isNotBlank() } ?: senderName

        // 4. Extract reply action with RemoteInput
        var replyAction: Notification.Action? = null
        var replyRemoteInput: RemoteInput? = null

        notification.actions?.forEach { action ->
            action.remoteInputs?.forEach { remoteInput ->
                if (remoteInput.resultKey.isNotBlank()) {
                    replyAction = action
                    replyRemoteInput = remoteInput
                    return@forEach
                }
            }
            if (replyAction != null) return@forEach
        }

        return ParsedWhatsAppNotification(
            conversationKey = conversationKey,
            senderName = senderName,
            messageText = messageText,
            isGroup = isGroup,
            timestamp = sbn.postTime,
            replyAction = replyAction,
            remoteInput = replyRemoteInput
        )
    }
}
