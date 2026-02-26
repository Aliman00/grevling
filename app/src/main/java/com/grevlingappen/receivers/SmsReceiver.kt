package com.grevlingappen.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.grevlingappen.R
import com.grevlingappen.data.PreferenceKeys
import com.grevlingappen.utils.AutoReplyHelper
import com.grevlingappen.utils.ContactHelper
import com.grevlingappen.utils.EmailSender
import com.grevlingappen.utils.EncryptedPrefsFactory
import com.grevlingappen.utils.ForwardingStats
import com.grevlingappen.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * SmsReceiver - BroadcastReceiver som fanger opp innkommende SMS-meldinger.
 * 
 * Funksjonalitet:
 * - Lytter på SMS_RECEIVED broadcasts fra systemet
 * - Sjekker om videresending er aktivert i appen
 * - Videresender SMS-innhold til konfigurert e-postadresse
 * - Sender automatisk svar til avsender hvis auto-svar er aktivert
 * 
 * Merk: Bruker goAsync() og timeout for å sikre at asynkron prosessering
 * fullføres før systemet gir oss en deadline på 10 sekunder.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    /**
     * Håndterer mottatt SMS-broadcast fra systemet.
     * Kjøres på main-tråden og må være rask - bruker goAsync() for async.
     */
    override fun onReceive(context: Context, intent: Intent) {
        // Kun håndtere SMS-meldinger, ignorere andre intents
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        Logger.d(TAG, "Mottatt SMS-broadcast")

        // Hent application context for å unngå memory leaks
        val appContext = context.applicationContext
        val prefs = EncryptedPrefsFactory.get(appContext)
        
        // Sjekk om videresending er aktivert av brukeren
        val isEnabled = prefs.getBoolean(PreferenceKeys.ENABLED, false)
        if (!isEnabled) {
            Logger.d(TAG, "Videresending er deaktivert - ignorerer SMS")
            return
        }

        // Hent alle SMS-deler fra intent (kan være flere deler ved lange meldinger)
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Finn gyldig avsender - bruk første melding med gyldig adresse
        val message = messages.firstOrNull { !it.displayOriginatingAddress.isNullOrBlank() } ?: messages[0]
        val phoneNumber = message.displayOriginatingAddress ?: return
        
        // goAsync() holder broadcast alive mens vi prosesserer asynkront
        // Systemet gir oss ~10 sekunder før timeout
        val pendingResult = goAsync()

        // Opprett egen scope for denne SMS-hendelsen for isolasjon
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                // Timeout på 9 sekunder for å ha margin til goAsync-deadline
                withTimeout(9_000L) {
                    // Samle alle meldingsdeler til én tekst
                    val fullMessage = messages.mapNotNull { it.messageBody }.joinToString("")
                    if (fullMessage.isBlank()) return@withTimeout

                    // Formater avsendernavn (kontaktnavn hvis tilgjengelig, ellers telefonnummer)
                    val sender = ContactHelper.formatSender(appContext, phoneNumber)
                    
                    // Bygg e-postemne og body
                    val subject = appContext.getString(R.string.email_sms_subject, sender)
                    val body = appContext.getString(R.string.email_sms_body, fullMessage)
                    
                    // Legg e-post i kø for bakgrunnsutsending
                    EmailSender.enqueueEmail(appContext, subject, body)
                    
                    // Registrer statistikk for videresending
                    ForwardingStats.recordSmsForwarded(appContext)
                    
                    // Send auto-svar hvis aktivert
                    AutoReplyHelper.sendSmsAutoReply(appContext, phoneNumber)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Feil ved prosessering av SMS", e)
            } finally {
                // Må kalle finish() for å signalisere til systemet at vi er ferdige
                pendingResult.finish()
            }
        }
    }
}