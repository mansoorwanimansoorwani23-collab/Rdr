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

    override fun onListenerConnected() {
        super.onListenerConnected()
        ServiceDiagnostics.onConnected()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        ServiceDiagnostics.onDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        val isWhatsApp = pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b"

        // Track notification in diagnostics
        ServiceDiagnostics.onNotificationReceived(pkg, isWhatsApp)

        if (!isWhatsApp) {
            return
        }

        val app = ReplyMateApplication.instance
        val settings = app.preferencesRepository.settingsFlow.value

        // Check if package is allowed based on user selection (Messenger / Business / Both)
        if (!WhatsAppPackageDetector.isPackageAllowed(pkg, settings.selectedWhatsAppPackage)) {
            ServiceDiagnostics.logEvent(
                eventType = "WhatsApp Notification Filtered",
                details = "Package $pkg not selected in Settings (Selected: ${settings.selectedWhatsAppPackage})",
                isWhatsApp = true
            )
            return
        }

        // Parse notification
        val parsed = NotificationParser.parse(sbn, settings.selectedWhatsAppPackage)
        if (parsed == null) {
            ServiceDiagnostics.logEvent(
                eventType = "WhatsApp Notification Skipped",
                details = "No extractable chat text in notification from $pkg",
                isWhatsApp = true
            )
            return
        }

        // WhatsApp Summary Notification Check (e.g. "158 messages from 2 chats")
        // Summary notifications must be ignored for AI processing and logged explicitly
        if (parsed.isSummaryNotification) {
            ServiceDiagnostics.logEvent(
                eventType = "WhatsApp Summary Ignored",
                details = "WhatsApp notification summary ignored ('${parsed.messageText}')",
                isWhatsApp = true
            )
            serviceScope.launch {
                app.database.replyLogDao().insert(
                    ReplyLogEntity(
                        senderName = parsed.senderName.ifBlank { "WhatsApp" },
                        incomingMessage = parsed.messageText,
                        replyText = "Auto-reply skipped: WhatsApp notification summary ignored",
                        timestamp = System.currentTimeMillis(),
                        status = ReplyLogEntity.STATUS_IGNORED,
                        provider = settings.aiProvider
                    )
                )
            }
            return
        }

        // Non-individual message or empty text check
        if (!parsed.isIndividualMessage || parsed.messageText.isBlank()) {
            ServiceDiagnostics.logEvent(
                eventType = "WhatsApp Notification Skipped",
                details = "No message text found in notification",
                isWhatsApp = true
            )
            return
        }

        // Self-sent outgoing message check
        if (parsed.isSelfSent) {
            ServiceDiagnostics.logEvent(
                eventType = "WhatsApp Outgoing Message",
                details = "Self-sent message detected ('${parsed.senderName}') — skipped",
                isWhatsApp = true
            )
            return
        }

        // Update diagnostics with extracted info
        ServiceDiagnostics.updateWhatsAppDetails(parsed.senderName, parsed.hasReplyAction)
        ServiceDiagnostics.updateMessageTextAvailable(true)
        ServiceDiagnostics.logEvent(
            eventType = "WhatsApp Message Detected",
            details = "Sender: Available ('${parsed.senderName}'), Text: Available, Action: ${if (parsed.hasReplyAction) "Available" else "Unavailable"}",
            isWhatsApp = true,
            senderAvailable = true,
            messageAvailable = true,
            hasReplyAction = parsed.hasReplyAction
        )

        // Cache reply action if available
        if (parsed.hasReplyAction) {
            ReplySender.cacheAction(
                conversationKey = parsed.conversationKey,
                action = parsed.replyAction!!,
                remoteInput = parsed.remoteInput!!,
                senderName = parsed.senderName
            )
        }

        // Record incoming message received in Recent Activity logs
        serviceScope.launch {
            app.database.replyLogDao().insert(
                ReplyLogEntity(
                    senderName = parsed.senderName,
                    incomingMessage = parsed.messageText,
                    replyText = "WhatsApp message received from ${parsed.senderName}",
                    timestamp = System.currentTimeMillis(),
                    status = ReplyLogEntity.STATUS_RECEIVED,
                    provider = settings.aiProvider
                )
            )

            // 1. Emergency Stop Check
            if (settings.emergencyKillSwitch) {
                app.database.replyLogDao().insert(
                    ReplyLogEntity(
                        senderName = parsed.senderName,
                        incomingMessage = parsed.messageText,
                        replyText = "Auto-reply blocked: Emergency Stop is active",
                        timestamp = System.currentTimeMillis(),
                        status = ReplyLogEntity.STATUS_IGNORED,
                        provider = settings.aiProvider
                    )
                )
                ServiceDiagnostics.logEvent(
                    eventType = "Message Ignored",
                    details = "Emergency Stop is currently active",
                    isWhatsApp = true
                )
                return@launch
            }

            // 2. AI Auto Reply Toggle Check
            if (!settings.aiAutoReplyEnabled) {
                app.database.replyLogDao().insert(
                    ReplyLogEntity(
                        senderName = parsed.senderName,
                        incomingMessage = parsed.messageText,
                        replyText = "Auto-reply skipped: AI Auto Reply is turned OFF",
                        timestamp = System.currentTimeMillis(),
                        status = ReplyLogEntity.STATUS_IGNORED,
                        provider = settings.aiProvider
                    )
                )
                ServiceDiagnostics.logEvent(
                    eventType = "Message Ignored",
                    details = "AI Auto Reply is turned OFF in Settings",
                    isWhatsApp = true
                )
                return@launch
            }

            // 3. Contact & Group Rules Evaluation
            val contactRule = app.preferencesRepository.contactRulesFlow.value[parsed.senderName]
            val eval = SmartRuleEvaluator.evaluate(
                senderName = parsed.senderName,
                isGroup = parsed.isGroup,
                settings = settings,
                contactRule = contactRule
            )
            if (!eval.shouldProcess) {
                val reason = eval.skipReason ?: "Filtered by smart rules"
                app.database.replyLogDao().insert(
                    ReplyLogEntity(
                        senderName = parsed.senderName,
                        incomingMessage = parsed.messageText,
                        replyText = "Auto-reply skipped: $reason",
                        timestamp = System.currentTimeMillis(),
                        status = ReplyLogEntity.STATUS_IGNORED,
                        provider = settings.aiProvider
                    )
                )
                ServiceDiagnostics.logEvent(
                    eventType = "Message Ignored",
                    details = reason,
                    isWhatsApp = true
                )
                return@launch
            }

            // 4. Message Filter (cooldown, loops, limits)
            val filterResult = app.messageFilter.evaluate(parsed, settings)
            if (filterResult is FilterResult.Blocked) {
                val reason = filterResult.reason
                app.database.replyLogDao().insert(
                    ReplyLogEntity(
                        senderName = parsed.senderName,
                        incomingMessage = parsed.messageText,
                        replyText = "Auto-reply skipped: $reason",
                        timestamp = System.currentTimeMillis(),
                        status = ReplyLogEntity.STATUS_IGNORED,
                        provider = settings.aiProvider
                    )
                )
                ServiceDiagnostics.logEvent(
                    eventType = "Message Filtered",
                    details = reason,
                    isWhatsApp = true
                )
                return@launch
            }

            // 5. Batching / Processing
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

            // Record received message for duplicate suppression only (does NOT start cooldown)
            app.messageFilter.recordReceivedMessage(conversationKey, messageText)

            // Save incoming message to local conversation history
            app.database.conversationMessageDao().insert(
                ConversationMessageEntity(
                    conversationKey = conversationKey,
                    sender = senderName,
                    messageText = messageText,
                    isFromMe = false,
                    timestamp = System.currentTimeMillis()
                )
            )

            ServiceDiagnostics.logEvent(
                eventType = "AI Processing Started",
                details = "Analyzing message from '$senderName' with ${settings.aiProvider.uppercase()}",
                isWhatsApp = true
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
                        replyText = "Auto-reply skipped: Sensitive message detected ($reason)",
                        timestamp = System.currentTimeMillis(),
                        status = ReplyLogEntity.STATUS_SENSITIVE,
                        provider = settings.aiProvider
                    )
                )
                ServiceDiagnostics.logEvent(
                    eventType = "Protected Message",
                    details = reason,
                    isWhatsApp = true
                )
                return@launch
            }

            // Generate AI reply
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
                    app.database.replyLogDao().insert(
                        ReplyLogEntity(
                            senderName = senderName,
                            incomingMessage = messageText,
                            replyText = "Auto-reply skipped: Sensitive message detected (${aiResult.reason})",
                            timestamp = System.currentTimeMillis(),
                            status = ReplyLogEntity.STATUS_SENSITIVE,
                            provider = settings.aiProvider
                        )
                    )
                }

                is AIReplyResult.Success -> {
                    ServiceDiagnostics.logEvent(
                        eventType = "AI Response Generated",
                        details = "AI reply generated (${aiResult.replyText.length} chars) via ${aiResult.provider}",
                        isWhatsApp = true
                    )

                    // Log AI generation stage
                    app.database.replyLogDao().insert(
                        ReplyLogEntity(
                            senderName = senderName,
                            incomingMessage = messageText,
                            replyText = "AI reply generated",
                            timestamp = System.currentTimeMillis(),
                            status = ReplyLogEntity.STATUS_GENERATED,
                            provider = aiResult.provider
                        )
                    )

                    if (isApprovalMode) {
                        // APPROVAL MODE: User reviews & can edit/send in app
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

                        app.database.replyLogDao().insert(
                            ReplyLogEntity(
                                senderName = senderName,
                                incomingMessage = messageText,
                                replyText = aiResult.replyText,
                                timestamp = System.currentTimeMillis(),
                                status = ReplyLogEntity.STATUS_APPROVAL_REQUIRED,
                                provider = aiResult.provider
                            )
                        )

                        NotificationHelper.showApprovalNotification(
                            context = applicationContext,
                            pendingId = pendingId,
                            sender = senderName,
                            suggestedReply = aiResult.replyText
                        )
                        ServiceDiagnostics.logEvent(
                            eventType = "Approval Required",
                            details = "Created pending card for manual review",
                            isWhatsApp = true
                        )
                    } else {
                        // AUTOMATIC MODE
                        // Verify reply action is available
                        val hasAction = ReplySender.hasCachedAction(conversationKey, senderName)
                        if (!hasAction) {
                            app.database.replyLogDao().insert(
                                ReplyLogEntity(
                                    senderName = senderName,
                                    incomingMessage = messageText,
                                    replyText = "Auto-reply skipped: No reply action available (WhatsApp reply action not available for this notification)",
                                    timestamp = System.currentTimeMillis(),
                                    status = ReplyLogEntity.STATUS_NO_ACTION,
                                    provider = aiResult.provider
                                )
                            )
                            ServiceDiagnostics.logEvent(
                                eventType = "Reply Action Missing",
                                details = "WhatsApp reply action not available for this notification",
                                isWhatsApp = true
                            )
                            ServiceDiagnostics.updateError("WhatsApp reply action not available for this notification")
                            return@launch
                        }

                        // Delay before sending
                        val delayMs = (settings.replyDelaySeconds * 1000L).coerceAtLeast(1000L)
                        delay(delayMs)

                        val sent = ReplySender.sendReply(
                            context = applicationContext,
                            conversationKey = conversationKey,
                            replyText = aiResult.replyText,
                            senderName = senderName
                        )

                        if (sent) {
                            // Cooldown ONLY starts upon successful reply sending!
                            app.messageFilter.recordSuccessfulReply(conversationKey)
                            app.messageFilter.incrementConsecutiveReplies(conversationKey)

                            app.database.replyLogDao().insert(
                                ReplyLogEntity(
                                    senderName = senderName,
                                    incomingMessage = messageText,
                                    replyText = "Auto-reply sent to $senderName: ${aiResult.replyText}",
                                    timestamp = System.currentTimeMillis(),
                                    status = ReplyLogEntity.STATUS_AUTO_SENT,
                                    provider = aiResult.provider
                                )
                            )

                            app.database.conversationMessageDao().insert(
                                ConversationMessageEntity(
                                    conversationKey = conversationKey,
                                    sender = "Me (AI)",
                                    messageText = aiResult.replyText,
                                    isFromMe = true,
                                    timestamp = System.currentTimeMillis()
                                )
                            )

                            ServiceDiagnostics.logEvent(
                                eventType = "Reply Sent",
                                details = "Auto-reply sent successfully to '$senderName'",
                                isWhatsApp = true,
                                hasReplyAction = true
                            )
                            ServiceDiagnostics.updateError(null)
                        } else {
                            // Reply action failed - DO NOT start cooldown!
                            app.database.replyLogDao().insert(
                                ReplyLogEntity(
                                    senderName = senderName,
                                    incomingMessage = messageText,
                                    replyText = "Failed to send reply via WhatsApp notification",
                                    timestamp = System.currentTimeMillis(),
                                    status = ReplyLogEntity.STATUS_FAILED,
                                    provider = aiResult.provider
                                )
                            )
                            ServiceDiagnostics.logEvent(
                                eventType = "Reply Failed",
                                details = "Failed to send reply via WhatsApp notification",
                                isWhatsApp = true
                            )
                            ServiceDiagnostics.updateError("Failed to send reply via WhatsApp notification")
                        }
                    }
                }

                is AIReplyResult.Error -> {
                    // AI Generation failed - DO NOT start cooldown!
                    app.database.replyLogDao().insert(
                        ReplyLogEntity(
                            senderName = senderName,
                            incomingMessage = messageText,
                            replyText = "Auto-reply failed: AI error (${aiResult.errorMessage})",
                            timestamp = System.currentTimeMillis(),
                            status = ReplyLogEntity.STATUS_FAILED,
                            provider = settings.aiProvider
                        )
                    )
                    ServiceDiagnostics.logEvent(
                        eventType = "AI Generation Failed",
                        details = aiResult.errorMessage,
                        isWhatsApp = true
                    )
                    ServiceDiagnostics.updateError(aiResult.errorMessage)
                }
            }
        }
    }
}
