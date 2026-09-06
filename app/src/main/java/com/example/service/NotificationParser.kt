package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

data class ParsedWhatsAppNotification(
    val conversationKey: String,
    val senderName: String,
    val messageText: String,
    val isGroup: Boolean,
    val groupTitle: String? = null,
    val isSelfSent: Boolean = false,
    val timestamp: Long,
    val replyAction: Notification.Action?,
    val remoteInput: RemoteInput?,
    val packageName: String,
    val isSummaryNotification: Boolean = false,
    val isIndividualMessage: Boolean = true
) {
    val hasReplyAction: Boolean get() = replyAction != null && remoteInput != null
}

object NotificationParser {

    val SYSTEM_IGNORED_SUBSTRINGS = listOf(
        "checking for new messages",
        "backup in progress",
        "backing up messages",
        "missed voice call",
        "missed video call",
        "incoming voice call",
        "incoming video call",
        "calling...",
        "ongoing call",
        "whatsapp web is currently active",
        "messages and calls are end-to-end encrypted"
    )

    fun isSystemOrCallMessage(text: String, sender: String? = null): Boolean {
        val lowerText = text.lowercase()
        for (ignored in SYSTEM_IGNORED_SUBSTRINGS) {
            if (lowerText.contains(ignored)) {
                return true
            }
        }
        return false
    }

    fun parse(
        sbn: StatusBarNotification,
        selectedPackageSetting: String = "both"
    ): ParsedWhatsAppNotification? {
        val result = WhatsAppNotificationParser.parse(sbn, selectedPackageSetting)
        if (!result.isWhatsApp) {
            return null
        }

        return ParsedWhatsAppNotification(
            conversationKey = result.conversationId,
            senderName = result.sender,
            messageText = result.messageText,
            isGroup = result.isGroup,
            groupTitle = result.groupTitle,
            isSelfSent = result.isSelfSent,
            timestamp = result.timestamp,
            replyAction = result.replyAction,
            remoteInput = result.remoteInput,
            packageName = result.packageName,
            isSummaryNotification = result.isSummaryNotification,
            isIndividualMessage = result.isIndividualMessage
        )
    }

    /**
     * Comprehensive extractor for Android RemoteInput and Notification.Action.
     * Looks through direct actions, NotificationCompat actions, WearableExtender, and invisible actions.
     */
    private fun extractReplyAction(notification: Notification): Pair<Notification.Action?, RemoteInput?> {
        // 1. Direct framework actions
        notification.actions?.forEach { action ->
            action.remoteInputs?.forEach { remoteInput ->
                if (remoteInput.resultKey.isNotBlank()) {
                    return Pair(action, remoteInput)
                }
            }
        }

        // 2. NotificationCompat actions
        val actionCount = NotificationCompat.getActionCount(notification)
        for (i in 0 until actionCount) {
            val compatAction = NotificationCompat.getAction(notification, i)
            if (compatAction != null) {
                compatAction.remoteInputs?.forEach { compatRemoteInput ->
                    if (compatRemoteInput.resultKey.isNotBlank()) {
                        // Find matching framework action if possible
                        val matchingFrameworkAction = notification.actions?.firstOrNull {
                            it.title?.toString() == compatAction.title?.toString()
                        } ?: notification.actions?.firstOrNull()
                        if (matchingFrameworkAction != null && matchingFrameworkAction.remoteInputs?.isNotEmpty() == true) {
                            return Pair(matchingFrameworkAction, matchingFrameworkAction.remoteInputs!!.first())
                        }
                    }
                }
            }
        }

        // 3. Invisible actions
        try {
            val invisibleActions = NotificationCompat.getInvisibleActions(notification)
            for (action in invisibleActions) {
                action.remoteInputs?.forEach { ri ->
                    if (ri.resultKey.isNotBlank()) {
                        val matching = notification.actions?.firstOrNull()
                        if (matching != null && matching.remoteInputs?.isNotEmpty() == true) {
                            return Pair(matching, matching.remoteInputs!!.first())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignored
        }

        return Pair(null, null)
    }
}
