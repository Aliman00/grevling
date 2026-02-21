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
 * SmsReceiver - Fanger innkommende SMS og videresender dem asynkront.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        // Fjernet scope fra companion object - opprettes nå per invokasjon
    }

    override fun onReceive(context: Context, intent: Intent) {
        Logger.d(TAG, "onReceive trigget med action: ${intent.action}")
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val appContext = context.applicationContext
        val prefs = EncryptedPrefsFactory.get(appContext)
        
        val isEnabled = prefs.getBoolean(PreferenceKeys.ENABLED, false)
        if (!isEnabled) {
            Logger.d(TAG, "App deaktivert, ignorerer SMS")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val message = messages[0]
        val phoneNumber = message.displayOriginatingAddress ?: return
        
        // Bruk goAsync for å sikre at vi rekker å kjøre logikken
        val pendingResult = goAsync()

        // Opprett ny scope PER SMS-hendelse - ikke i companion object
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                withTimeout(9_000L) {  // Holder seg innenfor goAsync()-grensen på 10s
                    val fullMessage = messages.mapNotNull { it.messageBody }.joinToString("")
                    if (fullMessage.isBlank()) return@withTimeout

                    val sender = ContactHelper.formatSender(appContext, phoneNumber)
                    val subject = appContext.getString(R.string.email_sms_subject, sender)
                    val body = appContext.getString(R.string.email_sms_body, fullMessage)
                    EmailSender.enqueueEmail(appContext, subject, body)
                    ForwardingStats.recordSmsForwarded(appContext)
                    AutoReplyHelper.sendSmsAutoReply(appContext, phoneNumber)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Feil ved prosessering av SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
