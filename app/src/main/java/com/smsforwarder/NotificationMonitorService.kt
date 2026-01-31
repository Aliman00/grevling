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

    // Cache for processed app notifications (unngå duplikater)
    private val processedAppNotifications = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > 100
            }
        }
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val category = notification.category

        // Ignorer egne varsler
        if (packageName == "com.smsforwarder") return

        // Eksisterende: Tapt anrop
        if (category == Notification.CATEGORY_MISSED_CALL ||
            (packageName == "com.android.server.telecom" && isMissedCallNotification(notification))) {

            Logger.d(TAG, "Tapt anrop detektert via notifikasjon")
            handleMissedCall()
            return
        }

        // NY: Håndter varsler fra valgte apper
        handleAppNotification(sbn)
    }

    private fun handleAppNotification(sbn: StatusBarNotification) {
        val prefs = PreferencesManager.getEncryptedPreferences(this)
        val enabled = prefs.getBoolean("enabled", false)
        
        if (!enabled) return

        val monitoredApps = prefs.getStringSet("monitored_apps", emptySet()) ?: emptySet()
        
        if (!monitoredApps.contains(sbn.packageName)) return

        val notification = sbn.notification
        val extras = notification.extras
        
        val title = extras.getString("android.title", "") ?: ""
        val text = extras.getCharSequence("android.text", "")?.toString() ?: ""
        
        // Unngå tomme varsler
        if (title.isEmpty() && text.isEmpty()) return

        // Lag en unik nøkkel for denne varslingen
        val notificationKey = "${sbn.packageName}:${title}:${text}"
        val now = System.currentTimeMillis()
        
        // Sjekk om vi nylig har prosessert samme varsel (innen 30 sekunder)
        val lastProcessed = processedAppNotifications[notificationKey]
        if (lastProcessed != null && (now - lastProcessed) < 30000) {
            Logger.d(TAG, "Duplikat varsel ignorert fra ${sbn.packageName}")
            return
        }
        
        processedAppNotifications[notificationKey] = now

        val appName = getAppName(sbn.packageName)
        Logger.d(TAG, "App-varsel fra $appName prosesseres")

        EmailSender.sendEmail(
            this,
            "🔔 $appName: $title",
            text.ifEmpty { "(Ingen tekst)" }
        )
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun isMissedCallNotification(notification: Notification): Boolean {
        val extras = notification.extras
        val title = extras.getString("android.title", "")
        val text = extras.getCharSequence("android.text", "")?.toString() ?: ""

        return title.contains("tapt", ignoreCase = true) ||
               title.contains("missed", ignoreCase = true) ||
               title.contains("ubesvart", ignoreCase = true) ||
               text.contains("tapt", ignoreCase = true) ||
               text.contains("missed", ignoreCase = true)
    }

    private fun handleMissedCall() {
        try {
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE
            )

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

                    if (timestamp > 0 && processedCallTimestamps.containsKey(timestamp)) {
                        Logger.d(TAG, "Anrop allerede prosessert, hopper over")
                        return
                    }

                    if (timestamp > 0) {
                        processedCallTimestamps[timestamp] = true
                    }

                    Logger.d(TAG, "Tapt anrop prosesseres")

                    EmailSender.sendEmail(this, "📞 Tapt anrop: $name", "Nummer: $number")
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
        // Ikke nødvendig
    }

    override fun onDestroy() {
        super.onDestroy()
        processedCallTimestamps.clear()
        processedAppNotifications.clear()
    }
}