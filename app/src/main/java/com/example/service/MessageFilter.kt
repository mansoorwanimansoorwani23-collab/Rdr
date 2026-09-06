package com.example.service

import com.example.data.local.AppDatabase
import com.example.data.model.SettingsData
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

sealed class FilterResult {
    object Allowed : FilterResult()
    data class Blocked(val reason: String) : FilterResult()
}

class MessageFilter(private val database: AppDatabase) {

    // Tracks last message timestamp per conversation key for cooldown & duplicate detection
    private val lastMessageTimestamps = ConcurrentHashMap<String, Long>()
    private val lastProcessedTexts = ConcurrentHashMap<String, String>()

    // Loop prevention: tracks consecutive AI replies sent without user intervention
    private val consecutiveReplyCounters = ConcurrentHashMap<String, Int>()

    suspend fun evaluate(
        parsed: ParsedWhatsAppNotification,
        settings: SettingsData
    ): FilterResult {
        // 1. Check if AI Auto Reply is toggled ON
        if (!settings.aiAutoReplyEnabled) {
            return FilterResult.Blocked("AI Auto Reply is currently turned OFF.")
        }

        // 2. Ignore group messages if configured
        if (settings.ignoreGroups && parsed.isGroup) {
            return FilterResult.Blocked("Group message ignored by filter rules.")
        }

        // 3. Ignore unknown numbers if configured
        if (settings.ignoreUnknownNumbers) {
            val isNumber = parsed.senderName.startsWith("+") ||
                    parsed.senderName.replace(" ", "").all { it.isDigit() || it == '+' }
            if (isNumber) {
                return FilterResult.Blocked("Unknown phone number ignored by settings.")
            }
        }

        // 4. Contact whitelist / blacklist
        if (settings.replyOnlyContacts) {
            val isAllowed = settings.allowedContacts.any {
                it.equals(parsed.senderName, ignoreCase = true)
            }
            if (!isAllowed) {
                return FilterResult.Blocked("Contact '${parsed.senderName}' is not in allowed contacts whitelist.")
            }
        } else {
            val isIgnored = settings.ignoredContacts.any {
                it.equals(parsed.senderName, ignoreCase = true)
            }
            if (isIgnored) {
                return FilterResult.Blocked("Contact '${parsed.senderName}' is in ignored contacts blacklist.")
            }
        }

        // 5. Check duplicate message
        val lastText = lastProcessedTexts[parsed.conversationKey]
        val lastTimestamp = lastMessageTimestamps[parsed.conversationKey] ?: 0L
        val now = System.currentTimeMillis()

        if (lastText != null && lastText == parsed.messageText && (now - lastTimestamp) < 15_000L) {
            return FilterResult.Blocked("Duplicate notification detected.")
        }

        // 6. Anti-spam cooldown per conversation
        val cooldownMs = (settings.conversationCooldownSeconds * 1000L).coerceAtLeast(5_000L)
        if (now - lastTimestamp < cooldownMs) {
            return FilterResult.Blocked("Cooldown active for this conversation (${cooldownMs / 1000}s).")
        }

        // 7. Loop prevention: check max consecutive replies
        val currentCount = consecutiveReplyCounters[parsed.conversationKey] ?: 0
        if (currentCount >= settings.maxRepliesPerConversation) {
            return FilterResult.Blocked("Loop protection: maximum consecutive replies (${settings.maxRepliesPerConversation}) reached for this conversation.")
        }

        // 8. Daily reply limit check
        val startOfDay = getStartOfDayTimestamp()
        val todayCount = database.replyLogDao().getTodayReplyCount(startOfDay)
        if (todayCount >= settings.dailyReplyLimit) {
            return FilterResult.Blocked("Daily reply limit (${settings.dailyReplyLimit}) reached.")
        }

        return FilterResult.Allowed
    }

    fun recordProcessedMessage(conversationKey: String, messageText: String) {
        lastMessageTimestamps[conversationKey] = System.currentTimeMillis()
        lastProcessedTexts[conversationKey] = messageText
    }

    fun incrementConsecutiveReplies(conversationKey: String) {
        val current = consecutiveReplyCounters[conversationKey] ?: 0
        consecutiveReplyCounters[conversationKey] = current + 1
    }

    fun resetConsecutiveReplies(conversationKey: String) {
        consecutiveReplyCounters[conversationKey] = 0
    }

    private fun getStartOfDayTimestamp(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
