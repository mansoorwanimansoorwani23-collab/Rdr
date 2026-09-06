package com.example.ai

import com.example.data.security.SecureKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAIProvider(
    private val keyStorage: SecureKeyStorage
) : AIProvider {

    override val id: String = "openai"
    override val displayName: String = "OpenAI"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun generateReply(
        incomingMessage: String,
        senderName: String,
        conversationHistory: List<Pair<String, String>>,
        replyStyle: String,
        customInstructions: String,
        signature: String?,
        preferredLanguage: String,
        replyLength: String,
        contactNotes: String,
        contactMemories: List<String>,
        naturalRules: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = keyStorage.getOpenAiApiKey()

        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Add an AI API key in Settings.")
            )
        }

        try {
            val systemPrompt = buildSystemPrompt(
                replyStyle = replyStyle,
                customInstructions = customInstructions,
                preferredLanguage = preferredLanguage,
                replyLength = replyLength,
                contactNotes = contactNotes,
                contactMemories = contactMemories,
                naturalRules = naturalRules
            )
            val userPrompt = buildUserPrompt(senderName, incomingMessage, conversationHistory)

            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }

            val requestJson = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("messages", messagesArray)
                put("temperature", 0.7)
                put("max_tokens", 200)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("AI reply unavailable. Please reply manually. (HTTP ${response.code})")
                )
            }

            val jsonObject = JSONObject(responseBody)
            val choices = jsonObject.optJSONArray("choices")
            val firstChoice = choices?.optJSONObject(0)
            val messageObj = firstChoice?.optJSONObject("message")
            val generatedText = messageObj?.optString("content")?.trim()

            if (generatedText.isNullOrBlank()) {
                return@withContext Result.failure(
                    Exception("AI reply unavailable. Please reply manually.")
                )
            }

            var cleanReply = cleanGeneratedReply(generatedText)

            // Validation Quality check
            val quality = ResponseQualityFilter.validateReply(cleanReply, incomingMessage)
            if (!quality.isValid) {
                return@withContext Result.failure(Exception(quality.reason ?: "AI output rejected by quality filter"))
            }

            if (!signature.isNullOrBlank()) {
                cleanReply = "$cleanReply\n$signature"
            }

            Result.success(cleanReply)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "AI reply unavailable. Please reply manually."))
        }
    }

    private fun buildSystemPrompt(
        replyStyle: String,
        customInstructions: String,
        preferredLanguage: String,
        replyLength: String,
        contactNotes: String,
        contactMemories: List<String>,
        naturalRules: String
    ): String {
        return buildString {
            append("You are ReplyMate, an intelligent WhatsApp reply assistant on behalf of the phone owner. ")
            append("Reply directly as the person receiving the message. Do NOT speak as an AI or third party. ")
            append("Personality profile: $replyStyle. ")
            append("Target reply length: $replyLength. ")

            if (preferredLanguage != "Auto Detect") {
                append("Language requirement: Strictly respond in $preferredLanguage. ")
            } else {
                append("Language requirement: Automatically match the sender's language and dialect (English, Hindi, Hinglish, Urdu, Punjabi, Bengali). ")
            }

            if (contactNotes.isNotBlank()) {
                append("Context about this contact: $contactNotes. ")
            }

            if (contactMemories.isNotEmpty()) {
                append("Known private memories about this contact: ")
                contactMemories.forEach { append("- $it. ") }
            }

            if (naturalRules.isNotBlank()) {
                append("Custom owner rules: $naturalRules. ")
            }

            if (customInstructions.isNotBlank()) {
                append("Special instructions: $customInstructions. ")
            }

            append("Rules: ")
            append("1. Be context-aware, avoid repetitive or generic phrasing. ")
            append("2. Keep replies natural and conversational for instant messaging. ")
            append("3. Never say 'Here is your reply' or 'As an AI'. Output ONLY the exact text message to be sent.")
        }
    }

    private fun buildUserPrompt(
        senderName: String,
        incomingMessage: String,
        conversationHistory: List<Pair<String, String>>
    ): String {
        return buildString {
            if (conversationHistory.isNotEmpty()) {
                append("Recent conversation context:\n")
                conversationHistory.forEach { (sender, text) ->
                    append("$sender: $text\n")
                }
                append("\n")
            }
            append("New incoming WhatsApp message from $senderName:\n")
            append("\"$incomingMessage\"\n\n")
            append("Generate the suggested reply:")
        }
    }

    private fun cleanGeneratedReply(text: String): String {
        var reply = text.trim()
        if (reply.startsWith("\"") && reply.endsWith("\"") && reply.length >= 2) {
            reply = reply.substring(1, reply.length - 1).trim()
        }
        return reply
    }
}
