package com.example.ai

import com.example.BuildConfig
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

class GeminiProvider(
    private val keyStorage: SecureKeyStorage
) : AIProvider {

    override val id: String = "gemini"
    override val displayName: String = "Google Gemini"

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
        // Priority: User's stored key -> BuildConfig key fallback
        val userKey = keyStorage.getGeminiApiKey()
        val apiKey = when {
            userKey.isNotBlank() -> userKey
            runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("").isNotBlank() &&
                    BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Add an AI API key in Settings.")
            )
        }

        try {
            val systemPrompt = buildSystemPrompt(replyStyle, customInstructions)
            val userPrompt = buildUserPrompt(senderName, incomingMessage, conversationHistory)

            val requestJson = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", userPrompt))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 200)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            // Using gemini-2.5-flash as per gemini-api skill
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
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
            val candidates = jsonObject.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val generatedText = parts?.optJSONObject(0)?.optString("text")?.trim()

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
            append("You are ReplyMate, a personal WhatsApp reply assistant on behalf of the smartphone owner. ")
            append("Reply directly as the person receiving the message. ")
            append("Your replies must sound natural, concise, and human. ")
            append("Reply Style: $replyStyle. ")
            if (customInstructions.isNotBlank()) {
                append("Owner's special instructions: $customInstructions. ")
            }
            append("Guidelines: ")
            append("1. Keep reply to 1-3 sentences maximum. ")
            append("2. Do not quote the message or say 'Here is your reply:'. ")
            append("3. Output ONLY the reply text itself. ")
            append("4. Match the language/tone of the sender (e.g. Hindi/Hinglish/English).")
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
