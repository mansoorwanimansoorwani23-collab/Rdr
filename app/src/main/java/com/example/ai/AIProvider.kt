package com.example.ai

interface AIProvider {
    val id: String
    val displayName: String

    suspend fun generateReply(
        incomingMessage: String,
        senderName: String,
        conversationHistory: List<Pair<String, String>>, // Pair(sender, text)
        replyStyle: String,
        customInstructions: String,
        signature: String?,
        preferredLanguage: String = "Auto Detect",
        replyLength: String = "Short (1-2 sentences)",
        contactNotes: String = "",
        contactMemories: List<String> = emptyList(),
        naturalRules: String = ""
    ): Result<String>
}
