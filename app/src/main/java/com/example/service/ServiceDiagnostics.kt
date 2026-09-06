package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

data class DiagnosticLogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val details: String,
    val isWhatsApp: Boolean = false,
    val senderAvailable: Boolean = false,
    val messageAvailable: Boolean = false,
    val hasReplyAction: Boolean = false
)

object ServiceDiagnostics {

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _serviceConnectedTimestamp = MutableStateFlow(0L)
    val serviceConnectedTimestamp: StateFlow<Long> = _serviceConnectedTimestamp.asStateFlow()

    private val _notificationsReceivedCount = MutableStateFlow(0)
    val notificationsReceivedCount: StateFlow<Int> = _notificationsReceivedCount.asStateFlow()

    private val _whatsappNotificationsCount = MutableStateFlow(0)
    val whatsappNotificationsCount: StateFlow<Int> = _whatsappNotificationsCount.asStateFlow()

    private val _lastWhatsAppTimestamp = MutableStateFlow(0L)
    val lastWhatsAppTimestamp: StateFlow<Long> = _lastWhatsAppTimestamp.asStateFlow()

    private val _lastWhatsAppSender = MutableStateFlow<String?>(null)
    val lastWhatsAppSender: StateFlow<String?> = _lastWhatsAppSender.asStateFlow()

    private val _lastReplyActionAvailable = MutableStateFlow(false)
    val lastReplyActionAvailable: StateFlow<Boolean> = _lastReplyActionAvailable.asStateFlow()

    private val _lastMessageTextAvailable = MutableStateFlow(false)
    val lastMessageTextAvailable: StateFlow<Boolean> = _lastMessageTextAvailable.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _events = MutableStateFlow<List<DiagnosticLogEntry>>(emptyList())
    val events: StateFlow<List<DiagnosticLogEntry>> = _events.asStateFlow()

    fun onConnected() {
        _isServiceConnected.value = true
        _serviceConnectedTimestamp.value = System.currentTimeMillis()
        logEvent(
            eventType = "NotificationListener Connected",
            details = "Android OS successfully bound to WhatsAppNotificationListenerService",
            isWhatsApp = false
        )
    }

    fun onDisconnected() {
        _isServiceConnected.value = false
        logEvent(
            eventType = "NotificationListener Disconnected",
            details = "Android OS disconnected or unbound the service",
            isWhatsApp = false
        )
    }

    fun onNotificationReceived(packageName: String, isWhatsApp: Boolean) {
        _notificationsReceivedCount.value += 1
        if (isWhatsApp) {
            _whatsappNotificationsCount.value += 1
            _lastWhatsAppTimestamp.value = System.currentTimeMillis()
        }
    }

    fun updateWhatsAppDetails(sender: String?, hasReplyAction: Boolean) {
        _lastWhatsAppSender.value = sender
        _lastReplyActionAvailable.value = hasReplyAction
    }

    fun updateMessageTextAvailable(available: Boolean) {
        _lastMessageTextAvailable.value = available
    }

    fun updateError(error: String?) {
        _lastError.value = error
    }

    fun logEvent(
        eventType: String,
        details: String,
        isWhatsApp: Boolean = false,
        senderAvailable: Boolean = false,
        messageAvailable: Boolean = false,
        hasReplyAction: Boolean = false
    ) {
        val entry = DiagnosticLogEntry(
            eventType = eventType,
            details = details,
            isWhatsApp = isWhatsApp,
            senderAvailable = senderAvailable,
            messageAvailable = messageAvailable,
            hasReplyAction = hasReplyAction
        )
        val current = _events.value.toMutableList()
        current.add(0, entry)
        if (current.size > 50) {
            _events.value = current.take(50)
        } else {
            _events.value = current
        }
    }

    fun clear() {
        _events.value = emptyList()
        _notificationsReceivedCount.value = 0
        _whatsappNotificationsCount.value = 0
        _lastWhatsAppSender.value = null
        _lastReplyActionAvailable.value = false
        _lastMessageTextAvailable.value = false
        _lastError.value = null
    }
}
