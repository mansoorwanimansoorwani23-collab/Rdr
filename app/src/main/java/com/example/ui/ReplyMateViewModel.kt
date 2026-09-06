package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ReplyMateApplication
import com.example.ai.AIReplyResult
import com.example.data.local.ConversationMessageEntity
import com.example.data.local.PendingReplyEntity
import com.example.data.local.ReplyLogEntity
import com.example.data.model.SettingsData
import com.example.data.security.SecureKeyStorage
import com.example.service.NotificationHelper
import com.example.service.ReplySender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class ReplyMateUiState(
    val settings: SettingsData = SettingsData(),
    val isNotificationAccessGranted: Boolean = false,
    val pendingReplies: List<PendingReplyEntity> = emptyList(),
    val recentLogs: List<ReplyLogEntity> = emptyList(),
    val todayReplyCount: Int = 0,
    val geminiKeyMasked: String = "Not configured",
    val openAiKeyMasked: String = "Not configured",
    val isTestingSim: Boolean = false,
    val testSimResult: String? = null,
    val snackbarMessage: String? = null
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
            geminiKeyMasked = SecureKeyStorage.maskKey(keyStorage.getGeminiApiKey()),
            openAiKeyMasked = SecureKeyStorage.maskKey(keyStorage.getOpenAiApiKey())
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
        _uiState.update { it.copy(isNotificationAccessGranted = granted) }
    }

    fun openNotificationSettings() {
        NotificationHelper.openNotificationListenerSettings(app)
    }

    fun toggleAutoReply(enabled: Boolean) {
        if (enabled && !NotificationHelper.isNotificationAccessGranted(app)) {
            _uiState.update { it.copy(snackbarMessage = "Notification Access is required.") }
            return
        }
        prefRepo.toggleAutoReply(enabled)
        _uiState.update {
            it.copy(snackbarMessage = if (enabled) "AI Auto Reply turned ON" else "AI Auto Reply turned OFF")
        }
    }

    fun updateSettings(newSettings: SettingsData) {
        prefRepo.updateSettings(newSettings)
        _uiState.update { it.copy(snackbarMessage = "Settings updated") }
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

    fun approveAndSend(pending: PendingReplyEntity) {
        viewModelScope.launch {
            val sent = ReplySender.sendReply(
                context = app,
                conversationKey = pending.conversationKey,
                replyText = pending.suggestedReply
            )

            val status = if (sent) PendingReplyEntity.STATUS_SENT else "SENT_OFFLINE"
            db.pendingReplyDao().updateStatus(pending.id, status)

            db.replyLogDao().insert(
                ReplyLogEntity(
                    senderName = pending.senderName,
                    incomingMessage = pending.incomingMessage,
                    replyText = pending.suggestedReply,
                    timestamp = System.currentTimeMillis(),
                    status = ReplyLogEntity.STATUS_MANUAL_SENT,
                    provider = pending.providerUsed
                )
            )

            db.conversationMessageDao().insert(
                ConversationMessageEntity(
                    conversationKey = pending.conversationKey,
                    sender = "Me (AI)",
                    messageText = pending.suggestedReply,
                    isFromMe = true,
                    timestamp = System.currentTimeMillis()
                )
            )

            _uiState.update {
                it.copy(
                    snackbarMessage = if (sent) "Reply sent to ${pending.senderName}"
                    else "Reply approved (Action expired or simulated)"
                )
            }
        }
    }

    fun editAndSend(pending: PendingReplyEntity, editedReply: String) {
        viewModelScope.launch {
            val sent = ReplySender.sendReply(
                context = app,
                conversationKey = pending.conversationKey,
                replyText = editedReply
            )

            db.pendingReplyDao().updateReplyText(
                id = pending.id,
                newReply = editedReply,
                status = PendingReplyEntity.STATUS_EDITED
            )

            db.replyLogDao().insert(
                ReplyLogEntity(
                    senderName = pending.senderName,
                    incomingMessage = pending.incomingMessage,
                    replyText = editedReply,
                    timestamp = System.currentTimeMillis(),
                    status = ReplyLogEntity.STATUS_MANUAL_SENT,
                    provider = pending.providerUsed
                )
            )

            db.conversationMessageDao().insert(
                ConversationMessageEntity(
                    conversationKey = pending.conversationKey,
                    sender = "Me (Edited)",
                    messageText = editedReply,
                    isFromMe = true,
                    timestamp = System.currentTimeMillis()
                )
            )

            _uiState.update {
                it.copy(
                    snackbarMessage = if (sent) "Edited reply sent to ${pending.senderName}"
                    else "Edited reply approved"
                )
            }
        }
    }

    fun regenerateReply(pending: PendingReplyEntity) {
        viewModelScope.launch {
            val settings = prefRepo.settingsFlow.value
            val result = aiEngine.processIncomingMessage(
                senderName = pending.senderName,
                conversationKey = pending.conversationKey,
                messageText = pending.incomingMessage,
                settings = settings
            )

            when (result) {
                is AIReplyResult.Success -> {
                    db.pendingReplyDao().updateReplyText(
                        id = pending.id,
                        newReply = result.replyText,
                        status = PendingReplyEntity.STATUS_PENDING
                    )
                    _uiState.update { it.copy(snackbarMessage = "New reply generated") }
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
            val result = aiEngine.processIncomingMessage(
                senderName = sender,
                conversationKey = sender,
                messageText = message,
                settings = settings
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
                            testSimResult = "Generated (${result.provider}):\n\"${result.replyText}\""
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
                            testSimResult = "🛡️ Sensitive message detected:\n${result.reason}\n(Auto-reply prevented)"
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
            _uiState.update { it.copy(snackbarMessage = "All ReplyMate settings and data reset") }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearSimResult() {
        _uiState.update { it.copy(testSimResult = null) }
    }
}
