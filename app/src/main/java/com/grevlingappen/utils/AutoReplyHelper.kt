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
 * AutoReplyHelper - Objekt som håndterer automatiske SMS-svar.
 * 
 * Funksjonalitet:
 * - Sender auto-svar ved mottatt SMS
 * - Sender auto-svar ved tapte anrop
 * - Rate-limiting: unngår å sende flere svar til samme nummer innen 5 minutter
 * - Filtrerer bort korte nummer (f.eks. 1881, numre fra bedrifter)
 * - Støtter både enhetlig melding og separate meldinger for SMS/anrop
 * 
 * Viktig: Krever SEND_SMS tillatelse for å fungere.
 */
object AutoReplyHelper {
    private const val TAG = "AutoReplyHelper"
    
    // Maksimal lengde for én SMS (lang melding blir delt i flere deler)
    private const val MAX_SMS_LENGTH = 160
    // Cooldown mellom auto-svar til samme nummer (5 minutter)
    private const val AUTO_REPLY_COOLDOWN_MS = 300_000L

    // LRU-cache som holder styr på når auto-svar sist ble sendt til ulike nummer
    // Begrenset til 50 oppføringer for å unngå memory leaks
    private val recentReplies = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(50, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean = size > 50
        }
    )

    /**
     * Enum som definerer hvilken type melding som skal brukes.
     * Har egne preferanse-nøkler og standard-meldinger for hver type.
     */
    private enum class MessageType(val prefKey: String, val defaultResId: Int) {
        SMS(PreferenceKeys.SMS_REPLY_MESSAGE, R.string.default_sms_message),
        CALL(PreferenceKeys.CALL_REPLY_MESSAGE, R.string.default_call_message)
    }

    /**
     * Sender auto-svar til avsender av mottatt SMS.
     * 
     * @param context App-kontekst
     * @param phoneNumber Nummeret som sendte SMS
     */
    suspend fun sendSmsAutoReply(context: Context, phoneNumber: String) {
        sendAutoReply(context, phoneNumber, MessageType.SMS)
    }

    /**
     * Sender auto-svar til nummer som ringte men ikke ble besvart.
     * 
     * @param context App-kontekst
     * @param phoneNumber Nummeret som ringte
     */
    suspend fun sendCallAutoReply(context: Context, phoneNumber: String) {
        sendAutoReply(context, phoneNumber, MessageType.CALL)
    }

    /**
     * Interne: Hovedlogikk for å sende auto-svar.
     * Sjekker tillatelser, validerer nummer, håndterer rate-limiting, og sender SMS.
     */
    private suspend fun sendAutoReply(context: Context, phoneNumber: String, messageType: MessageType) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        Logger.d(TAG, "Forsøker auto-svar til $phoneNumber for type $messageType")
        
        // Sjekk at vi har SEND_SMS tillatelse
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Logger.w(TAG, "Mangler SEND_SMS tillatelse - kan ikke sende auto-svar")
            return@withContext
        }

        // Sjekk om auto-svar er aktivert i innstillinger
        val prefs = EncryptedPrefsFactory.get(appContext)
        val isAutoReplyEnabled = prefs.getBoolean(PreferenceKeys.AUTO_REPLY_ENABLED, false)
        Logger.d(TAG, "Auto-svar aktivert i innstillinger: $isAutoReplyEnabled")
        if (!isAutoReplyEnabled) return@withContext

        // Validér telefonnummerformat
        if (!PhoneNumberUtils.isValidPhoneNumber(phoneNumber)) {
            Logger.w(TAG, "Ugyldig telefonnummer format: $phoneNumber")
            return@withContext
        }

        // Ignorer korte nummer (f.eks. 1881, bedriftsnummer)
        // Norske mobilnummer er 8 siffer - alt under er sannsynligvis ikke et personlig nummer
        val digitsOnly = phoneNumber.filter { it.isDigit() }
        if (digitsOnly.length < 8) {
            Logger.w(TAG, "Nummeret er for kort (${digitsOnly.length} siffer), ignorerer auto-svar: $phoneNumber")
            return@withContext
        }

        // Normaliser nummeret (fjern +47 og erstatt med 47)
        val normalized = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
        val now = System.currentTimeMillis()
        
        // Sjekk rate-limiting: har vi sendt til dette nummeret nylig?
        val messageToSend = synchronized(recentReplies) {
            val lastReply = recentReplies[normalized]
            // Hvis vi har sendt de siste 5 minuttene, hopp over
            if (lastReply != null && (now - lastReply) < AUTO_REPLY_COOLDOWN_MS) {
                Logger.d(TAG, "Auto-svar nylig sendt til $normalized, hopper over")
                return@synchronized null
            }
            
            // Hent meldingen som skal sendes
            val message = getAutoReplyMessage(appContext, prefs, messageType)
            if (message.isBlank()) {
                Logger.w(TAG, "Ingen gyldig melding funnet (tom eller null)")
                return@synchronized null
            }

            // Registrer at vi skal sende nå
            recentReplies[normalized] = now
            message
        }

        // Send SMS hvis vi har en melding
        if (messageToSend != null) {
            sendSms(appContext, phoneNumber, messageToSend)
        }
    }

    /**
     * Henter auto-svar melding basert på type og innstillinger.
     * Returnerer lagret melding eller standard-melding hvis ingen er satt.
     */
    private fun getAutoReplyMessage(context: Context, prefs: android.content.SharedPreferences, messageType: MessageType): String {
        val useSameMessage = prefs.getBoolean(PreferenceKeys.USE_SAME_MESSAGE, true)
        
        // Bestem hvilken melding som skal brukes
        val storedMessage = if (useSameMessage) {
            prefs.getString(PreferenceKeys.UNIFIED_REPLY_MESSAGE, null)
        } else {
            prefs.getString(messageType.prefKey, null)
        }
        
        // Returner lagret melding eller fallback til standard
        return storedMessage?.ifBlank { null } ?: context.getString(messageType.defaultResId)
    }

    /**
     * Sender faktisk SMS via SmsManager.
     * Håndterer både korte og lange meldinger (deler opp ved behov).
     */
    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            // Skaff SmsManager - API 31+ krever getSystemService
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            } ?: throw Exception("SmsManager ikke tilgjengelig")

            // Del opp i multiple SMS-deler hvis meldingen er for lang
            if (message.length > MAX_SMS_LENGTH) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
            Logger.d(TAG, "Auto-svar sendt til $phoneNumber")
        } catch (e: Exception) {
            Logger.e(TAG, "Feil ved sending av SMS", e)
        }
    }
}