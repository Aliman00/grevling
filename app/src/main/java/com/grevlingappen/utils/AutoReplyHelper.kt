package com.grevlingappen.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.grevlingappen.R
import com.grevlingappen.data.PreferenceKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * AutoReplyHelper - Sender automatiske SMS-svar ved innkommende SMS eller tapte anrop.
 * Inneholder rate-limiting for å unngå spamming av mottakere.
 */
object AutoReplyHelper {
    private const val TAG = "AutoReplyHelper"
    private const val MAX_SMS_LENGTH = 160
    private const val AUTO_REPLY_COOLDOWN_MS = 300_000L // 5 minutter

    private val recentReplies = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(50, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean = size > 50
        }
    )

    private enum class MessageType(val prefKey: String, val defaultResId: Int) {
        SMS(PreferenceKeys.SMS_REPLY_MESSAGE, R.string.default_sms_message),
        CALL(PreferenceKeys.CALL_REPLY_MESSAGE, R.string.default_call_message)
    }

    /** Send auto-svar for mottatt SMS. */
    suspend fun sendSmsAutoReply(context: Context, phoneNumber: String) {
        sendAutoReply(context, phoneNumber, MessageType.SMS)
    }

    /** Send auto-svar for tapt anrop. */
    suspend fun sendCallAutoReply(context: Context, phoneNumber: String) {
        sendAutoReply(context, phoneNumber, MessageType.CALL)
    }

    private suspend fun sendAutoReply(context: Context, phoneNumber: String, messageType: MessageType) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        Logger.d(TAG, "Forsøker auto-svar til $phoneNumber for type $messageType")
        
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Logger.w(TAG, "Mangler SEND_SMS tillatelse - kan ikke sende auto-svar")
            return@withContext
        }

        val prefs = EncryptedPrefsFactory.get(appContext)
        val isAutoReplyEnabled = prefs.getBoolean(PreferenceKeys.AUTO_REPLY_ENABLED, false)
        Logger.d(TAG, "Auto-svar aktivert i innstillinger: $isAutoReplyEnabled")
        if (!isAutoReplyEnabled) return@withContext

        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            Logger.w(TAG, "Ugyldig telefonnummer format: $phoneNumber")
            return@withContext
        }

        val normalized = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
        val now = System.currentTimeMillis()
        
        val messageToSend = synchronized(recentReplies) {
            val lastReply = recentReplies[normalized]
            if (lastReply != null && (now - lastReply) < AUTO_REPLY_COOLDOWN_MS) {
                Logger.d(TAG, "Auto-svar nylig sendt til $normalized, hopper over")
                return@synchronized null
            }
            
            val message = getAutoReplyMessage(appContext, prefs, messageType)
            if (message.isBlank()) {
                Logger.w(TAG, "Ingen gyldig melding funnet (tom eller null)")
                return@synchronized null
            }

            recentReplies[normalized] = now
            message
        }

        if (messageToSend != null) {
            sendSms(appContext, phoneNumber, messageToSend)
        }
    }

    private fun getAutoReplyMessage(context: Context, prefs: android.content.SharedPreferences, messageType: MessageType): String {
        val useSameMessage = prefs.getBoolean(PreferenceKeys.USE_SAME_MESSAGE, true)
        
        val storedMessage = if (useSameMessage) {
            prefs.getString(PreferenceKeys.UNIFIED_REPLY_MESSAGE, null)
        } else {
            prefs.getString(messageType.prefKey, null)
        }
        
        return storedMessage?.ifBlank { null } ?: context.getString(messageType.defaultResId)
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            } ?: throw Exception("SmsManager ikke tilgjengelig")

            if (message.length > MAX_SMS_LENGTH) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
            Logger.d(TAG, "Auto-svar sendt")
        } catch (e: Exception) {
            Logger.e(TAG, "Feil ved sending av SMS", e)
        }
    }
}
