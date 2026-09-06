package com.example.data.model

data class SettingsData(
    val aiAutoReplyEnabled: Boolean = false,
    val aiProvider: String = PROVIDER_GEMINI, // "gemini" or "openai"
    val replyStyle: String = PROFILE_FRIENDLY,
    val customInstructions: String = "",
    val approvalMode: Boolean = true, // Default to approval mode for personal safety
    val replyOnlyContacts: Boolean = false,
    val allowedContacts: Set<String> = emptySet(),
    val ignoredContacts: Set<String> = emptySet(),
    val ignoreGroups: Boolean = true,
    val groupMode: String = GROUP_MODE_DISABLED, // "DISABLED", "ALL", "MENTION_ONLY", "SELECTED"
    val allowedGroups: Set<String> = emptySet(),
    val ignoreUnknownNumbers: Boolean = false,
    val replyDelaySeconds: Int = 5,
    val maxRepliesPerConversation: Int = 3,
    val dailyReplyLimit: Int = 25,
    val includeSignature: Boolean = true,
    val signatureText: String = "— AI assistant",

    // Smart Pro features
    val preferredLanguage: String = LANG_AUTO,
    val replyLength: String = LENGTH_SHORT,
    val conversationCooldownSeconds: Int = 30, // Cooldown between automatic consecutive replies
    val smartBatchingEnabled: Boolean = true,
    val batchWindowSeconds: Int = 5, // Wait window to aggregate rapid fire messages
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 22, // 10 PM
    val quietStartMinute: Int = 0,
    val quietEndHour: Int = 7,   // 7 AM
    val quietEndMinute: Int = 0,
    val emergencyKillSwitch: Boolean = false, // Instant global pause
    val appLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val customNaturalLanguageRules: String = "", // e.g. "If anyone asks for dinner, say I am free after 8"
    val smartFallbackEnabled: Boolean = true, // Fallback to OpenAI if Gemini fails or vice-versa
    val selectedWhatsAppPackage: String = PACKAGE_BOTH // "both", "com.whatsapp", or "com.whatsapp.w4b"
) {
    companion object {
        const val PACKAGE_WHATSAPP = "com.whatsapp"
        const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"
        const val PACKAGE_BOTH = "both"

        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_OPENAI = "openai"

        // Personality Profiles
        const val PROFILE_FRIENDLY = "Friendly"
        const val PROFILE_CASUAL = "Casual"
        const val PROFILE_PROFESSIONAL = "Professional"
        const val PROFILE_FUNNY = "Funny"
        const val PROFILE_SHORT_DIRECT = "Short & Direct"
        const val PROFILE_CUSTOM = "Custom"

        val ALL_PROFILES = listOf(
            PROFILE_FRIENDLY,
            PROFILE_CASUAL,
            PROFILE_PROFESSIONAL,
            PROFILE_FUNNY,
            PROFILE_SHORT_DIRECT,
            PROFILE_CUSTOM
        )

        // Languages
        const val LANG_AUTO = "Auto Detect"
        const val LANG_ENGLISH = "English"
        const val LANG_HINDI = "Hindi"
        const val LANG_HINGLISH = "Hinglish"
        const val LANG_URDU = "Urdu"
        const val LANG_PUNJABI = "Punjabi"
        const val LANG_BENGALI = "Bengali"

        val ALL_LANGUAGES = listOf(
            LANG_AUTO,
            LANG_ENGLISH,
            LANG_HINDI,
            LANG_HINGLISH,
            LANG_URDU,
            LANG_PUNJABI,
            LANG_BENGALI
        )

        // Reply Lengths
        const val LENGTH_VERY_SHORT = "Very Short (1 line)"
        const val LENGTH_SHORT = "Short (1-2 sentences)"
        const val LENGTH_MEDIUM = "Medium (2-3 sentences)"
        const val LENGTH_DETAILED = "Detailed"

        val ALL_LENGTHS = listOf(
            LENGTH_VERY_SHORT,
            LENGTH_SHORT,
            LENGTH_MEDIUM,
            LENGTH_DETAILED
        )

        // Group Modes
        const val GROUP_MODE_DISABLED = "OFF"
        const val GROUP_MODE_MENTION_ONLY = "Mention Only (@me)"
        const val GROUP_MODE_SELECTED = "Selected Groups Only"
        const val GROUP_MODE_ALL = "All Groups"

        val ALL_GROUP_MODES = listOf(
            GROUP_MODE_DISABLED,
            GROUP_MODE_MENTION_ONLY,
            GROUP_MODE_SELECTED,
            GROUP_MODE_ALL
        )

        // Legacy compatibility
        val DEFAULT_STYLES = ALL_PROFILES
    }
}
