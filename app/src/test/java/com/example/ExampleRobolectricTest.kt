package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.SensitiveMessageDetector
import com.example.data.security.SecureKeyStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ReplyMate", appName)
    }

    @Test
    fun `sensitive message detector flags emergencies and financial requests`() {
        val emergencyRes = SensitiveMessageDetector.evaluate("Please call an ambulance urgent accident")
        assertTrue(emergencyRes.isSensitive)

        val otpRes = SensitiveMessageDetector.evaluate("Send me your WhatsApp OTP code")
        assertTrue(otpRes.isSensitive)

        val bankRes = SensitiveMessageDetector.evaluate("Please transfer 5000 to my bank account via UPI")
        assertTrue(bankRes.isSensitive)

        val normalRes = SensitiveMessageDetector.evaluate("Hey are we meeting at 5 PM for coffee?")
        assertFalse(normalRes.isSensitive)
    }

    @Test
    fun `whatsapp package detector identifies standard packages`() {
        val messengerPkg = com.example.data.model.SettingsData.PACKAGE_WHATSAPP
        val businessPkg = com.example.data.model.SettingsData.PACKAGE_WHATSAPP_BUSINESS
        
        assertEquals("com.whatsapp", messengerPkg)
        assertEquals("com.whatsapp.w4b", businessPkg)
        
        assertTrue(com.example.service.WhatsAppPackageDetector.isPackageAllowed("com.whatsapp", "both"))
        assertTrue(com.example.service.WhatsAppPackageDetector.isPackageAllowed("com.whatsapp.w4b", "both"))
        assertFalse(com.example.service.WhatsAppPackageDetector.isPackageAllowed("com.facebook.katana", "both"))
        assertTrue(com.example.service.WhatsAppPackageDetector.isPackageAllowed("com.whatsapp", messengerPkg))
        assertFalse(com.example.service.WhatsAppPackageDetector.isPackageAllowed("com.whatsapp.w4b", messengerPkg))
    }

    @Test
    fun `notification parser filters out system and call messages`() {
        assertTrue(com.example.service.NotificationParser.isSystemOrCallMessage("Calling...", null))
        assertTrue(com.example.service.NotificationParser.isSystemOrCallMessage("Missed voice call", null))
        assertTrue(com.example.service.NotificationParser.isSystemOrCallMessage("Checking for new messages", null))
        assertTrue(com.example.service.NotificationParser.isSystemOrCallMessage("WhatsApp Web is currently active", null))
        assertFalse(com.example.service.NotificationParser.isSystemOrCallMessage("Hey, what are you doing?", "Alex"))
    }

    @Test
    fun `mask key utility properly hides sensitive characters`() {
        val emptyMasked = SecureKeyStorage.maskKey("")
        assertEquals("Not configured", emptyMasked)

        val apiKey = "AIzaSyD-1234567890abcdef"
        val masked = SecureKeyStorage.maskKey(apiKey)
        assertTrue(masked.startsWith("AIza"))
        assertTrue(masked.endsWith("cdef"))
        assertTrue(masked.contains("••••••••"))
    }
}
