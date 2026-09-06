package com.example.service

import com.example.data.model.ContactRule
import com.example.data.model.SettingsData
import java.util.Calendar

data class FilterEvaluation(
    val shouldProcess: Boolean,
    val skipReason: String? = null
)

object SmartRuleEvaluator {

    fun evaluate(
        senderName: String,
        isGroup: Boolean,
        settings: SettingsData,
        contactRule: ContactRule? = null
    ): FilterEvaluation {
        // 1. Emergency Kill Switch
        if (settings.emergencyKillSwitch) {
            return FilterEvaluation(false, "Emergency Kill Switch is active")
        }

        // 2. Global AI Auto Reply Enabled
        if (!settings.aiAutoReplyEnabled) {
            return FilterEvaluation(false, "Auto-reply is paused")
        }

        // 3. Contact specific override
        if (contactRule != null && !contactRule.autoReplyEnabled) {
            return FilterEvaluation(false, "Auto-reply disabled for contact: $senderName")
        }

        // 4. Ignored contacts list
        if (settings.ignoredContacts.contains(senderName)) {
            return FilterEvaluation(false, "Contact is in Ignored List: $senderName")
        }

        // 5. Allowed contacts list (if replyOnlyContacts is true)
        if (settings.replyOnlyContacts && !settings.allowedContacts.contains(senderName)) {
            return FilterEvaluation(false, "Not in Allowed Contacts list: $senderName")
        }

        // 6. Unknown number check
        if (settings.ignoreUnknownNumbers) {
            val isPhoneNumber = senderName.replace(Regex("[\\s+\\-]"), "").all { it.isDigit() }
            if (isPhoneNumber) {
                return FilterEvaluation(false, "Ignoring unknown number: $senderName")
            }
        }

        // 7. Group Mode Check
        if (isGroup) {
            when (settings.groupMode) {
                SettingsData.GROUP_MODE_DISABLED -> {
                    return FilterEvaluation(false, "Groups are disabled")
                }
                SettingsData.GROUP_MODE_SELECTED -> {
                    if (!settings.allowedGroups.contains(senderName)) {
                        return FilterEvaluation(false, "Group not in selected allowed groups: $senderName")
                    }
                }
                SettingsData.GROUP_MODE_MENTION_ONLY -> {
                    // Handled in message parsing (requires @ tag)
                }
                SettingsData.GROUP_MODE_ALL -> {
                    // Allowed
                }
            }
        }

        // 8. Quiet Schedule Check
        if (settings.quietHoursEnabled && isInQuietHours(settings)) {
            return FilterEvaluation(false, "Quiet hours active (Sleeping / Focus time)")
        }

        return FilterEvaluation(true)
    }

    fun isInQuietHours(settings: SettingsData): Boolean {
        if (!settings.quietHoursEnabled) return false

        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = settings.quietStartHour * 60 + settings.quietStartMinute
        val endMinutes = settings.quietEndHour * 60 + settings.quietEndMinute

        return if (startMinutes < endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            // Overnight window (e.g. 22:00 to 07:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }
}
