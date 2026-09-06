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
        signature: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = keyStorage.getOpenAiApiKey()

        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Add an AI API key in Settings.")
            )
        }

        try {
            val systemPrompt = buildSystemPrompt(replyStyle, customInstructions)
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
            if (!signature.isNullOrBlank()) {
                cleanReply = "$cleanReply\n$signature"
            }

            Result.success(cleanReply)
        } catch (e: Exception) {
            Result.failure(Exception("AI reply unavailable. Please reply manually."))
        }
    }

    private fun buildSystemPrompt(replyStyle: String, customInstructions: String): String {
        return buildString {
            append("You are ReplyMate, a personal WhatsApp reply assistant on behalf of the user. ")
            append("Reply directly as the person receiving the message. ")
            append("Replies must be concise, helpful, human-like. ")
            append("Reply Style: $replyStyle. ")
            if (customInstructions.isNotBlank()) {
                append("Custom instructions from owner: $customInstructions. ")
            }
            append("Guidelines: Keep within 1-3 sentences. Output ONLY the reply text itself.")
        }
    }

    private fun buildUserPrompt(
        senderName: String,
        incomingMessage: String,
        conversationHistory: List<Pair<String, String>>
    ): String {
        return buildString {
            if (conversationHistory.isNotEmpty()) {
                append("Recent chat history:\n")
                conversationHistory.forEach { (sender, text) ->
                    append("$sender: $text\n")
                }
                append("\n")
            }
            append("New incoming WhatsApp message from $senderName:\n")
            append("\"$incomingMessage\"\n\n")
            append("Reply:")
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
