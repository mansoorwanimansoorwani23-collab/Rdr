package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.data.model.SettingsData

data class WhatsAppParsedResult(
    val isWhatsApp: Boolean,
    val isWhatsAppBusiness: Boolean,
    val isSummaryNotification: Boolean,
    val isIndividualMessage: Boolean,
    val sender: String,
    val messageText: String,
    val conversationId: String,
    val timestamp: Long,
    val replyActionAvailable: Boolean,
    val replyAction: Notification.Action? = null,
    val remoteInput: RemoteInput? = null,
    val isGroup: Boolean = false,
    val groupTitle: String? = null,
    val isSelfSent: Boolean = false,
    val packageName: String = ""
)

object WhatsAppNotificationParser {

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

    private val SUMMARY_PATTERNS = listOf(
        Regex("""(?i)^\d+\s+(?:new\s+)?messages?(?:\s+from\s+\d+\s+chats?)?$"""),
        Regex("""(?i)^\d+\s+messages?\s+from\s+\d+\s+chats?$"""),
        Regex("""(?i)^\d+\s+chats?$"""),
        Regex("""(?i)^\d+\s+unread\s+messages?$"""),
        Regex("""(?i)^\d+\s+naye\s+sandesh(?:\s+\d+\s+chats?\s+se)?$""")
    )

    fun isSummaryText(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        for (pattern in SUMMARY_PATTERNS) {
            if (pattern.matches(trimmed)) return true
        }
        val lower = trimmed.lowercase()
        if (lower.contains("messages from") && lower.contains("chat")) return true
        return false
    }

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
        selectedPackageSetting: String = SettingsData.PACKAGE_BOTH
    ): WhatsAppParsedResult {
        val pkg = sbn.packageName ?: ""
        val isMessenger = pkg == SettingsData.PACKAGE_WHATSAPP
        val isBusiness = pkg == SettingsData.PACKAGE_WHATSAPP_BUSINESS
        val isWhatsApp = isMessenger || isBusiness

        if (!isWhatsApp) {
            return WhatsAppParsedResult(
                isWhatsApp = false,
                isWhatsAppBusiness = false,
                isSummaryNotification = false,
                isIndividualMessage = false,
                sender = "",
                messageText = "",
                conversationId = "",
                timestamp = sbn.postTime,
                replyActionAvailable = false,
                packageName = pkg
            )
        }

        // Package filter check
        if (!WhatsAppPackageDetector.isPackageAllowed(pkg, selectedPackageSetting)) {
            return WhatsAppParsedResult(
                isWhatsApp = true,
                isWhatsAppBusiness = isBusiness,
                isSummaryNotification = false,
                isIndividualMessage = false,
                sender = "",
                messageText = "",
                conversationId = "",
                timestamp = sbn.postTime,
                replyActionAvailable = false,
                packageName = pkg
            )
        }

        val notification = sbn.notification
        if (notification == null) {
            return WhatsAppParsedResult(
                isWhatsApp = true,
                isWhatsAppBusiness = isBusiness,
                isSummaryNotification = false,
                isIndividualMessage = false,
                sender = "",
                messageText = "",
                conversationId = "",
                timestamp = sbn.postTime,
                replyActionAvailable = false,
                packageName = pkg
            )
        }

        val extras = notification.extras ?: Bundle()

        // 1. Check if Android or WhatsApp flagged this as a group summary notification
        val isFlagSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        val isCompatSummary = NotificationCompat.isGroupSummary(notification)
        val isExtrasSummary = extras.getBoolean("android.support.isGroupSummary", false) ||
                extras.getBoolean("android.isGroupSummary", false)

        // 2. Extract RemoteInput reply action
        val (replyAction, remoteInput) = extractReplyAction(notification)
        val replyActionAvailable = replyAction != null && remoteInput != null

        // 3. Extract Titles and conversation metadata
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

        // Layer A: Notification.EXTRA_MESSAGES / android.messages (Parcelable bundle array in MessagingStyle)
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
            // Fall through to next extraction layer
        }

        // Layer B: NotificationCompat.MessagingStyle
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

        // Layer E: Notification.EXTRA_TEXT_LINES (InboxStyle)
        if (messageText.isBlank()) {
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (!textLines.isNullOrEmpty()) {
                val lastLine = textLines.lastOrNull()?.toString()?.trim() ?: ""
                if (lastLine.isNotBlank()) {
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

        // Check if outgoing self-sent message
        if (messageText.startsWith("You:", ignoreCase = true) ||
            messageText.startsWith("Aap:", ignoreCase = true) ||
            extractedSender.equals("You", ignoreCase = true)
        ) {
            isSelfSent = true
        }

        // Check summary conditions
        val isSummaryByText = isSummaryText(messageText) ||
                ((title.equals("WhatsApp", ignoreCase = true) || title.equals("WhatsApp Business", ignoreCase = true)) &&
                        (messageText.contains("messages from", ignoreCase = true) || messageText.contains("new messages", ignoreCase = true)))

        val isSummaryNotification = isFlagSummary || isCompatSummary || isExtrasSummary || isSummaryByText

        // Conversation ID
        val conversationId = sbn.tag?.takeIf { it.isNotBlank() }
            ?: (if (isGroup && !extractedGroupTitle.isNullOrBlank()) extractedGroupTitle!! else extractedSender)

        val isSystemMsg = isSystemOrCallMessage(messageText, extractedSender)
        val isIndividualMessage = isWhatsApp && !isSummaryNotification && !isSystemMsg && !isSelfSent && messageText.isNotBlank()

        return WhatsAppParsedResult(
            isWhatsApp = true,
            isWhatsAppBusiness = isBusiness,
            isSummaryNotification = isSummaryNotification,
            isIndividualMessage = isIndividualMessage,
            sender = extractedSender,
            messageText = messageText,
            conversationId = conversationId,
            timestamp = sbn.postTime,
            replyActionAvailable = replyActionAvailable,
            replyAction = replyAction,
            remoteInput = remoteInput,
            isGroup = isGroup,
            groupTitle = extractedGroupTitle,
            isSelfSent = isSelfSent,
            packageName = pkg
        )
    }

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
