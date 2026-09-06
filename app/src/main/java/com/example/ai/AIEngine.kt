package com.example.ai

import com.example.data.local.AppDatabase
import com.example.data.model.SettingsData
import com.example.data.security.SecureKeyStorage

sealed class AIReplyResult {
    data class Success(val replyText: String, val provider: String) : AIReplyResult()
    data class Sensitive(val reason: String) : AIReplyResult()
    data class Error(val errorMessage: String) : AIReplyResult()
}

class AIEngine(
    private val keyStorage: SecureKeyStorage,
    private val database: AppDatabase
) {
    private val geminiProvider = GeminiProvider(keyStorage)
    private val openAIProvider = OpenAIProvider(keyStorage)

    fun getProvider(providerId: String): AIProvider {
        return when (providerId.lowercase()) {
            "openai" -> openAIProvider
            else -> geminiProvider
        }
    }

    suspend fun processIncomingMessage(
        senderName: String,
        conversationKey: String,
        messageText: String,
        settings: SettingsData
    ): AIReplyResult {
        // 1. Safety & Sensitivity check
        val sensitivity = SensitiveMessageDetector.evaluate(messageText)
        if (sensitivity.isSensitive) {
            return AIReplyResult.Sensitive(
                reason = sensitivity.reason ?: "Potentially sensitive message — manual reply recommended."
            )
        }

        // 2. Fetch small local context for this conversation
        val recentContextEntities = database.conversationMessageDao()
            .getRecentContext(conversationKey, limit = 4)
        val conversationHistory = recentContextEntities.map {
            Pair(it.sender, it.messageText)
        }

        // 3. Resolve active provider
        val provider = getProvider(settings.aiProvider)

        val signature = if (settings.includeSignature) settings.signatureText else null

        // 4. Generate reply
        val result = provider.generateReply(
            incomingMessage = messageText,
            senderName = senderName,
            conversationHistory = conversationHistory,
            replyStyle = settings.replyStyle,
            customInstructions = settings.customInstructions,
            signature = signature
        )

        return result.fold(
            onSuccess = { reply ->
                AIReplyResult.Success(replyText = reply, provider = provider.displayName)
            },
            onFailure = { error ->
                AIReplyResult.Error(
                    errorMessage = error.message ?: "AI reply unavailable. Please reply manually."
                )
            }
        )
    }
}
