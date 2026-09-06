package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ReplyMateApplication
import com.example.ai.AIReplyResult
import com.example.ai.MessageIntelligence
import com.example.ai.StyleAnalyzer
import com.example.data.local.ContactMemoryEntity
import com.example.data.local.ConversationMessageEntity
import com.example.data.local.PendingReplyEntity
import com.example.data.local.ReplyLogEntity
import com.example.data.model.ContactRule
import com.example.data.model.SettingsData
import com.example.data.model.StyleAnalysis
import com.example.data.security.SecureKeyStorage
import com.example.service.DiagnosticItemResult
import com.example.service.DiagnosticLogEntry
import com.example.service.DiagnosticRunner
import com.example.service.NotificationHelper
import com.example.service.ReplySender
import com.example.service.ServiceDiagnostics
import com.example.service.WhatsAppInstallStatus
import com.example.service.WhatsAppPackageDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class ReplyMateUiState(
    val settings: SettingsData = SettingsData(),
    val isNotificationAccessGranted: Boolean = false,
    val isServiceConnected: Boolean = false,
    val pendingReplies: List<PendingReplyEntity> = emptyList(),
    val recentLogs: List<ReplyLogEntity> = emptyList(),
    val todayReplyCount: Int = 0,
    val geminiKeyMasked: String = "Not configured",
    val openAiKeyMasked: String = "Not configured",
    val isTestingSim: Boolean = false,
    val testSimResult: String? = null,
    val snackbarMessage: String? = null,

    // Pro Features state
    val contactRules: Map<String, ContactRule> = emptyMap(),
    val userSampleMessages: List<String> = emptyList(),
    val styleAnalysis: StyleAnalysis = StyleAnalysis(),
    val allMemories: List<ContactMemoryEntity> = emptyList(),
    val isAppLocked: Boolean = false,

    // Diagnostics state
    val isDiagnosticsRunning: Boolean = false,
    val diagnosticResults: List<DiagnosticItemResult> = emptyList(),
    val diagnosticEvents: List<DiagnosticLogEntry> = emptyList(),
    val whatsAppInstallStatus: WhatsAppInstallStatus = WhatsAppInstallStatus(false, false),
    val keyVerificationStatus: String? = null,
    val isVerifyingKey: Boolean = false,

    // Real-time live status
    val lastWhatsAppNotificationDetected: Boolean = false,
    val lastSenderAvailable: Boolean = false,
    val lastMessageTextAvailable: Boolean = false,
    val lastReplyActionAvailable: Boolean = false,
    val lastError: String? = null,
    val replyTestResult: com.example.service.ReplyTestResult? = null,
    val isRunningReplyTest: Boolean = false
)

class ReplyMateViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ReplyMateApplication
    private val prefRepo = app.preferencesRepository
    private val keyStorage = app.secureKeyStorage
    private val db = app.database
    private val aiEngine = app.aiEngine

    private val _uiState = MutableStateFlow(
        ReplyMateUiState(
            settings = prefRepo.settingsFlow.value,
            isNotificationAccessGranted = NotificationHelper.isNotificationAccessGranted(app),
            isServiceConnected = ServiceDiagnostics.isServiceConnected.value,
            geminiKeyMasked = SecureKeyStorage.maskKey(keyStorage.getGeminiApiKey()),
            openAiKeyMasked = SecureKeyStorage.maskKey(keyStorage.getOpenAiApiKey()),
            contactRules = prefRepo.contactRulesFlow.value,
            userSampleMessages = prefRepo.userSampleMessages.value,
            styleAnalysis = prefRepo.styleAnalysisFlow.value,
            isAppLocked = prefRepo.settingsFlow.value.appLockEnabled,
            whatsAppInstallStatus = WhatsAppPackageDetector.checkInstalledPackages(app)
        )
    )
    val uiState: StateFlow<ReplyMateUiState> = _uiState.asStateFlow()

    init {
        // Collect settings updates
        viewModelScope.launch {
            prefRepo.settingsFlow.collect { newSettings ->
                _uiState.update { it.copy(settings = newSettings) }
            }
        }

        // Collect contact rules updates
        viewModelScope.launch {
            prefRepo.contactRulesFlow.collect { rules ->
                _uiState.update { it.copy(contactRules = rules) }
            }
        }

        // Collect style samples
        viewModelScope.launch {
            prefRepo.userSampleMessages.collect { samples ->
                _uiState.update { it.copy(userSampleMessages = samples) }
            }
        }

        // Collect style analysis
        viewModelScope.launch {
            prefRepo.styleAnalysisFlow.collect { analysis ->
                _uiState.update { it.copy(styleAnalysis = analysis) }
            }
        }

        // Collect memories
        viewModelScope.launch {
            db.contactMemoryDao().observeAllMemories().collect { mems ->
                _uiState.update { it.copy(allMemories = mems) }
            }
        }

        // Collect pending replies
        viewModelScope.launch {
            db.pendingReplyDao().getPendingRepliesFlow().collect { pendingList ->
                _uiState.update { it.copy(pendingReplies = pendingList) }
            }
        }

        // Collect recent logs
        viewModelScope.launch {
            db.replyLogDao().getAllLogsFlow().collect { logList ->
                _uiState.update { it.copy(recentLogs = logList) }
            }
        }

        // Collect today reply count
        viewModelScope.launch {
            db.replyLogDao().getTodayReplyCountFlow(getStartOfDay()).collect { count ->
                _uiState.update { it.copy(todayReplyCount = count) }
            }
        }

        // Collect live diagnostic events
        viewModelScope.launch {
            ServiceDiagnostics.events.collect { evts ->
                _uiState.update { it.copy(diagnosticEvents = evts) }
            }
        }

        // Collect service connection state
        viewModelScope.launch {
            ServiceDiagnostics.isServiceConnected.collect { connected ->
                _uiState.update { it.copy(isServiceConnected = connected) }
            }
        }

        // Collect real-time diagnostic flags
        viewModelScope.launch {
            ServiceDiagnostics.whatsappNotificationsCount.collect { count ->
                _uiState.update { it.copy(lastWhatsAppNotificationDetected = count > 0) }
            }
        }

        viewModelScope.launch {
            ServiceDiagnostics.lastWhatsAppSender.collect { sender ->
                _uiState.update { it.copy(lastSenderAvailable = !sender.isNullOrBlank()) }
            }
        }

        viewModelScope.launch {
            ServiceDiagnostics.lastMessageTextAvailable.collect { available ->
                _uiState.update { it.copy(lastMessageTextAvailable = available) }
            }
        }

        viewModelScope.launch {
            ServiceDiagnostics.lastReplyActionAvailable.collect { available ->
                _uiState.update { it.copy(lastReplyActionAvailable = available) }
            }
        }

        viewModelScope.launch {
            ServiceDiagnostics.lastError.collect { error ->
                _uiState.update { it.copy(lastError = error) }
            }
        }

        // Check WhatsApp installed apps
        checkWhatsAppInstallations()
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun refreshNotificationAccess() {
        val granted = NotificationHelper.isNotificationAccessGranted(app)
        val connected = ServiceDiagnostics.isServiceConnected.value
        _uiState.update {
            it.copy(
                isNotificationAccessGranted = granted,
                isServiceConnected = connected
            )
        }
    }

    fun openNotificationSettings() {
        NotificationHelper.openNotificationListenerSettings(app)
    }

    fun forceRebindService() {
        val success = NotificationHelper.forceServiceRebind(app)
        refreshNotificationAccess()
        _uiState.update {
            it.copy(
                snackbarMessage = if (success) "Notification listener rebind requested from Android OS" else "Rebind request failed"
            )
        }
        runFullDiagnostics()
    }

    fun checkWhatsAppInstallations() {
        val status = WhatsAppPackageDetector.checkInstalledPackages(app)
        _uiState.update { it.copy(whatsAppInstallStatus = status) }
        // Auto-select installed WhatsApp if only one is installed and current is unconfigured
        val current = prefRepo.settingsFlow.value
        if (current.selectedWhatsAppPackage == "both") {
            if (status.onlyMessenger) {
                prefRepo.updateSettings(current.copy(selectedWhatsAppPackage = SettingsData.PACKAGE_WHATSAPP))
            } else if (status.onlyBusiness) {
                prefRepo.updateSettings(current.copy(selectedWhatsAppPackage = SettingsData.PACKAGE_WHATSAPP_BUSINESS))
            }
        }
    }

    fun setSelectedWhatsAppPackage(pkg: String) {
        val current = prefRepo.settingsFlow.value
        prefRepo.updateSettings(current.copy(selectedWhatsAppPackage = pkg))
        _uiState.update { it.copy(snackbarMessage = "Target WhatsApp package updated") }
    }

    fun toggleAutoReply(enabled: Boolean) {
        prefRepo.toggleAutoReply(enabled)
    }

    fun toggleEmergencyKillSwitch(kill: Boolean) {
        prefRepo.setEmergencyKillSwitch(kill)
        _uiState.update {
            it.copy(snackbarMessage = if (kill) "EMERGENCY: All Auto-Replies STOPPED immediately" else "Auto-Reply Resumed")
        }
    }

    fun emergencyStop() {
        viewModelScope.launch {
            prefRepo.toggleAutoReply(false)
            prefRepo.setEmergencyKillSwitch(true)
            db.pendingReplyDao().clearAll()
            _uiState.update {
                it.copy(snackbarMessage = "EMERGENCY STOPPED: Auto-replies disabled & pending queue cleared.")
            }
        }
    }

    fun updateSettings(newSettings: SettingsData) {
        prefRepo.updateSettings(newSettings)
    }

    fun saveGeminiApiKey(key: String) {
        keyStorage.saveGeminiApiKey(key)
        updateMaskedKeys()
        _uiState.update { it.copy(snackbarMessage = "Gemini API key saved securely in Keystore") }
    }

    fun saveOpenAiApiKey(key: String) {
        keyStorage.saveOpenAiApiKey(key)
        updateMaskedKeys()
        _uiState.update { it.copy(snackbarMessage = "OpenAI API key saved securely in Keystore") }
    }

    fun verifyAndSaveApiKey(provider: String, key: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingKey = true, keyVerificationStatus = null) }
            val testResult = aiEngine.verifyApiKey(provider, key)
            if (testResult.isSuccess) {
                if (provider == SettingsData.PROVIDER_GEMINI) {
                    keyStorage.saveGeminiApiKey(key)
                } else {
                    keyStorage.saveOpenAiApiKey(key)
                }
                updateMaskedKeys()
                _uiState.update {
                    it.copy(
                        isVerifyingKey = false,
                        keyVerificationStatus = "Valid API Key verified successfully!",
                        snackbarMessage = "$provider API key verified and saved."
                    )
                }
                onSuccess()
            } else {
                val err = testResult.exceptionOrNull()?.message ?: "Verification failed"
                _uiState.update {
                    it.copy(
                        isVerifyingKey = false,
                        keyVerificationStatus = "Verification failed: $err"
                    )
                }
            }
        }
    }

    fun clearApiKeys() {
        keyStorage.clearAllKeys()
        updateMaskedKeys()
        _uiState.update { it.copy(snackbarMessage = "All API keys removed") }
    }

    private fun updateMaskedKeys() {
        _uiState.update {
            it.copy(
                geminiKeyMasked = SecureKeyStorage.maskKey(keyStorage.getGeminiApiKey()),
                openAiKeyMasked = SecureKeyStorage.maskKey(keyStorage.getOpenAiApiKey())
            )
        }
    }

    // Diagnostics
    fun runFullDiagnostics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiagnosticsRunning = true) }
            refreshNotificationAccess()
            val results = DiagnosticRunner.runDiagnostics(app)
            _uiState.update {
                it.copy(
                    isDiagnosticsRunning = false,
                    diagnosticResults = results,
                    isNotificationAccessGranted = NotificationHelper.isNotificationAccessGranted(app),
                    isServiceConnected = ServiceDiagnostics.isServiceConnected.value
                )
            }
        }
    }

    fun clearDiagnostics() {
        ServiceDiagnostics.clear()
        _uiState.update { it.copy(diagnosticEvents = emptyList(), snackbarMessage = "Diagnostics log cleared") }
    }

    fun runReplyTest(sender: String = "Test Contact", message: String = "Hello! Are you free for a quick chat?") {
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningReplyTest = true, replyTestResult = null) }
            val result = DiagnosticRunner.runReplyTest(app, sender, message)
            _uiState.update {
                it.copy(
                    isRunningReplyTest = false,
                    replyTestResult = result,
                    snackbarMessage = "Reply Test finished: ${result.finalStatus}"
                )
            }
        }
    }

    fun clearReplyTestResult() {
        _uiState.update { it.copy(replyTestResult = null) }
    }

    // Contact Intelligence
    fun saveContactRule(rule: ContactRule) {
        prefRepo.saveContactRule(rule)
        _uiState.update { it.copy(snackbarMessage = "Rule saved for ${rule.contactName}") }
    }

    fun deleteContactRule(contactName: String) {
        prefRepo.deleteContactRule(contactName)
        _uiState.update { it.copy(snackbarMessage = "Rule removed for $contactName") }
    }

    // Memories
    fun addContactMemory(contactName: String, fact: String) {
        viewModelScope.launch {
            db.contactMemoryDao().insertMemory(
                ContactMemoryEntity(contactKey = contactName, memoryFact = fact)
            )
            _uiState.update { it.copy(snackbarMessage = "Memory remembered for $contactName") }
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            db.contactMemoryDao().deleteMemory(id)
            _uiState.update { it.copy(snackbarMessage = "Memory erased") }
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            db.contactMemoryDao().clearAllMemories()
            _uiState.update { it.copy(snackbarMessage = "All contact memories cleared") }
        }
    }

    // Learn My Style
    fun addUserSampleMessage(message: String) {
        if (message.isBlank()) return
        val current = prefRepo.userSampleMessages.value.toMutableList()
        current.add(message.trim())
        prefRepo.saveUserSamples(current)
        reanalyzeStyle(current)
    }

    fun removeUserSampleMessage(index: Int) {
        val current = prefRepo.userSampleMessages.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            prefRepo.saveUserSamples(current)
            reanalyzeStyle(current)
        }
    }

    fun resetUserStyle() {
        prefRepo.saveUserSamples(emptyList())
        val defaultAnalysis = StyleAnalysis()
        prefRepo.saveStyleAnalysis(defaultAnalysis)
        _uiState.update { it.copy(snackbarMessage = "Personal style reset to default") }
    }

    private fun reanalyzeStyle(samples: List<String>) {
        val analysis = StyleAnalyzer.analyzeSamples(samples)
        prefRepo.saveStyleAnalysis(analysis)
        _uiState.update { it.copy(snackbarMessage = "Style analyzed: ${analysis.tone}") }
    }

    fun unlockApp(pin: String): Boolean {
        val correctPin = prefRepo.settingsFlow.value.appLockPin
        return if (pin == correctPin || correctPin.isBlank()) {
            _uiState.update { it.copy(isAppLocked = false) }
            true
        } else {
            false
        }
    }

    fun lockApp() {
        if (prefRepo.settingsFlow.value.appLockEnabled) {
            _uiState.update { it.copy(isAppLocked = true) }
        }
    }

    fun approveAndSend(pending: PendingReplyEntity) {
        viewModelScope.launch {
            val sent = ReplySender.sendReply(
                context = app,
                conversationKey = pending.conversationKey,
                replyText = pending.suggestedReply,
                senderName = pending.senderName
            )

            val status = if (sent) PendingReplyEntity.STATUS_SENT else "SENT_FAILED"
            db.pendingReplyDao().updateStatus(pending.id, status)

            if (sent) {
                app.messageFilter.recordSuccessfulReply(pending.conversationKey)
                app.messageFilter.incrementConsecutiveReplies(pending.conversationKey)
            }

            db.replyLogDao().insert(
                ReplyLogEntity(
                    senderName = pending.senderName,
                    incomingMessage = pending.incomingMessage,
                    replyText = if (sent) "Auto-reply sent to ${pending.senderName}: ${pending.suggestedReply}"
                    else "Failed to send reply via WhatsApp notification: Reply action expired",
                    timestamp = System.currentTimeMillis(),
                    status = if (sent) ReplyLogEntity.STATUS_MANUAL_SENT else ReplyLogEntity.STATUS_FAILED,
                    provider = pending.providerUsed
                )
            )

            if (sent) {
                db.conversationMessageDao().insert(
                    ConversationMessageEntity(
                        conversationKey = pending.conversationKey,
                        sender = "Me (AI)",
                        messageText = pending.suggestedReply,
                        isFromMe = true,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            _uiState.update {
                it.copy(
                    snackbarMessage = if (sent) "Reply sent to ${pending.senderName}"
                    else "Reply action expired in notification shade (WhatsApp notification dismissed)"
                )
            }
        }
    }

    fun editAndSend(pending: PendingReplyEntity, editedReply: String) {
        viewModelScope.launch {
            val sent = ReplySender.sendReply(
                context = app,
                conversationKey = pending.conversationKey,
                replyText = editedReply,
                senderName = pending.senderName
            )

            db.pendingReplyDao().updateReplyText(
                id = pending.id,
                newReply = editedReply,
                status = if (sent) PendingReplyEntity.STATUS_EDITED else "EDIT_FAILED"
            )

            if (sent) {
                app.messageFilter.recordSuccessfulReply(pending.conversationKey)
                app.messageFilter.incrementConsecutiveReplies(pending.conversationKey)
            }

            db.replyLogDao().insert(
                ReplyLogEntity(
                    senderName = pending.senderName,
                    incomingMessage = pending.incomingMessage,
                    replyText = if (sent) "Auto-reply sent to ${pending.senderName}: $editedReply"
                    else "Failed to send reply via WhatsApp notification: Reply action expired",
                    timestamp = System.currentTimeMillis(),
                    status = if (sent) ReplyLogEntity.STATUS_MANUAL_SENT else ReplyLogEntity.STATUS_FAILED,
                    provider = pending.providerUsed
                )
            )

            if (sent) {
                db.conversationMessageDao().insert(
                    ConversationMessageEntity(
                        conversationKey = pending.conversationKey,
                        sender = "Me (AI)",
                        messageText = editedReply,
                        isFromMe = true,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            _uiState.update {
                it.copy(
                    snackbarMessage = if (sent) "Edited reply sent to ${pending.senderName}"
                    else "Reply action expired in notification shade (WhatsApp notification dismissed)"
                )
            }
        }
    }

    fun regenerateReply(pending: PendingReplyEntity) {
        viewModelScope.launch {
            val settings = prefRepo.settingsFlow.value
            val contactRule = prefRepo.contactRulesFlow.value[pending.senderName]
            val result = aiEngine.processIncomingMessage(
                senderName = pending.senderName,
                conversationKey = pending.conversationKey,
                messageText = pending.incomingMessage,
                settings = settings,
                contactRule = contactRule
            )

            when (result) {
                is AIReplyResult.Success -> {
                    db.pendingReplyDao().updateReplyText(
                        id = pending.id,
                        newReply = result.replyText,
                        status = PendingReplyEntity.STATUS_PENDING
                    )
                    _uiState.update { it.copy(snackbarMessage = "Regenerated reply using ${result.provider}") }
                }
                is AIReplyResult.Sensitive -> {
                    _uiState.update { it.copy(snackbarMessage = "Message flagged as sensitive: ${result.reason}") }
                }
                is AIReplyResult.Error -> {
                    _uiState.update { it.copy(snackbarMessage = result.errorMessage) }
                }
            }
        }
    }

    fun cancelPendingReply(id: Long) {
        viewModelScope.launch {
            db.pendingReplyDao().updateStatus(id, PendingReplyEntity.STATUS_CANCELLED)
            _uiState.update { it.copy(snackbarMessage = "Reply cancelled") }
        }
    }

    fun simulateTestIncomingMessage(sender: String, message: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTestingSim = true,
                    testSimResult = "Simulating incoming message from $sender..."
                )
            }

            val settings = prefRepo.settingsFlow.value
            val contactRule = prefRepo.contactRulesFlow.value[sender]
            val analysis = MessageIntelligence.analyze(message)

            val result = aiEngine.processIncomingMessage(
                senderName = sender,
                conversationKey = sender,
                messageText = message,
                settings = settings,
                contactRule = contactRule
            )

            when (result) {
                is AIReplyResult.Success -> {
                    db.pendingReplyDao().insert(
                        PendingReplyEntity(
                            senderName = sender,
                            conversationKey = sender,
                            incomingMessage = message,
                            suggestedReply = result.replyText,
                            timestamp = System.currentTimeMillis(),
                            status = PendingReplyEntity.STATUS_PENDING,
                            providerUsed = result.provider
                        )
                    )

                    _uiState.update {
                        it.copy(
                            isTestingSim = false,
                            testSimResult = "Detected Intent: ${analysis.intent.name} | Lang: ${analysis.detectedLanguage} | Priority: ${analysis.priority.name}\n\nGenerated (${result.provider}):\n\"${result.replyText}\""
                        )
                    }
                }
                is AIReplyResult.Sensitive -> {
                    db.replyLogDao().insert(
                        ReplyLogEntity(
                            senderName = sender,
                            incomingMessage = message,
                            replyText = "[Sensitive message: ${result.reason}]",
                            timestamp = System.currentTimeMillis(),
                            status = ReplyLogEntity.STATUS_SENSITIVE,
                            provider = settings.aiProvider
                        )
                    )

                    _uiState.update {
                        it.copy(
                            isTestingSim = false,
                            testSimResult = "🛡️ Sensitive message detected (${analysis.intent.name}):\n${result.reason}\n(Auto-reply prevented)"
                        )
                    }
                }
                is AIReplyResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isTestingSim = false,
                            testSimResult = "Error: ${result.errorMessage}"
                        )
                    }
                }
            }
        }
    }

    fun clearConversationContext() {
        viewModelScope.launch {
            db.conversationMessageDao().clearAll()
            _uiState.update { it.copy(snackbarMessage = "Local conversation context cleared") }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            db.replyLogDao().clearAll()
            _uiState.update { it.copy(snackbarMessage = "Reply logs cleared") }
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            prefRepo.resetToDefaults()
            keyStorage.clearAllKeys()
            updateMaskedKeys()
            db.pendingReplyDao().clearAll()
            db.conversationMessageDao().clearAll()
            db.replyLogDao().clearAll()
            db.contactMemoryDao().clearAllMemories()
            _uiState.update { it.copy(snackbarMessage = "All ReplyMate settings, memories, and data reset") }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearSimResult() {
        _uiState.update { it.copy(testSimResult = null) }
    }
}
