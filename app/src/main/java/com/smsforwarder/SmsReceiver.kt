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

            for (sms in messages) {
                val phoneNumber = sms.displayOriginatingAddress
                if (phoneNumber == null) {
                    Logger.w(TAG, "SMS mottatt med null telefonnummer")
                    continue
                }

                val message = sms.messageBody
                if (message == null) {
                    Logger.w(TAG, "SMS mottatt med null melding")
                    continue
                }

                val sender = ContactHelper.formatSender(context, phoneNumber)

                // Logging uten sensitiv data (GDPR-compliant)
                Logger.d(TAG, "SMS mottatt og prosesseres")

                // Send email-varsel
                EmailSender.sendEmail(
                    context,
                    "📱 Ny SMS fra $sender",
                    "Melding: $message"
                )

                // Send auto-svar SMS
                AutoReplyHelper.sendSmsAutoReply(context, phoneNumber)
            }
        }
    }
}
