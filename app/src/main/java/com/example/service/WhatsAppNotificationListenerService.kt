package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.ReplyMateApplication
import com.example.ai.AIReplyResult
import com.example.ai.SensitiveMessageDetector
import com.example.data.local.ConversationMessageEntity
import com.example.data.local.PendingReplyEntity
import com.example.data.local.ReplyLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WhatsAppNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val app = ReplyMateApplication.instance
        val settings = app.preferencesRepository.settingsFlow.value

        // If AI Auto Reply is OFF, immediately ignore
        if (!settings.aiAutoReplyEnabled) {
            return
        }

        val parsed = NotificationParser.parse(sbn) ?: return

        serviceScope.launch {
            // Evaluate filters (whitelist, blacklist, groups, anti-spam, loop protection)
            val filterResult = app.messageFilter.evaluate(parsed, settings)
            if (filterResult is FilterResult.Blocked) {
                return@launch
            }

            // Record this message in anti-spam tracker
            app.messageFilter.recordProcessedMessage(parsed.conversationKey, parsed.messageText)

            // Cache reply action if available
            if (parsed.replyAction != null && parsed.remoteInput != null) {
                ReplySender.cacheAction(parsed.conversationKey, parsed.replyAction, parsed.remoteInput)
            }

            // Save incoming message to local short-term conversation context
            app.database.conversationMessageDao().insert(
                ConversationMessageEntity(
                    conversationKey = parsed.conversationKey,
                    sender = parsed.senderName,
                    messageText = parsed.messageText,
                    isFromMe = false,
                    timestamp = parsed.timestamp
                )
            )

            // Check emergency / sensitive message protection
            val sensitivity = SensitiveMessageDetector.evaluate(parsed.messageText)
            if (sensitivity.isSensitive) {
                val reason = sensitivity.reason ?: "Potentially sensitive message — manual reply recommended."
                app.database.pendingReplyDao().insert(
                    PendingReplyEntity(
                        senderName = parsed.senderName,
                        conversationKey = parsed.conversationKey,
                        incomingMessage = parsed.messageText,
                        suggestedReply = "Potentially sensitive message — manual reply recommended.",
                        timestamp = System.currentTimeMillis(),
                        status = PendingReplyEntity.STATUS_SENSITIVE_SKIPPED,
                        sensitiveReason = reason,
                        providerUsed = settings.aiProvider
                    )
                )

                app.database.replyLogDao().insert(
                    ReplyLogEntity(
                        senderName = parsed.senderName,
                        incomingMessage = parsed.messageText,
                        replyText = "[Manual reply recommended: $reason]",
                        timestamp = System.currentTimeMillis(),
                        status = ReplyLogEntity.STATUS_SENSITIVE,
                        provider = settings.aiProvider
                    )
                )
                return@launch
            }

            // Generate AI reply
            val aiResult = app.aiEngine.processIncomingMessage(
                senderName = parsed.senderName,
                conversationKey = parsed.conversationKey,
                messageText = parsed.messageText,
                settings = settings
            )

            when (aiResult) {
                is AIReplyResult.Sensitive -> {
                    app.database.pendingReplyDao().insert(
                        PendingReplyEntity(
                            senderName = parsed.senderName,
                            conversationKey = parsed.conversationKey,
                            incomingMessage = parsed.messageText,
                            suggestedReply = "Potentially sensitive message — manual reply recommended.",
                            timestamp = System.currentTimeMillis(),
                            status = PendingReplyEntity.STATUS_SENSITIVE_SKIPPED,
                            sensitiveReason = aiResult.reason,
                            providerUsed = settings.aiProvider
                        )
                    )
                }

                is AIReplyResult.Success -> {
                    if (settings.approvalMode) {
                        // APPROVAL MODE: User must review in app
                        val pendingId = app.database.pendingReplyDao().insert(
                            PendingReplyEntity(
                                senderName = parsed.senderName,
                                conversationKey = parsed.conversationKey,
                                incomingMessage = parsed.messageText,
                                suggestedReply = aiResult.replyText,
                                timestamp = System.currentTimeMillis(),
                                status = PendingReplyEntity.STATUS_PENDING,
                                providerUsed = aiResult.provider
                            )
                        )

                        NotificationHelper.showApprovalNotification(
                            context = applicationContext,
                            pendingId = pendingId,
                            sender = parsed.senderName,
                            suggestedReply = aiResult.replyText
                        )
                    } else {
                        // AUTO MODE: Send automatically after configured delay
                        val delayMs = (settings.replyDelaySeconds * 1000L).coerceAtLeast(1000L)
                        delay(delayMs)

                        val sent = ReplySender.sendReply(
                            context = applicationContext,
                            conversationKey = parsed.conversationKey,
                            replyText = aiResult.replyText,
                            action = parsed.replyAction,
                            remoteInput = parsed.remoteInput
                        )

                        if (sent) {
                            app.messageFilter.incrementConsecutiveReplies(parsed.conversationKey)

                            app.database.replyLogDao().insert(
                                ReplyLogEntity(
                                    senderName = parsed.senderName,
                                    incomingMessage = parsed.messageText,
                                    replyText = aiResult.replyText,
                                    timestamp = System.currentTimeMillis(),
                                    status = ReplyLogEntity.STATUS_AUTO_SENT,
                                    provider = aiResult.provider
                                )
                            )

                            // Add sent reply to context
                            app.database.conversationMessageDao().insert(
                                ConversationMessageEntity(
                                    conversationKey = parsed.conversationKey,
                                    sender = "Me (AI)",
                                    messageText = aiResult.replyText,
                                    isFromMe = true,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        } else {
                            app.database.replyLogDao().insert(
                                ReplyLogEntity(
                                    senderName = parsed.senderName,
                                    incomingMessage = parsed.messageText,
                                    replyText = aiResult.replyText,
                                    timestamp = System.currentTimeMillis(),
                                    status = ReplyLogEntity.STATUS_FAILED,
                                    provider = aiResult.provider
                                )
                            )
                        }
                    }
                }

                is AIReplyResult.Error -> {
                    app.database.replyLogDao().insert(
                        ReplyLogEntity(
                            senderName = parsed.senderName,
                            incomingMessage = parsed.messageText,
                            replyText = "[Error: ${aiResult.errorMessage}]",
                            timestamp = System.currentTimeMillis(),
                            status = ReplyLogEntity.STATUS_FAILED,
                            provider = settings.aiProvider
                        )
                    )
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Clean up cached action if notification dismissed
        val parsed = sbn?.let { NotificationParser.parse(it) }
        if (parsed != null) {
            // Keep action cached for a short grace period in case user is approving
        }
    }
}
