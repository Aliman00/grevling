package com.smsforwarder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

object AutoReplyHelper {

    private const val TAG = "AutoReplyHelper"
    // Standard SMS length before MMS is triggered
    private const val MAX_SMS_LENGTH = 160

    private enum class MessageType(val prefKey: String, val logName: String) {
        SMS("sms_reply_message", "SMS"),
        CALL("call_reply_message", "anrop")
    }

    fun sendSmsAutoReply(context: Context, phoneNumber: String) {
        sendAutoReply(context, phoneNumber, MessageType.SMS)
    }

    fun sendCallAutoReply(context: Context, phoneNumber: String) {
        sendAutoReply(context, phoneNumber, MessageType.CALL)
    }

    private fun sendAutoReply(context: Context, phoneNumber: String, messageType: MessageType) {
        // Sjekk tillatelser først
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Logger.w(TAG, "SEND_SMS tillatelse mangler")
            return
        }

        val prefs = PreferencesManager.getEncryptedPreferences(context)
        val autoReplyEnabled = prefs.getBoolean("auto_reply_enabled", false)

        if (!autoReplyEnabled) {
            Logger.d(TAG, "Auto-svar er deaktivert")
            return
        }

        if (phoneNumber.isBlank()) {
            Logger.w(TAG, "Ugyldig telefonnummer, hopper over auto-svar")
            return
        }

        val message = getAutoReplyMessage(prefs, messageType)

        if (message.isEmpty()) {
            Logger.d(TAG, "Ingen auto-svar melding satt for ${messageType.logName}")
            return
        }

        if (message.length > MAX_SMS_LENGTH) {
            Logger.w(TAG, "Auto-svar melding er ${message.length} tegn, sendes som multi-part SMS")
        }

        sendSms(context, phoneNumber, message)
    }

    private fun getAutoReplyMessage(prefs: android.content.SharedPreferences, messageType: MessageType): String {
        val useSameMessage = prefs.getBoolean("use_same_message", true)

        return if (useSameMessage) {
            prefs.getString("unified_reply_message", "") ?: ""
        } else {
            prefs.getString(messageType.prefKey, "") ?: ""
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            // Bruk moderne API (API 31+) hvis tilgjengelig, fall tilbake til deprecated API
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Håndter lange meldinger automatisk (multi-part SMS)
            if (message.length > MAX_SMS_LENGTH) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                Logger.d(TAG, "Auto-svar SMS sendt (${parts.size} deler)")
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Logger.d(TAG, "Auto-svar SMS sendt")
            }
        } catch (e: SecurityException) {
            Logger.e(TAG, "Tilgang nektet for sending av SMS", e)
        } catch (e: IllegalArgumentException) {
            Logger.e(TAG, "Ugyldig telefonnummer eller melding", e)
        } catch (e: Exception) {
            Logger.e(TAG, "Feil ved sending av auto-svar", e)
        }
    }
}
