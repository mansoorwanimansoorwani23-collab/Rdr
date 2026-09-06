package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ContactRule
import com.example.data.model.SettingsData
import com.example.data.model.StyleAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class PreferencesRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("replymate_user_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<SettingsData> = _settingsFlow.asStateFlow()

    private val _contactRulesFlow = MutableStateFlow(loadContactRules())
    val contactRulesFlow: StateFlow<Map<String, ContactRule>> = _contactRulesFlow.asStateFlow()

    private val _userSampleMessages = MutableStateFlow(loadUserSamples())
    val userSampleMessages: StateFlow<List<String>> = _userSampleMessages.asStateFlow()

    private val _styleAnalysisFlow = MutableStateFlow(loadStyleAnalysis())
    val styleAnalysisFlow: StateFlow<StyleAnalysis> = _styleAnalysisFlow.asStateFlow()

    private fun loadSettings(): SettingsData {
        return SettingsData(
            aiAutoReplyEnabled = prefs.getBoolean(KEY_AI_ENABLED, false),
            aiProvider = prefs.getString(KEY_AI_PROVIDER, SettingsData.PROVIDER_GEMINI) ?: SettingsData.PROVIDER_GEMINI,
            replyStyle = prefs.getString(KEY_REPLY_STYLE, SettingsData.PROFILE_FRIENDLY) ?: SettingsData.PROFILE_FRIENDLY,
            customInstructions = prefs.getString(KEY_CUSTOM_INSTRUCTIONS, "") ?: "",
            approvalMode = prefs.getBoolean(KEY_APPROVAL_MODE, true),
            replyOnlyContacts = prefs.getBoolean(KEY_REPLY_ONLY_CONTACTS, false),
            allowedContacts = prefs.getStringSet(KEY_ALLOWED_CONTACTS, emptySet()) ?: emptySet(),
            ignoredContacts = prefs.getStringSet(KEY_IGNORED_CONTACTS, emptySet()) ?: emptySet(),
            ignoreGroups = prefs.getBoolean(KEY_IGNORE_GROUPS, true),
            groupMode = prefs.getString(KEY_GROUP_MODE, SettingsData.GROUP_MODE_DISABLED) ?: SettingsData.GROUP_MODE_DISABLED,
            allowedGroups = prefs.getStringSet(KEY_ALLOWED_GROUPS, emptySet()) ?: emptySet(),
            ignoreUnknownNumbers = prefs.getBoolean(KEY_IGNORE_UNKNOWN_NUMBERS, false),
            replyDelaySeconds = prefs.getInt(KEY_REPLY_DELAY, 5),
            maxRepliesPerConversation = prefs.getInt(KEY_MAX_REPLIES, 3),
            dailyReplyLimit = prefs.getInt(KEY_DAILY_LIMIT, 25),
            includeSignature = prefs.getBoolean(KEY_INCLUDE_SIGNATURE, true),
            signatureText = prefs.getString(KEY_SIGNATURE_TEXT, "— AI assistant") ?: "— AI assistant",

            preferredLanguage = prefs.getString(KEY_PREFERRED_LANG, SettingsData.LANG_AUTO) ?: SettingsData.LANG_AUTO,
            replyLength = prefs.getString(KEY_REPLY_LENGTH, SettingsData.LENGTH_SHORT) ?: SettingsData.LENGTH_SHORT,
            conversationCooldownSeconds = prefs.getInt(KEY_COOLDOWN, 30),
            smartBatchingEnabled = prefs.getBoolean(KEY_BATCHING_ENABLED, true),
            batchWindowSeconds = prefs.getInt(KEY_BATCH_WINDOW, 5),
            quietHoursEnabled = prefs.getBoolean(KEY_QUIET_ENABLED, false),
            quietStartHour = prefs.getInt(KEY_QUIET_START_H, 22),
            quietStartMinute = prefs.getInt(KEY_QUIET_START_M, 0),
            quietEndHour = prefs.getInt(KEY_QUIET_END_H, 7),
            quietEndMinute = prefs.getInt(KEY_QUIET_END_M, 0),
            emergencyKillSwitch = prefs.getBoolean(KEY_KILL_SWITCH, false),
            appLockEnabled = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false),
            appLockPin = prefs.getString(KEY_APP_LOCK_PIN, "") ?: "",
            customNaturalLanguageRules = prefs.getString(KEY_NATURAL_RULES, "") ?: "",
            smartFallbackEnabled = prefs.getBoolean(KEY_SMART_FALLBACK, true)
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
            .putString(KEY_GROUP_MODE, newSettings.groupMode)
            .putStringSet(KEY_ALLOWED_GROUPS, newSettings.allowedGroups)
            .putBoolean(KEY_IGNORE_UNKNOWN_NUMBERS, newSettings.ignoreUnknownNumbers)
            .putInt(KEY_REPLY_DELAY, newSettings.replyDelaySeconds)
            .putInt(KEY_MAX_REPLIES, newSettings.maxRepliesPerConversation)
            .putInt(KEY_DAILY_LIMIT, newSettings.dailyReplyLimit)
            .putBoolean(KEY_INCLUDE_SIGNATURE, newSettings.includeSignature)
            .putString(KEY_SIGNATURE_TEXT, newSettings.signatureText)

            .putString(KEY_PREFERRED_LANG, newSettings.preferredLanguage)
            .putString(KEY_REPLY_LENGTH, newSettings.replyLength)
            .putInt(KEY_COOLDOWN, newSettings.conversationCooldownSeconds)
            .putBoolean(KEY_BATCHING_ENABLED, newSettings.smartBatchingEnabled)
            .putInt(KEY_BATCH_WINDOW, newSettings.batchWindowSeconds)
            .putBoolean(KEY_QUIET_ENABLED, newSettings.quietHoursEnabled)
            .putInt(KEY_QUIET_START_H, newSettings.quietStartHour)
            .putInt(KEY_QUIET_START_M, newSettings.quietStartMinute)
            .putInt(KEY_QUIET_END_H, newSettings.quietEndHour)
            .putInt(KEY_QUIET_END_M, newSettings.quietEndMinute)
            .putBoolean(KEY_KILL_SWITCH, newSettings.emergencyKillSwitch)
            .putBoolean(KEY_APP_LOCK_ENABLED, newSettings.appLockEnabled)
            .putString(KEY_APP_LOCK_PIN, newSettings.appLockPin)
            .putString(KEY_NATURAL_RULES, newSettings.customNaturalLanguageRules)
            .putBoolean(KEY_SMART_FALLBACK, newSettings.smartFallbackEnabled)
            .apply()

        _settingsFlow.value = newSettings
    }

    fun toggleAutoReply(enabled: Boolean) {
        val updated = _settingsFlow.value.copy(aiAutoReplyEnabled = enabled)
        updateSettings(updated)
    }

    fun setEmergencyKillSwitch(kill: Boolean) {
        val updated = _settingsFlow.value.copy(emergencyKillSwitch = kill)
        updateSettings(updated)
    }

    // Contact Rules Management
    private fun loadContactRules(): Map<String, ContactRule> {
        val jsonStr = prefs.getString(KEY_CONTACT_RULES_JSON, null) ?: return emptyMap()
        return try {
            val json = JSONObject(jsonStr)
            val result = mutableMapOf<String, ContactRule>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.getJSONObject(key)
                result[key] = ContactRule(
                    contactName = obj.optString("contactName", key),
                    autoReplyEnabled = obj.optBoolean("autoReplyEnabled", true),
                    personality = obj.optString("personality", SettingsData.PROFILE_FRIENDLY),
                    replyLength = obj.optString("replyLength", SettingsData.LENGTH_SHORT),
                    preferredLanguage = obj.optString("preferredLanguage", SettingsData.LANG_AUTO),
                    memoryEnabled = obj.optBoolean("memoryEnabled", true),
                    approvalMode = obj.optBoolean("approvalMode", true),
                    customNotes = obj.optString("customNotes", ""),
                    customInstructions = obj.optString("customInstructions", "")
                )
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveContactRule(rule: ContactRule) {
        val current = _contactRulesFlow.value.toMutableMap()
        current[rule.contactName] = rule
        saveContactRulesMap(current)
    }

    fun deleteContactRule(contactName: String) {
        val current = _contactRulesFlow.value.toMutableMap()
        current.remove(contactName)
        saveContactRulesMap(current)
    }

    private fun saveContactRulesMap(rulesMap: Map<String, ContactRule>) {
        val json = JSONObject()
        for ((key, rule) in rulesMap) {
            val obj = JSONObject().apply {
                put("contactName", rule.contactName)
                put("autoReplyEnabled", rule.autoReplyEnabled)
                put("personality", rule.personality)
                put("replyLength", rule.replyLength)
                put("preferredLanguage", rule.preferredLanguage)
                put("memoryEnabled", rule.memoryEnabled)
                put("approvalMode", rule.approvalMode)
                put("customNotes", rule.customNotes)
                put("customInstructions", rule.customInstructions)
            }
            json.put(key, obj)
        }
        prefs.edit().putString(KEY_CONTACT_RULES_JSON, json.toString()).apply()
        _contactRulesFlow.value = rulesMap
    }

    // Learn My Style Sample Messages
    private fun loadUserSamples(): List<String> {
        val raw = prefs.getString(KEY_USER_SAMPLES_JSON, null) ?: return listOf(
            "Haan bhai, bilkul! Main thodi der mein call karta hu.",
            "Sounds good! Let me check with team and get back to you.",
            "Kal sham ko milte hain coffee pe?"
        )
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveUserSamples(samples: List<String>) {
        val array = JSONArray()
        samples.forEach { array.put(it) }
        prefs.edit().putString(KEY_USER_SAMPLES_JSON, array.toString()).apply()
        _userSampleMessages.value = samples
    }

    fun saveStyleAnalysis(analysis: StyleAnalysis) {
        val json = JSONObject().apply {
            put("detectedLanguage", analysis.detectedLanguage)
            put("avgSentenceLength", analysis.avgSentenceLength)
            put("emojiFrequency", analysis.emojiFrequency)
            put("punctuationStyle", analysis.punctuationStyle)
            put("tone", analysis.tone)
            put("customPromptSnippet", analysis.customPromptSnippet)
            val commonArray = JSONArray()
            analysis.commonExpressions.forEach { commonArray.put(it) }
            put("commonExpressions", commonArray)
        }
        prefs.edit().putString(KEY_STYLE_ANALYSIS_JSON, json.toString()).apply()
        _styleAnalysisFlow.value = analysis
    }

    private fun loadStyleAnalysis(): StyleAnalysis {
        val raw = prefs.getString(KEY_STYLE_ANALYSIS_JSON, null) ?: return StyleAnalysis()
        return try {
            val json = JSONObject(raw)
            val commonArray = json.optJSONArray("commonExpressions")
            val expressions = mutableListOf<String>()
            if (commonArray != null) {
                for (i in 0 until commonArray.length()) {
                    expressions.add(commonArray.getString(i))
                }
            }
            StyleAnalysis(
                detectedLanguage = json.optString("detectedLanguage", "Hinglish"),
                avgSentenceLength = json.optString("avgSentenceLength", "Short (1-2 sentences)"),
                emojiFrequency = json.optString("emojiFrequency", "Moderate (1 emoji per text)"),
                punctuationStyle = json.optString("punctuationStyle", "Casual"),
                tone = json.optString("tone", "Warm casual"),
                commonExpressions = expressions.ifEmpty { listOf("Haan", "Sounds good") },
                customPromptSnippet = json.optString("customPromptSnippet", "")
            )
        } catch (e: Exception) {
            StyleAnalysis()
        }
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _settingsFlow.value = SettingsData()
        _contactRulesFlow.value = emptyMap()
        _userSampleMessages.value = emptyList()
        _styleAnalysisFlow.value = StyleAnalysis()
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
        private const val KEY_GROUP_MODE = "key_group_mode"
        private const val KEY_ALLOWED_GROUPS = "key_allowed_groups"
        private const val KEY_IGNORE_UNKNOWN_NUMBERS = "key_ignore_unknown_numbers"
        private const val KEY_REPLY_DELAY = "key_reply_delay"
        private const val KEY_MAX_REPLIES = "key_max_replies"
        private const val KEY_DAILY_LIMIT = "key_daily_limit"
        private const val KEY_INCLUDE_SIGNATURE = "key_include_signature"
        private const val KEY_SIGNATURE_TEXT = "key_signature_text"

        private const val KEY_PREFERRED_LANG = "key_preferred_language"
        private const val KEY_REPLY_LENGTH = "key_reply_length"
        private const val KEY_COOLDOWN = "key_conversation_cooldown"
        private const val KEY_BATCHING_ENABLED = "key_batching_enabled"
        private const val KEY_BATCH_WINDOW = "key_batch_window"
        private const val KEY_QUIET_ENABLED = "key_quiet_enabled"
        private const val KEY_QUIET_START_H = "key_quiet_start_h"
        private const val KEY_QUIET_START_M = "key_quiet_start_m"
        private const val KEY_QUIET_END_H = "key_quiet_end_h"
        private const val KEY_QUIET_END_M = "key_quiet_end_m"
        private const val KEY_KILL_SWITCH = "key_kill_switch"
        private const val KEY_APP_LOCK_ENABLED = "key_app_lock_enabled"
        private const val KEY_APP_LOCK_PIN = "key_app_lock_pin"
        private const val KEY_NATURAL_RULES = "key_natural_rules"
        private const val KEY_SMART_FALLBACK = "key_smart_fallback"

        private const val KEY_CONTACT_RULES_JSON = "key_contact_rules_json"
        private const val KEY_USER_SAMPLES_JSON = "key_user_samples_json"
        private const val KEY_STYLE_ANALYSIS_JSON = "key_style_analysis_json"
    }
}
