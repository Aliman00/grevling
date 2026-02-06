package com.smsforwarder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import java.util.Collections

object AutoReplyHelper {

    private const val TAG = "AutoReplyHelper"
    private const val MAX_SMS_LENGTH = 160
    private const val AUTO_REPLY_COOLDOWN_MS = 300_000L // 5 minutter

    // Cache: telefonnummer → tidspunkt for siste auto-svar
    private val recentReplies = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(50, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?) = size > 50
        }
    )

    private enum class MessageType(val prefKey: String, val logName: String) {
        SMS(PreferencesManager.KEY_SMS_REPLY_MESSAGE, "SMS"),
        CALL(PreferencesManager.KEY_CALL_REPLY_MESSAGE, "anrop")
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
        val autoReplyEnabled = prefs.getBoolean(PreferencesManager.KEY_AUTO_REPLY_ENABLED, false)

        if (!autoReplyEnabled) {
            Logger.d(TAG, "Auto-svar er deaktivert")
            return
        }

        // Valider telefonnummer - må inneholde minst noen sifre
        if (phoneNumber.isBlank() || !phoneNumber.any { it.isDigit() }) {
            Logger.w(TAG, "Ugyldig telefonnummer format, hopper over auto-svar")
            return
        }

        // Rate-limit: Maks én auto-svar per 5 minutter per nummer
        val now = System.currentTimeMillis()
        val lastReply = recentReplies[phoneNumber]
        if (lastReply != null && (now - lastReply) < AUTO_REPLY_COOLDOWN_MS) {
            Logger.d(TAG, "Auto-svar allerede sendt nylig, hopper over")
            return
        }

        val message = getAutoReplyMessage(prefs, messageType)

        if (message.isEmpty()) {
            Logger.d(TAG, "Ingen auto-svar melding satt for ${messageType.logName}")
            return
        }

        // Oppdater rate-limit kun etter at meldingen er validert
        recentReplies[phoneNumber] = now

        if (message.length > MAX_SMS_LENGTH) {
            Logger.w(TAG, "Auto-svar melding er ${message.length} tegn, sendes som multi-part SMS")
        }

        sendSms(context, phoneNumber, message)
    }

    private fun getAutoReplyMessage(prefs: android.content.SharedPreferences, messageType: MessageType): String {
        val useSameMessage = prefs.getBoolean(PreferencesManager.KEY_USE_SAME_MESSAGE, true)

        return if (useSameMessage) {
            prefs.getString(PreferencesManager.KEY_UNIFIED_REPLY_MESSAGE, "") ?: ""
        } else {
            prefs.getString(messageType.prefKey, "") ?: ""
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            // Bruk moderne API (API 31+) hvis tilgjengelig, fall tilbake til deprecated API
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java) ?: run {
                    Logger.e(TAG, "SmsManager ikke tilgjengelig (ingen SIM?)")
                    return
                }
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
