package com.example.data.model

data class ContactRule(
    val contactName: String = "",
    val autoReplyEnabled: Boolean = true,
    val personality: String = SettingsData.PROFILE_FRIENDLY,
    val replyLength: String = SettingsData.LENGTH_SHORT,
    val preferredLanguage: String = SettingsData.LANG_AUTO, // "Auto Detect", "English", "Hindi", "Hinglish", "Urdu", "Punjabi", "Bengali"
    val memoryEnabled: Boolean = true,
    val approvalMode: Boolean = true,
    val customNotes: String = "", // Personal notes (e.g. "College friend, talks in Hinglish")
    val customInstructions: String = "", // e.g. "Keep replies casual, don't mention weekend plans"
    val lastUpdated: Long = System.currentTimeMillis()
)
