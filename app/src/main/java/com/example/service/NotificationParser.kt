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
    val packageName: String
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
        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null
        val pkg = sbn.packageName ?: return null

        // 1. Verify WhatsApp package
        if (!WhatsAppPackageDetector.isPackageAllowed(pkg, selectedPackageSetting)) {
            return null
        }

        // 2. Extract reply action first (even if summary, some Wearable/OS summaries have reply actions)
        val (replyAction, remoteInput) = extractReplyAction(notification)

        // 3. Extract sender, titles, and group indicators
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        val isExplicitGroup = extras.getBoolean("android.isGroupConversation", false)

        var extractedSender = ""
        var extractedGroupTitle: String? = null
        var isGroup = false
        var isSelfSent = false

        if (!conversationTitle.isNullOrBlank()) {
            isGroup = true
            extractedGroupTitle = conversationTitle
            extractedSender = if (title.isNotBlank()) title else conversationTitle
        } else if (isExplicitGroup) {
            isGroup = true
            extractedGroupTitle = title.ifBlank { "WhatsApp Group" }
            extractedSender = title.ifBlank { "WhatsApp Contact" }
        } else {
            extractedSender = title.ifBlank { "WhatsApp Contact" }
        }

        // 4. Extract message text using multi-layer extraction
        var messageText = ""

        // Layer A: Parse framework EXTRA_MESSAGES bundle array (standard in modern Android MessagingStyle)
        try {
            val rawMessages = extras.getParcelableArray("android.messages")
                ?: extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (rawMessages != null && rawMessages.isNotEmpty()) {
                val lastMsgBundle = rawMessages.lastOrNull() as? Bundle
                if (lastMsgBundle != null) {
                    val text = lastMsgBundle.getCharSequence("text")?.toString()?.trim() ?: ""
                    val sender = lastMsgBundle.getCharSequence("sender")?.toString()?.trim()
                    if (text.isNotBlank()) {
                        messageText = text
                        if (!sender.isNullOrBlank()) {
                            // Check if the sender is "You" or indicates outgoing
                            if (sender.equals("You", ignoreCase = true) || sender.equals("Aap", ignoreCase = true)) {
                                isSelfSent = true
                            } else {
                                extractedSender = sender
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fall through to next layer
        }

        // Layer B: NotificationCompat MessagingStyle
        if (messageText.isBlank()) {
            try {
                val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
                if (messagingStyle != null) {
                    if (messagingStyle.isGroupConversation) {
                        isGroup = true
                        messagingStyle.conversationTitle?.toString()?.let { extractedGroupTitle = it }
                    }
                    val msgs = messagingStyle.messages
                    if (msgs.isNotEmpty()) {
                        val lastMsg = msgs.last()
                        val text = lastMsg.text?.toString()?.trim() ?: ""
                        if (text.isNotBlank()) {
                            messageText = text
                            val senderName = lastMsg.person?.name?.toString()?.trim()
                            if (senderName != null) {
                                if (senderName.equals("You", ignoreCase = true)) {
                                    isSelfSent = true
                                } else {
                                    extractedSender = senderName
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Fall through
            }
        }

        // Layer C: Notification.EXTRA_BIG_TEXT
        if (messageText.isBlank()) {
            messageText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""
        }

        // Layer D: Notification.EXTRA_TEXT
        if (messageText.isBlank()) {
            messageText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        }

        // Layer E: Notification.EXTRA_TEXT_LINES (InboxStyle lines)
        if (messageText.isBlank()) {
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (!textLines.isNullOrEmpty()) {
                val lastLine = textLines.lastOrNull()?.toString()?.trim() ?: ""
                if (lastLine.isNotBlank()) {
                    // Check if format is "Sender: message"
                    val colonIndex = lastLine.indexOf(':')
                    if (colonIndex > 0 && colonIndex < lastLine.length - 1) {
                        val possibleSender = lastLine.substring(0, colonIndex).trim()
                        val possibleText = lastLine.substring(colonIndex + 1).trim()
                        if (possibleSender.isNotBlank() && possibleText.isNotBlank()) {
                            extractedSender = possibleSender
                            messageText = possibleText
                        } else {
                            messageText = lastLine
                        }
                    } else {
                        messageText = lastLine
                    }
                }
            }
        }

        // If message is still empty and it is a pure group summary with no content, discard
        if (messageText.isBlank()) {
            return null
        }

        // Detect if outgoing self-sent message ("You: ...")
        if (messageText.startsWith("You:", ignoreCase = true) ||
            messageText.startsWith("Aap:", ignoreCase = true) ||
            extractedSender.equals("You", ignoreCase = true)
        ) {
            isSelfSent = true
        }

        // Filter system WhatsApp notifications
        if (isSystemOrCallMessage(messageText, extractedSender)) {
            return null
        }

        // Derive stable conversationKey
        val conversationKey = sbn.tag?.takeIf { it.isNotBlank() }
            ?: (if (isGroup && !extractedGroupTitle.isNullOrBlank()) extractedGroupTitle!! else extractedSender)

        return ParsedWhatsAppNotification(
            conversationKey = conversationKey,
            senderName = extractedSender,
            messageText = messageText,
            isGroup = isGroup,
            groupTitle = extractedGroupTitle,
            isSelfSent = isSelfSent,
            timestamp = sbn.postTime,
            replyAction = replyAction,
            remoteInput = remoteInput,
            packageName = pkg
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
