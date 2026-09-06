package com.example.ai

import com.example.data.model.StyleAnalysis

object StyleAnalyzer {

    fun analyzeSamples(samples: List<String>): StyleAnalysis {
        val validSamples = samples.filter { it.isNotBlank() }
        if (validSamples.isEmpty()) {
            return StyleAnalysis()
        }

        // 1. Language detection
        val langs = validSamples.map { MessageIntelligence.detectLanguage(it) }
        val dominantLang = langs.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "Hinglish"

        // 2. Average sentence length
        val avgWords = validSamples.map { it.split(Regex("\\s+")).size }.average()
        val sentenceLengthDesc = when {
            avgWords < 5 -> "Very Short (1-4 words)"
            avgWords < 12 -> "Short (1-2 sentences)"
            avgWords < 25 -> "Medium (2-3 sentences)"
            else -> "Detailed"
        }

        // 3. Emoji frequency
        val emojiRegex = Regex("[\\p{So}\\p{Sk}\\p{Cs}\\x{1F300}-\\x{1F9FF}]")
        val totalEmojis = validSamples.sumOf { emojiRegex.findAll(it).count() }
        val emojiRatio = totalEmojis.toFloat() / validSamples.size
        val emojiFreqDesc = when {
            emojiRatio < 0.2f -> "Rare / None"
            emojiRatio < 1.2f -> "Moderate (1 emoji per text)"
            else -> "Expressive / Frequent"
        }

        // 4. Punctuation
        val exclamationCount = validSamples.sumOf { it.count { c -> c == '!' } }
        val periodCount = validSamples.sumOf { it.count { c -> c == '.' } }
        val punctuationDesc = when {
            exclamationCount > periodCount -> "Enthusiastic & warm (!)"
            periodCount > 0 -> "Structured & punctuated"
            else -> "Casual (minimal punctuation)"
        }

        // 5. Common expressions
        val sampleWords = validSamples.flatMap { it.split(Regex("[^a-zA-Z0-9]+")) }
            .map { it.trim().lowercase() }
            .filter { it.length > 2 && !isStopWord(it) }
        val topWords = sampleWords.groupBy { it }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first.replaceFirstChar { c -> c.uppercase() } }

        val commonExpr = if (topWords.isNotEmpty()) topWords else listOf("Haan", "Sure", "Will check", "Sounds good")

        val tone = when {
            dominantLang == "Hinglish" -> "Warm casual Hinglish, friendly texting"
            emojiRatio > 0.8f -> "Upbeat, energetic and expressive"
            avgWords < 6 -> "Direct, swift and minimalist"
            else -> "Polite, helpful and conversational"
        }

        val promptSnippet = "Reply matching user's writing style: $tone. Keep replies around $sentenceLengthDesc. Emoji usage: $emojiFreqDesc. Primary dialect/language: $dominantLang."

        return StyleAnalysis(
            detectedLanguage = dominantLang,
            avgSentenceLength = sentenceLengthDesc,
            emojiFrequency = emojiFreqDesc,
            punctuationStyle = punctuationDesc,
            tone = tone,
            commonExpressions = commonExpr,
            customPromptSnippet = promptSnippet
        )
    }

    private fun isStopWord(word: String): Boolean {
        return word in setOf("the", "and", "for", "with", "this", "that", "you", "are", "have", "will")
    }
}
