package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.SettingsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("replymate_user_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<SettingsData> = _settingsFlow.asStateFlow()

    private fun loadSettings(): SettingsData {
        return SettingsData(
            aiAutoReplyEnabled = prefs.getBoolean(KEY_AI_ENABLED, false),
            aiProvider = prefs.getString(KEY_AI_PROVIDER, SettingsData.PROVIDER_GEMINI) ?: SettingsData.PROVIDER_GEMINI,
            replyStyle = prefs.getString(KEY_REPLY_STYLE, SettingsData.STYLE_CASUAL_SHORT) ?: SettingsData.STYLE_CASUAL_SHORT,
            customInstructions = prefs.getString(KEY_CUSTOM_INSTRUCTIONS, "") ?: "",
            approvalMode = prefs.getBoolean(KEY_APPROVAL_MODE, true),
            replyOnlyContacts = prefs.getBoolean(KEY_REPLY_ONLY_CONTACTS, false),
            allowedContacts = prefs.getStringSet(KEY_ALLOWED_CONTACTS, emptySet()) ?: emptySet(),
            ignoredContacts = prefs.getStringSet(KEY_IGNORED_CONTACTS, emptySet()) ?: emptySet(),
            ignoreGroups = prefs.getBoolean(KEY_IGNORE_GROUPS, true),
            ignoreUnknownNumbers = prefs.getBoolean(KEY_IGNORE_UNKNOWN_NUMBERS, false),
            replyDelaySeconds = prefs.getInt(KEY_REPLY_DELAY, 5),
            maxRepliesPerConversation = prefs.getInt(KEY_MAX_REPLIES, 3),
            dailyReplyLimit = prefs.getInt(KEY_DAILY_LIMIT, 25),
            includeSignature = prefs.getBoolean(KEY_INCLUDE_SIGNATURE, true),
            signatureText = prefs.getString(KEY_SIGNATURE_TEXT, "— AI assistant") ?: "— AI assistant"
        )
    }

    fun updateSettings(newSettings: SettingsData) {
        prefs.edit()
            .putBoolean(KEY_AI_ENABLED, newSettings.aiAutoReplyEnabled)
            .putString(KEY_AI_PROVIDER, newSettings.aiProvider)
            .putString(KEY_REPLY_STYLE, newSettings.replyStyle)
            .putString(KEY_CUSTOM_INSTRUCTIONS, newSettings.customInstructions)
            .putBoolean(KEY_APPROVAL_MODE, newSettings.approvalMode)
            .putBoolean(KEY_REPLY_ONLY_CONTACTS, newSettings.replyOnlyContacts)
            .putStringSet(KEY_ALLOWED_CONTACTS, newSettings.allowedContacts)
            .putStringSet(KEY_IGNORED_CONTACTS, newSettings.ignoredContacts)
            .putBoolean(KEY_IGNORE_GROUPS, newSettings.ignoreGroups)
            .putBoolean(KEY_IGNORE_UNKNOWN_NUMBERS, newSettings.ignoreUnknownNumbers)
            .putInt(KEY_REPLY_DELAY, newSettings.replyDelaySeconds)
            .putInt(KEY_MAX_REPLIES, newSettings.maxRepliesPerConversation)
            .putInt(KEY_DAILY_LIMIT, newSettings.dailyReplyLimit)
            .putBoolean(KEY_INCLUDE_SIGNATURE, newSettings.includeSignature)
            .putString(KEY_SIGNATURE_TEXT, newSettings.signatureText)
            .apply()

        _settingsFlow.value = newSettings
    }

    fun toggleAutoReply(enabled: Boolean) {
        val updated = _settingsFlow.value.copy(aiAutoReplyEnabled = enabled)
        updateSettings(updated)
    }

    fun setAiProvider(provider: String) {
        val updated = _settingsFlow.value.copy(aiProvider = provider)
        updateSettings(updated)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _settingsFlow.value = SettingsData()
    }

    companion object {
        private const val KEY_AI_ENABLED = "key_ai_auto_reply_enabled"
        private const val KEY_AI_PROVIDER = "key_ai_provider"
        private const val KEY_REPLY_STYLE = "key_reply_style"
        private const val KEY_CUSTOM_INSTRUCTIONS = "key_custom_instructions"
        private const val KEY_APPROVAL_MODE = "key_approval_mode"
        private const val KEY_REPLY_ONLY_CONTACTS = "key_reply_only_contacts"
        private const val KEY_ALLOWED_CONTACTS = "key_allowed_contacts"
        private const val KEY_IGNORED_CONTACTS = "key_ignored_contacts"
        private const val KEY_IGNORE_GROUPS = "key_ignore_groups"
        private const val KEY_IGNORE_UNKNOWN_NUMBERS = "key_ignore_unknown_numbers"
        private const val KEY_REPLY_DELAY = "key_reply_delay"
        private const val KEY_MAX_REPLIES = "key_max_replies"
        private const val KEY_DAILY_LIMIT = "key_daily_limit"
        private const val KEY_INCLUDE_SIGNATURE = "key_include_signature"
        private const val KEY_SIGNATURE_TEXT = "key_signature_text"
    }
}
