package com.smsforwarder

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.provider.CallLog
import android.database.Cursor
import android.app.Notification
import java.util.Collections

class NotificationMonitorService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationMonitor"
    }

    // Thread-safe LRU cache for processed call timestamps (max 50 entries)
    private val processedCallTimestamps = Collections.synchronizedMap(
        object : LinkedHashMap<Long, Boolean>(50, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Boolean>?): Boolean {
                return size > 50
            }
        }
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val category = notification.category

        // Mer robust deteksjon av tapt anrop
        if (category == Notification.CATEGORY_MISSED_CALL ||
            (packageName == "com.android.server.telecom" && isMissedCallNotification(notification))) {

            Logger.d(TAG, "Tapt anrop detektert via notifikasjon")
            handleMissedCall()
        }
    }

    private fun isMissedCallNotification(notification: Notification): Boolean {
        val extras = notification.extras
        val title = extras.getString("android.title", "")
        val text = extras.getCharSequence("android.text", "")?.toString() ?: ""

        // Sjekk for tapt anrop i flere språk og varianter
        // Norsk: "tapt", "ubesvart"
        // Engelsk: "missed"
        // Dette dekker de fleste Android-versjoner og språk
        return title.contains("tapt", ignoreCase = true) ||
               title.contains("missed", ignoreCase = true) ||
               title.contains("ubesvart", ignoreCase = true) ||
               text.contains("tapt", ignoreCase = true) ||
               text.contains("missed", ignoreCase = true)
    }

    private fun handleMissedCall() {
        // Sjekk at service ikke har blitt destroyed
        try {
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE
            )

            // Bruk konstanter for ORDER BY for å unngå injection-mønster
            val sortOrder = CallLog.Calls.DATE + " DESC"
            val cursor: Cursor? = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                "${CallLog.Calls.TYPE} = ?",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString()),
                sortOrder
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)

                    val number = if (numberIndex >= 0) it.getString(numberIndex) ?: "Ukjent" else "Ukjent"
                    val name = if (nameIndex >= 0) it.getString(nameIndex) ?: number else number
                    val timestamp = if (dateIndex >= 0) it.getLong(dateIndex) else 0L

                    // Unngå duplikat-varsler for samme anrop
                    if (timestamp > 0 && processedCallTimestamps.containsKey(timestamp)) {
                        Logger.d(TAG, "Anrop allerede prosessert, hopper over")
                        return
                    }

                    if (timestamp > 0) {
                        processedCallTimestamps[timestamp] = true
                    }

                    // Logging uten sensitiv data (GDPR-compliant)
                    Logger.d(TAG, "Tapt anrop prosesseres")

                    // Send email-varsel
                    EmailSender.sendEmail(this, "📞 Tapt anrop: $name", "Nummer: $number")

                    // Send auto-svar SMS
                    AutoReplyHelper.sendCallAutoReply(this, number)
                }
            }
        } catch (e: SecurityException) {
            Logger.e(TAG, "Tilgang nektet til CallLog", e)
        } catch (e: IllegalStateException) {
            Logger.e(TAG, "Service er i ugyldig state", e)
        } catch (e: Exception) {
            Logger.e(TAG, "Feil ved CallLog-sjekk", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Ikke nødvendig for tapte anrop
    }

    override fun onDestroy() {
        super.onDestroy()
        processedCallTimestamps.clear()
    }
}
