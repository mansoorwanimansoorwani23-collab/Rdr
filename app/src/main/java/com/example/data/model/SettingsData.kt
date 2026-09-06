package com.example.data.model

data class SettingsData(
    val aiAutoReplyEnabled: Boolean = false,
    val aiProvider: String = PROVIDER_GEMINI, // "gemini" or "openai"
    val replyStyle: String = STYLE_CASUAL_SHORT,
    val customInstructions: String = "",
    val approvalMode: Boolean = true, // Default to approval mode for personal safety
    val replyOnlyContacts: Boolean = false,
    val allowedContacts: Set<String> = emptySet(),
    val ignoredContacts: Set<String> = emptySet(),
    val ignoreGroups: Boolean = true,
    val ignoreUnknownNumbers: Boolean = false,
    val replyDelaySeconds: Int = 5,
    val maxRepliesPerConversation: Int = 3,
    val dailyReplyLimit: Int = 25,
    val includeSignature: Boolean = true,
    val signatureText: String = "— AI assistant"
) {
    companion object {
        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_OPENAI = "openai"

        const val STYLE_CASUAL_SHORT = "Casual & Short"
        const val STYLE_FRIENDLY_HINGLISH = "Friendly Hinglish"
        const val STYLE_WARM_EMOJIS = "Warm with Emojis"
        const val STYLE_FORMAL = "Professional & Polite"
        const val STYLE_HINDI = "Hindi (Natural)"
        const val STYLE_ENGLISH = "English (Concise)"

        val DEFAULT_STYLES = listOf(
            STYLE_CASUAL_SHORT,
            STYLE_FRIENDLY_HINGLISH,
            STYLE_WARM_EMOJIS,
            STYLE_FORMAL,
            STYLE_HINDI,
            STYLE_ENGLISH
        )
    }
}
