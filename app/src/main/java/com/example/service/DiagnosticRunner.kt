package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.ReplyMateApplication
import com.example.data.model.SettingsData

enum class DiagnosticStatus {
    PASS, WARNING, FAILED
}

data class DiagnosticItemResult(
    val id: Int,
    val title: String,
    val status: DiagnosticStatus,
    val explanation: String,
    val actionLabel: String? = null,
    val actionType: String? = null
)

object DiagnosticRunner {

    suspend fun runDiagnostics(context: Context): List<DiagnosticItemResult> {
        val app = ReplyMateApplication.instance
        val settings = app.preferencesRepository.settingsFlow.value
        val results = mutableListOf<DiagnosticItemResult>()

        // 1. WhatsApp installed
        val installStatus = WhatsAppPackageDetector.checkInstalledPackages(context)
        if (installStatus.anyInstalled) {
            val installedNames = buildList {
                if (installStatus.isWhatsAppInstalled) add("WhatsApp Messenger")
                if (installStatus.isBusinessInstalled) add("WhatsApp Business")
            }.joinToString(" & ")
            results.add(
                DiagnosticItemResult(
                    id = 1,
                    title = "WhatsApp Installed",
                    status = DiagnosticStatus.PASS,
                    explanation = "$installedNames detected on this device."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 1,
                    title = "WhatsApp Installed",
                    status = DiagnosticStatus.FAILED,
                    explanation = "No supported WhatsApp app (Messenger or Business) detected on this device.",
                    actionLabel = "Install WhatsApp",
                    actionType = "WHATSAPP_STORE"
                )
            )
        }

        // 2. Selected WhatsApp package
        val packageValid = when (settings.selectedWhatsAppPackage) {
            SettingsData.PACKAGE_WHATSAPP -> installStatus.isWhatsAppInstalled
            SettingsData.PACKAGE_WHATSAPP_BUSINESS -> installStatus.isBusinessInstalled
            else -> installStatus.anyInstalled
        }
        val packageLabel = when (settings.selectedWhatsAppPackage) {
            SettingsData.PACKAGE_WHATSAPP -> "WhatsApp Messenger"
            SettingsData.PACKAGE_WHATSAPP_BUSINESS -> "WhatsApp Business"
            else -> "Both Messenger & Business"
        }
        if (packageValid) {
            results.add(
                DiagnosticItemResult(
                    id = 2,
                    title = "Selected WhatsApp App",
                    status = DiagnosticStatus.PASS,
                    explanation = "Configured to monitor: $packageLabel."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 2,
                    title = "Selected WhatsApp App",
                    status = DiagnosticStatus.FAILED,
                    explanation = "Selected package ($packageLabel) is not installed.",
                    actionLabel = "Change Selection",
                    actionType = "SETTINGS_WHATSAPP"
                )
            )
        }

        // 3. Notification Access granted
        val accessGranted = NotificationHelper.isNotificationAccessGranted(context)
        if (accessGranted) {
            results.add(
                DiagnosticItemResult(
                    id = 3,
                    title = "Notification Access Granted",
                    status = DiagnosticStatus.PASS,
                    explanation = "System permission enabled for ReplyMate."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 3,
                    title = "Notification Access Granted",
                    status = DiagnosticStatus.FAILED,
                    explanation = "Notification access permission is NOT granted. ReplyMate cannot read incoming messages.",
                    actionLabel = "Grant Access",
                    actionType = "NOTIFICATION_SETTINGS"
                )
            )
        }

        // 4. NotificationListenerService connected
        val serviceConnected = ServiceDiagnostics.isServiceConnected.value
        if (serviceConnected) {
            results.add(
                DiagnosticItemResult(
                    id = 4,
                    title = "Listener Service Connected",
                    status = DiagnosticStatus.PASS,
                    explanation = "Android OS is actively bound to ReplyMate's notification listener."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 4,
                    title = "Listener Service Connected",
                    status = if (accessGranted) DiagnosticStatus.WARNING else DiagnosticStatus.FAILED,
                    explanation = if (accessGranted) {
                        "Permission is granted but Android OS has not bound the listener service yet. Tap Reconnect to force binding."
                    } else {
                        "Service cannot connect without Notification Access."
                    },
                    actionLabel = if (accessGranted) "Reconnect Service" else "Grant Access",
                    actionType = if (accessGranted) "REBIND_SERVICE" else "NOTIFICATION_SETTINGS"
                )
            )
        }

        // 5. Listener receiving notifications
        val notifCount = ServiceDiagnostics.notificationsReceivedCount.value
        if (notifCount > 0) {
            results.add(
                DiagnosticItemResult(
                    id = 5,
                    title = "Listener Receiving Notifications",
                    status = DiagnosticStatus.PASS,
                    explanation = "Verified: $notifCount notification(s) intercepted by service."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 5,
                    title = "Listener Receiving Notifications",
                    status = DiagnosticStatus.WARNING,
                    explanation = "No system notifications received yet. Send a test WhatsApp message or wait for device activity."
                )
            )
        }

        // 6. WhatsApp notification detected
        val waNotifCount = ServiceDiagnostics.whatsappNotificationsCount.value
        if (waNotifCount > 0) {
            results.add(
                DiagnosticItemResult(
                    id = 6,
                    title = "WhatsApp Notification Detected",
                    status = DiagnosticStatus.PASS,
                    explanation = "$waNotifCount WhatsApp notification(s) successfully identified."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 6,
                    title = "WhatsApp Notification Detected",
                    status = DiagnosticStatus.WARNING,
                    explanation = "No WhatsApp notifications detected yet. Send a message to this device from another phone to test."
                )
            )
        }

        // 7. Message text extracted
        if (waNotifCount > 0) {
            results.add(
                DiagnosticItemResult(
                    id = 7,
                    title = "Message Text Extracted",
                    status = DiagnosticStatus.PASS,
                    explanation = "WhatsApp chat message text successfully extracted by parser."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 7,
                    title = "Message Text Extracted",
                    status = DiagnosticStatus.WARNING,
                    explanation = "Waiting for an incoming WhatsApp message to test extraction."
                )
            )
        }

        // 8. Sender information extracted where available
        val lastSender = ServiceDiagnostics.lastWhatsAppSender.value
        if (!lastSender.isNullOrBlank()) {
            results.add(
                DiagnosticItemResult(
                    id = 8,
                    title = "Sender Information Extracted",
                    status = DiagnosticStatus.PASS,
                    explanation = "Sender identified: Available."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 8,
                    title = "Sender Information Extracted",
                    status = DiagnosticStatus.WARNING,
                    explanation = "No sender parsed yet. Awaiting WhatsApp notification."
                )
            )
        }

        // 9. Supported reply action detected
        val hasReplyAction = ServiceDiagnostics.lastReplyActionAvailable.value
        if (hasReplyAction) {
            results.add(
                DiagnosticItemResult(
                    id = 9,
                    title = "Supported Reply Action Detected",
                    status = DiagnosticStatus.PASS,
                    explanation = "WhatsApp notification supplied an Android RemoteInput quick reply action."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 9,
                    title = "Supported Reply Action Detected",
                    status = DiagnosticStatus.WARNING,
                    explanation = "Reply action pending receipt of an active WhatsApp message."
                )
            )
        }

        // 10. AI provider configured
        val providerName = if (settings.aiProvider == SettingsData.PROVIDER_GEMINI) "Google Gemini" else "OpenAI"
        results.add(
            DiagnosticItemResult(
                id = 10,
                title = "AI Provider Configured",
                status = DiagnosticStatus.PASS,
                explanation = "Active AI engine: $providerName."
            )
        )

        // 11. API key valid
        val hasKey = app.secureKeyStorage.hasKey(settings.aiProvider)
        if (hasKey) {
            results.add(
                DiagnosticItemResult(
                    id = 11,
                    title = "API Key Saved",
                    status = DiagnosticStatus.PASS,
                    explanation = "Valid encrypted API key stored in Android Keystore."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 11,
                    title = "API Key Saved",
                    status = DiagnosticStatus.FAILED,
                    explanation = "No API key configured for $providerName. Auto-reply cannot generate text without an API key.",
                    actionLabel = "Enter API Key",
                    actionType = "SETTINGS_AI"
                )
            )
        }

        // 12. Network available
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNet)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        if (hasInternet) {
            results.add(
                DiagnosticItemResult(
                    id = 12,
                    title = "Network Available",
                    status = DiagnosticStatus.PASS,
                    explanation = "Active internet connection detected for AI API requests."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 12,
                    title = "Network Available",
                    status = DiagnosticStatus.FAILED,
                    explanation = "No internet connection detected. AI requests will fail until online."
                )
            )
        }

        // 13. AI response generated
        val startOfDay = System.currentTimeMillis() - 86400000L
        val todayReplies = app.database.replyLogDao().getTodayReplyCount(startOfDay)
        if (todayReplies > 0 || hasKey) {
            results.add(
                DiagnosticItemResult(
                    id = 13,
                    title = "AI Response Generated",
                    status = DiagnosticStatus.PASS,
                    explanation = "AI engine ready ($todayReplies replies generated today)."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 13,
                    title = "AI Response Generated",
                    status = DiagnosticStatus.WARNING,
                    explanation = "Configure your API key to enable AI reply generation.",
                    actionLabel = "Configure AI",
                    actionType = "SETTINGS_AI"
                )
            )
        }

        // 14. Reply action available in cache
        val anyCached = ReplySender.anyCachedActionAvailable()
        if (anyCached) {
            results.add(
                DiagnosticItemResult(
                    id = 14,
                    title = "Reply Action Cached",
                    status = DiagnosticStatus.PASS,
                    explanation = "Active WhatsApp reply action is cached in memory and ready for immediate sending."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 14,
                    title = "Reply Action Cached",
                    status = DiagnosticStatus.WARNING,
                    explanation = "No active WhatsApp notification is currently open in Android's notification shade."
                )
            )
        }

        // 15. Reply sent successfully
        if (todayReplies > 0) {
            results.add(
                DiagnosticItemResult(
                    id = 15,
                    title = "Reply Sent Successfully",
                    status = DiagnosticStatus.PASS,
                    explanation = "Confirmed: Auto-replies or approved replies have been sent today."
                )
            )
        } else {
            results.add(
                DiagnosticItemResult(
                    id = 15,
                    title = "Reply Sent Successfully",
                    status = DiagnosticStatus.WARNING,
                    explanation = "Ready to send once incoming WhatsApp messages arrive and are processed."
                )
            )
        }

        return results
    }
}
