package com.example.ai

object ResponseQualityFilter {

    private val HALLUCINATION_PHRASES = listOf(
        "as an ai", "as a language model", "i am an artificial intelligence",
        "i don't have access to your whatsapp", "i don't have personal feelings",
        "here is your reply", "suggested reply:", "here's what you could say:"
    )

    private val SENSITIVE_OUTPUT_TRIGGERS = listOf(
        "password is", "otp is", "my pin is", "cvv is", "my credit card is"
    )

    fun validateReply(reply: String, incomingMessage: String): ValidationResult {
        val trimmed = reply.trim()
        val lower = trimmed.lowercase()

        // 1. Check if empty
        if (trimmed.isBlank()) {
            return ValidationResult(isValid = false, reason = "Empty reply generated")
        }

        // 2. Check for AI robotic phrases
        for (phrase in HALLUCINATION_PHRASES) {
            if (lower.contains(phrase)) {
                return ValidationResult(
                    isValid = false,
                    reason = "Response contained AI robotic artifact ($phrase)"
                )
            }
        }

        // 3. Prevent accidental leaks
        for (trigger in SENSITIVE_OUTPUT_TRIGGERS) {
            if (lower.contains(trigger)) {
                return ValidationResult(
                    isValid = false,
                    reason = "Response attempted to output sensitive financial/password credentials"
                )
            }
        }

        // 4. Exact echo check (prevent repeating verbatim what incoming message said)
        if (trimmed.equals(incomingMessage.trim(), ignoreCase = true) && trimmed.length > 8) {
            return ValidationResult(
                isValid = false,
                reason = "Response merely repeated the incoming question"
            )
        }

        return ValidationResult(isValid = true)
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val reason: String? = null
)
