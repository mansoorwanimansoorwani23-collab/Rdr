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
        signature: String?
    ): Result<String>
}
