package com.example.ai

import com.example.data.local.AppDatabase
import com.example.data.model.ContactRule
import com.example.data.model.SettingsData
import com.example.data.security.SecureKeyStorage

sealed class AIReplyResult {
    data class Success(
        val replyText: String,
        val provider: String,
        val analysis: MessageAnalysis? = null
    ) : AIReplyResult()

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

    suspend fun verifyApiKey(providerId: String, key: String): Result<Boolean> {
        return when (providerId.lowercase()) {
            "openai" -> openAIProvider.verifyKey(key)
            else -> geminiProvider.verifyKey(key)
        }
    }

    suspend fun processIncomingMessage(
        senderName: String,
        conversationKey: String,
        messageText: String,
        settings: SettingsData,
        contactRule: ContactRule? = null
    ): AIReplyResult {
        // 1. Analyze message intent, priority and safety
        val analysis = MessageIntelligence.analyze(messageText)
        if (analysis.priority == MessagePriority.SENSITIVE) {
            return AIReplyResult.Sensitive(
                reason = analysis.explanation
            )
        }

        // 2. Fetch small local context for this conversation (last 5 messages)
        val recentContextEntities = database.conversationMessageDao()
            .getRecentContext(conversationKey, limit = 5)
        val conversationHistory = recentContextEntities.map {
            Pair(it.sender, it.messageText)
        }

        // 3. Fetch private local memories for this contact
        val memories = if (contactRule?.memoryEnabled != false) {
            database.contactMemoryDao().getMemoriesForContact(senderName).map { it.memoryFact }
        } else {
            emptyList()
        }

        // 4. Resolve effective configuration (Contact overrides or Global)
        val effectiveStyle = contactRule?.personality ?: settings.replyStyle
        val effectiveLength = contactRule?.replyLength ?: settings.replyLength
        val effectiveLang = contactRule?.preferredLanguage ?: settings.preferredLanguage
        val contactNotes = contactRule?.customNotes ?: ""
        val contactSpecificInstr = contactRule?.customInstructions ?: ""

        val combinedCustomInstr = buildString {
            if (settings.customInstructions.isNotBlank()) {
                append(settings.customInstructions)
                append(" ")
            }
            if (contactSpecificInstr.isNotBlank()) {
                append("For $senderName specifically: $contactSpecificInstr")
            }
        }.trim()

        val signature = if (settings.includeSignature) settings.signatureText else null

        // 5. Primary Provider attempt
        val primaryProvider = getProvider(settings.aiProvider)
        val fallbackProvider = if (settings.aiProvider == SettingsData.PROVIDER_GEMINI) openAIProvider else geminiProvider

        val primaryResult = primaryProvider.generateReply(
            incomingMessage = messageText,
            senderName = senderName,
            conversationHistory = conversationHistory,
            replyStyle = effectiveStyle,
            customInstructions = combinedCustomInstr,
            signature = signature,
            preferredLanguage = effectiveLang,
            replyLength = effectiveLength,
            contactNotes = contactNotes,
            contactMemories = memories,
            naturalRules = settings.customNaturalLanguageRules
        )

        if (primaryResult.isSuccess) {
            return AIReplyResult.Success(
                replyText = primaryResult.getOrThrow(),
                provider = primaryProvider.displayName,
                analysis = analysis
            )
        }

        // 6. Smart Fallback if enabled and primary failed
        if (settings.smartFallbackEnabled) {
            val fallbackResult = fallbackProvider.generateReply(
                incomingMessage = messageText,
                senderName = senderName,
                conversationHistory = conversationHistory,
                replyStyle = effectiveStyle,
                customInstructions = combinedCustomInstr,
                signature = signature,
                preferredLanguage = effectiveLang,
                replyLength = effectiveLength,
                contactNotes = contactNotes,
                contactMemories = memories,
                naturalRules = settings.customNaturalLanguageRules
            )

            if (fallbackResult.isSuccess) {
                return AIReplyResult.Success(
                    replyText = fallbackResult.getOrThrow(),
                    provider = "${fallbackProvider.displayName} (Fallback)",
                    analysis = analysis
                )
            }
        }

        val err = primaryResult.exceptionOrNull()?.message ?: "AI reply unavailable. Please reply manually."
        return AIReplyResult.Error(errorMessage = err)
    }
}
