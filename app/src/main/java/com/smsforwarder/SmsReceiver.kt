package com.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            if (messages.isEmpty()) return

            // Hent avsender fra første del
            val phoneNumber = messages[0].displayOriginatingAddress
            if (phoneNumber == null) {
                Logger.w(TAG, "SMS mottatt med null telefonnummer")
                return
            }

            // Kombiner alle meldingsdeler til én komplett melding
            val fullMessage = messages
                .mapNotNull { it.messageBody }
                .joinToString("")

            if (fullMessage.isEmpty()) {
                Logger.w(TAG, "SMS mottatt med tom melding")
                return
            }

            val sender = ContactHelper.formatSender(context, phoneNumber)

            // Logging uten sensitiv data (GDPR-compliant)
            Logger.d(TAG, "SMS mottatt og prosesseres (${messages.size} del(er))")

            // Send én email-varsel med komplett melding
            EmailSender.sendEmail(
                context,
                "📱 Ny SMS fra $sender",
                "Melding: $fullMessage"
            )

            // Send auto-svar SMS (kun én gang)
            AutoReplyHelper.sendSmsAutoReply(context, phoneNumber)
        }
    }
}