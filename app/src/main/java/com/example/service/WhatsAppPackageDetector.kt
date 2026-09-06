package com.example.service

import android.content.Context
import android.content.pm.PackageManager
import com.example.data.model.SettingsData

data class WhatsAppInstallStatus(
    val isWhatsAppInstalled: Boolean,
    val isBusinessInstalled: Boolean
) {
    val anyInstalled: Boolean get() = isWhatsAppInstalled || isBusinessInstalled
    val bothInstalled: Boolean get() = isWhatsAppInstalled && isBusinessInstalled
    val onlyMessenger: Boolean get() = isWhatsAppInstalled && !isBusinessInstalled
    val onlyBusiness: Boolean get() = !isWhatsAppInstalled && isBusinessInstalled
}

object WhatsAppPackageDetector {

    fun checkInstalledPackages(context: Context): WhatsAppInstallStatus {
        val pm = context.packageManager
        val hasMessenger = isPackageInstalled(pm, SettingsData.PACKAGE_WHATSAPP)
        val hasBusiness = isPackageInstalled(pm, SettingsData.PACKAGE_WHATSAPP_BUSINESS)
        return WhatsAppInstallStatus(
            isWhatsAppInstalled = hasMessenger,
            isBusinessInstalled = hasBusiness
        )
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Determines whether a notification package is accepted based on the user's selectedWhatsAppPackage setting.
     */
    fun isPackageAllowed(packageName: String, selectedSetting: String): Boolean {
        return when (selectedSetting) {
            SettingsData.PACKAGE_WHATSAPP -> packageName == SettingsData.PACKAGE_WHATSAPP
            SettingsData.PACKAGE_WHATSAPP_BUSINESS -> packageName == SettingsData.PACKAGE_WHATSAPP_BUSINESS
            else -> packageName == SettingsData.PACKAGE_WHATSAPP || packageName == SettingsData.PACKAGE_WHATSAPP_BUSINESS
        }
    }
}
