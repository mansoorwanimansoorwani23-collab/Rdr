package com.example.ai

data class SensitivityResult(
    val isSensitive: Boolean,
    val reason: String? = null
)

object SensitiveMessageDetector {

    private val EMERGENCY_KEYWORDS = listOf(
        "help", "accident", "hospital", "police", "ambulance", "emergency",
        "urgent", "danger", "injured", "bleeding", "fire", "save me", "icu",
        "doctor emergency", "madad karo", "bachao", "turant"
    )

    private val FINANCIAL_KEYWORDS = listOf(
        "otp", "upi", "bank", "atm", "pin", "cvv", "credit card", "debit card",
        "transfer money", "send money", "gpay", "phonepe", "paytm", "account number",
        "net banking", "paisa bhejo", "paise daalo", "wire transfer", "crypto",
        "bitcoin", "verify account"
    )

    private val SECURITY_KEYWORDS = listOf(
        "password", "passcode", "secret code", "auth code", "2fa",
        "verification code", "login credential", "recovery code", "access token"
    )

    private val LEGAL_MEDICAL_KEYWORDS = listOf(
        "lawyer", "court notice", "police complaint", "fir", "arrest",
        "legal notice", "custody", "suicide", "prescription drug", "overdose"
    )

    fun evaluate(message: String): SensitivityResult {
        val lower = message.lowercase()

        // 1. Emergency detection
        for (kw in EMERGENCY_KEYWORDS) {
            if (containsWord(lower, kw)) {
                return SensitivityResult(
                    isSensitive = true,
                    reason = "Emergency or safety topic detected ($kw)"
                )
            }
        }

        // 2. Financial / OTP detection
        for (kw in FINANCIAL_KEYWORDS) {
            if (containsWord(lower, kw)) {
                return SensitivityResult(
                    isSensitive = true,
                    reason = "Financial transaction, OTP or banking request ($kw)"
                )
            }
        }

        // 3. Security / Passwords detection
        for (kw in SECURITY_KEYWORDS) {
            if (containsWord(lower, kw)) {
                return SensitivityResult(
                    isSensitive = true,
                    reason = "Security credentials or verification code ($kw)"
                )
            }
        }

        // 4. Legal / Serious medical detection
        for (kw in LEGAL_MEDICAL_KEYWORDS) {
            if (containsWord(lower, kw)) {
                return SensitivityResult(
                    isSensitive = true,
                    reason = "Sensitive legal or serious medical matter ($kw)"
                )
            }
        }

        return SensitivityResult(isSensitive = false)
    }

    private fun containsWord(text: String, word: String): Boolean {
        // Regex word boundary check to avoid false matches (e.g. "hotpot" matching "otp")
        val pattern = "\\b${Regex.escape(word)}\\b".toRegex(RegexOption.IGNORE_CASE)
        return pattern.containsMatchIn(text) || text.contains(word)
    }
}
