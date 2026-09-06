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
    private var batcher: SmartMessageBatcher? = null

    override fun onCreate() {
        super.onCreate()
        batcher = SmartMessageBatcher(serviceScope) { convKey, sender, combinedMsg ->
            handleBatchedMessage(convKey, sender, combinedMsg)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val app = ReplyMateApplication.instance
        val settings = app.preferencesRepository.settingsFlow.value

        // Quick check: If AI Auto Reply is OFF or Kill Switch is ON, ignore immediately
        if (!settings.aiAutoReplyEnabled || settings.emergencyKillSwitch) {
            return
        }

        val parsed = NotificationParser.parse(sbn) ?: return

        // Cache reply action as soon as seen
        if (parsed.replyAction != null && parsed.remoteInput != null) {
            ReplySender.cacheAction(parsed.conversationKey, parsed.replyAction, parsed.remoteInput)
        }

        serviceScope.launch {
            val contactRule = app.preferencesRepository.contactRulesFlow.value[parsed.senderName]

            // 1. Evaluate smart rule evaluator (quiet hours, kill switch, whitelist, group modes)
            val eval = SmartRuleEvaluator.evaluate(
                senderName = parsed.senderName,
                isGroup = parsed.isGroup,
                settings = settings,
                contactRule = contactRule
            )
            if (!eval.shouldProcess) {
                return@launch
            }

            // 2. Evaluate message filter (cooldown, loops, daily limits)
            val filterResult = app.messageFilter.evaluate(parsed, settings)
            if (filterResult is FilterResult.Blocked) {
                return@launch
            }

            // 3. Batching check: aggregate rapid-fire messages if enabled
            if (settings.smartBatchingEnabled && settings.batchWindowSeconds > 0) {
                batcher?.addMessage(
                    conversationKey = parsed.conversationKey,
                    senderName = parsed.senderName,
                    messageText = parsed.messageText,
                    windowSeconds = settings.batchWindowSeconds
                )
            } else {
                handleBatchedMessage(parsed.conversationKey, parsed.senderName, parsed.messageText)
            }
        }
    }

    private fun handleBatchedMessage(conversationKey: String, senderName: String, messageText: String) {
        serviceScope.launch {
            val app = ReplyMateApplication.instance
            val settings = app.preferencesRepository.settingsFlow.value
            val contactRule = app.preferencesRepository.contactRulesFlow.value[senderName]

            // Record processed message in anti-spam tracker
            app.messageFilter.recordProcessedMessage(conversationKey, messageText)

            // Save incoming message to local short-term conversation context
            app.database.conversationMessageDao().insert(
                ConversationMessageEntity(
                    conversationKey = conversationKey,
                    sender = senderName,
                    messageText = messageText,
                    isFromMe = false,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Check emergency / sensitive message protection
            val sensitivity = SensitiveMessageDetector.evaluate(messageText)
            if (sensitivity.isSensitive) {
                val reason = sensitivity.reason ?: "Potentially sensitive message — manual reply recommended."
                app.database.pendingReplyDao().insert(
                    PendingReplyEntity(
                        senderName = senderName,
                        conversationKey = conversationKey,
                        incomingMessage = messageText,
                        suggestedReply = "Potentially sensitive message — manual reply recommended.",
                        timestamp = System.currentTimeMillis(),
                        status = PendingReplyEntity.STATUS_SENSITIVE_SKIPPED,
                        sensitiveReason = reason,
                        providerUsed = settings.aiProvider
                    )
                )

                app.database.replyLogDao().insert(
                    ReplyLogEntity(
                        senderName = senderName,
                        incomingMessage = messageText,
                        replyText = "[Manual reply recommended: $reason]",
                        timestamp = System.currentTimeMillis(),
                        status = ReplyLogEntity.STATUS_SENSITIVE,
                        provider = settings.aiProvider
                    )
                )
                return@launch
            }

            // Generate AI reply with full contact intelligence & pro engine
            val aiResult = app.aiEngine.processIncomingMessage(
                senderName = senderName,
                conversationKey = conversationKey,
                messageText = messageText,
                settings = settings,
                contactRule = contactRule
            )

            val isApprovalMode = contactRule?.approvalMode ?: settings.approvalMode

            when (aiResult) {
                is AIReplyResult.Sensitive -> {
                    app.database.pendingReplyDao().insert(
                        PendingReplyEntity(
                            senderName = senderName,
                            conversationKey = conversationKey,
                            incomingMessage = messageText,
                            suggestedReply = "Potentially sensitive message — manual reply recommended.",
                            timestamp = System.currentTimeMillis(),
                            status = PendingReplyEntity.STATUS_SENSITIVE_SKIPPED,
                            sensitiveReason = aiResult.reason,
                            providerUsed = settings.aiProvider
                        )
                    )
                }

                is AIReplyResult.Success -> {
                    if (isApprovalMode) {
                        // APPROVAL MODE: User reviews & can regenerate / edit in app
                        val pendingId = app.database.pendingReplyDao().insert(
                            PendingReplyEntity(
                                senderName = senderName,
                                conversationKey = conversationKey,
                                incomingMessage = messageText,
                                suggestedReply = aiResult.replyText,
                                timestamp = System.currentTimeMillis(),
                                status = PendingReplyEntity.STATUS_PENDING,
                                providerUsed = aiResult.provider
                            )
                        )

                        NotificationHelper.showApprovalNotification(
                            context = applicationContext,
                            pendingId = pendingId,
                            sender = senderName,
                            suggestedReply = aiResult.replyText
                        )
                    } else {
                        // AUTO MODE: Send automatically after configured delay
                        val delayMs = (settings.replyDelaySeconds * 1000L).coerceAtLeast(1000L)
                        delay(delayMs)

                        val sent = ReplySender.sendReply(
                            context = applicationContext,
                            conversationKey = conversationKey,
                            replyText = aiResult.replyText
                        )

                        if (sent) {
                            app.messageFilter.incrementConsecutiveReplies(conversationKey)

                            app.database.replyLogDao().insert(
                                ReplyLogEntity(
                                    senderName = senderName,
                                    incomingMessage = messageText,
                                    replyText = aiResult.replyText,
                                    timestamp = System.currentTimeMillis(),
                                    status = ReplyLogEntity.STATUS_AUTO_SENT,
                                    provider = aiResult.provider
                                )
                            )

                            // Add sent reply to context
                            app.database.conversationMessageDao().insert(
                                ConversationMessageEntity(
                                    conversationKey = conversationKey,
                                    sender = "Me (AI)",
                                    messageText = aiResult.replyText,
                                    isFromMe = true,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        } else {
                            app.database.replyLogDao().insert(
                                ReplyLogEntity(
                                    senderName = senderName,
                                    incomingMessage = messageText,
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
                            senderName = senderName,
                            incomingMessage = messageText,
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
    }
}
