package com.example.ai

enum class MessageIntent {
    GREETING,
    QUESTION,
    REQUEST,
    CASUAL_CHAT,
    FOLLOW_UP,
    URGENT,
    FINANCIAL_SENSITIVE,
    UNKNOWN
}

enum class MessagePriority {
    LOW,
    NORMAL,
    IMPORTANT,
    SENSITIVE
}

data class MessageAnalysis(
    val intent: MessageIntent,
    val priority: MessagePriority,
    val detectedLanguage: String,
    val isActionable: Boolean,
    val explanation: String
)

object MessageIntelligence {

    private val GREETINGS = listOf(
        "hi", "hello", "hey", "namaste", "pranam", "salaam", "kya haal",
        "good morning", "good evening", "good night", "gm", "gn", "sup", "yo", "wassup"
    )

    private val QUESTIONS_STARTERS = listOf(
        "what", "when", "where", "why", "how", "who", "which", "whose",
        "kahan", "kab", "kyun", "kaise", "kya", "kitna", "kon", "kisne",
        "can you", "are you", "will you", "could you", "do you", "is it"
    )

    private val REQUESTS_WORDS = listOf(
        "please", "send", "share", "give", "help", "forward", "tell",
        "bhejo", "dedo", "batao", "bhej do", "de do", "kardo", "kar dena"
    )

    private val FOLLOW_UPS = listOf(
        "and then", "what about", "aur batao", "phir kya hua", "any update",
        "kya hua", "update do", "remind me", "did you", "done?"
    )

    private val URGENT_WORDS = listOf(
        "urgent", "asap", "emergency", "immediately", "turant", "jaldi", "fast", "right now", "imp"
    )

    fun analyze(message: String): MessageAnalysis {
        val trimmed = message.trim()
        val lower = trimmed.lowercase()

        // 1. Check Sensitivity first
        val sensitivity = SensitiveMessageDetector.evaluate(trimmed)
        if (sensitivity.isSensitive) {
            return MessageAnalysis(
                intent = MessageIntent.FINANCIAL_SENSITIVE,
                priority = MessagePriority.SENSITIVE,
                detectedLanguage = detectLanguage(trimmed),
                isActionable = false,
                explanation = sensitivity.reason ?: "Sensitive topic detected"
            )
        }

        // 2. Urgent / Important priority
        val isUrgent = URGENT_WORDS.any { containsWord(lower, it) }
        val priority = if (isUrgent) MessagePriority.IMPORTANT else MessagePriority.NORMAL

        // 3. Detect Intent
        val intent = when {
            isUrgent -> MessageIntent.URGENT
            FOLLOW_UPS.any { lower.contains(it) } -> MessageIntent.FOLLOW_UP
            lower.endsWith("?") || QUESTIONS_STARTERS.any { lower.startsWith(it) || lower.contains(" $it ") } -> MessageIntent.QUESTION
            REQUESTS_WORDS.any { containsWord(lower, it) } -> MessageIntent.REQUEST
            GREETINGS.any { lower == it || lower.startsWith("$it ") || lower.endsWith(" $it") } -> MessageIntent.GREETING
            else -> MessageIntent.CASUAL_CHAT
        }

        val lang = detectLanguage(trimmed)

        return MessageAnalysis(
            intent = intent,
            priority = priority,
            detectedLanguage = lang,
            isActionable = intent != MessageIntent.CASUAL_CHAT || isUrgent,
            explanation = "Intent: ${intent.name}, Priority: ${priority.name}, Lang: $lang"
        )
    }

    fun detectLanguage(text: String): String {
        val hindiCharCount = text.count { it in '\u0900'..'\u097F' }
        val bengaliCharCount = text.count { it in '\u0980'..'\u09FF' }
        val gurmukhiCharCount = text.count { it in '\u0A00'..'\u0A7F' }
        val urduCharCount = text.count { it in '\u0600'..'\u06FF' }

        val totalLen = text.length.coerceAtLeast(1)

        if (hindiCharCount.toFloat() / totalLen > 0.25f) return "Hindi"
        if (bengaliCharCount.toFloat() / totalLen > 0.25f) return "Bengali"
        if (gurmukhiCharCount.toFloat() / totalLen > 0.25f) return "Punjabi"
        if (urduCharCount.toFloat() / totalLen > 0.25f) return "Urdu"

        // Check for Hinglish (Latin letters but Hindi/Urdu romanized words)
        val hinglishTokens = listOf(
            "hai", "haan", "nahi", "kya", "kaha", "kahan", "kaise", "bhai", "yaar",
            "karo", "karna", "baat", "aao", "jao", "theek", "thik", "achha", "achaa",
            "kuch", "sab", "ab", "kab", "mein", "hum", "tum", "aap", "mera", "meri",
            "apna", "shukriya", "dhanyawad", "milte", "kal", "aaj"
        )
        val lower = text.lowercase()
        val words = lower.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
        val hinglishCount = words.count { it in hinglishTokens }

        if (hinglishCount >= 2 || (words.size in 1..4 && hinglishCount >= 1)) {
            return "Hinglish"
        }

        return "English"
    }

    private fun containsWord(text: String, word: String): Boolean {
        if (!text.contains(word)) return false
        val regex = Regex("(^|\\b|\\s)${Regex.escape(word)}(\\b|\\s|$)")
        return regex.containsMatchIn(text)
    }
}
