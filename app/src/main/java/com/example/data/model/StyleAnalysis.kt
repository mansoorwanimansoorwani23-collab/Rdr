package com.example.data.model

data class StyleAnalysis(
    val detectedLanguage: String = "Hinglish",
    val avgSentenceLength: String = "Short (1-2 sentences)",
    val emojiFrequency: String = "Moderate (1 emoji per text)",
    val punctuationStyle: String = "Casual (minimal punctuation, exclamation points)",
    val tone: String = "Warm, casual, friendly",
    val commonExpressions: List<String> = listOf("Haan", "Achaa", "Sounds good", "Will check", "Sure thing"),
    val customPromptSnippet: String = "Replies should be friendly, concise, natural texting style with occasional warm emoji."
)
