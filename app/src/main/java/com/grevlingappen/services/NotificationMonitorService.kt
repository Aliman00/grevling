package com.grevlingappen.services

import android.app.Notification
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.grevlingappen.R
import com.grevlingappen.data.PreferencesRepository
import com.grevlingappen.utils.AutoReplyHelper
import com.grevlingappen.utils.ContactHelper
import com.grevlingappen.utils.EmailSender
import com.grevlingappen.utils.ForwardingStats
import com.grevlingappen.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.LinkedHashMap
import java.util.LinkedHashSet

class NotificationMonitorService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationMonitor"
        
        // Magiske tall erstattet med navngitte konstanter
        private const val MAX_PROCESSED_MISSED_CALLS = 500
        private const val MAX_PROCESSED_NOTIFICATIONS = 1000
        private const val RETRY_DELAY_MS = 1500L
        private const val RECENT_CALL_THRESHOLD_MS = 30_000L
        
        // Tidsbasert opprydding
        private const val ITEM_MAX_AGE_MS = 86_400_000L // 24 timer
    }

    private lateinit var prefsRepo: PreferencesRepository
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Bruk synchronized collections for thread-safety
    private val activeCalls = Collections.synchronizedMap(
        LinkedHashMap<String, Long>()
    )
    private val processedMissedCalls = Collections.synchronizedSet(
        LinkedHashSet<Long>()
    )
    private val processedNotifications = Collections.synchronizedSet(
        LinkedHashSet<String>()
    )

    @Volatile
    private var cachedMonitoredApps: Set<String> = emptySet()

    // Lytter på endringer i CallLog (mer pålitelig enn forsinkelse)
    private val callLogObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            // Sjekk bare hvis endringen faktisk gjelder anropsloggen
            if (uri == null || uri == CallLog.Calls.CONTENT_URI) {
                scope.launch {
                    checkForMissedCall()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefsRepo = PreferencesRepository.getInstance(applicationContext)
        
        try {
            contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                callLogObserver
            )
            Logger.i(TAG, "Tjeneste startet og lytter på anropslogg")
        } catch (e: Exception) {
            Logger.e(TAG, "Kunne ikke registrere CallLogObserver", e)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        cachedMonitoredApps = prefsRepo.getMonitoredApps()
        
        scope.launch {
            prefsRepo.state.collect {
                cachedMonitoredApps = prefsRepo.getMonitoredApps()
            }
        }
        
        Logger.i(TAG, "Listener koblet til med ${cachedMonitoredApps.size} overvåkede apper")
    }

    override fun onDestroy() {
        try {
            contentResolver.unregisterContentObserver(callLogObserver)
        } catch (e: Exception) {
            Logger.w(TAG, "Feil ved avregistrering av observer", e)
        }
        scope.cancel()
        super.onDestroy()
        Logger.i(TAG, "Tjeneste stoppet")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val state = prefsRepo.state.value
        if (!state.isEnabled || !state.hasEmailConfig) return
        if (sbn.packageName == packageName) return

        val notification = sbn.notification ?: return
        val pkg = sbn.packageName
        val cat = notification.category
        val time = sbn.postTime

        when (cat) {
            Notification.CATEGORY_CALL -> activeCalls[pkg] = time
            else -> handleAppNotification(pkg, notification, time)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val state = prefsRepo.state.value
        if (!state.isEnabled || !state.hasEmailConfig) return

        val pkg = sbn.packageName
        val cat = sbn.notification?.category

        if (cat == Notification.CATEGORY_CALL && activeCalls.containsKey(pkg)) {
            activeCalls.remove(pkg)
            // Vi trenger ikke kalle checkForMissedCall her lenger,
            // da ContentObserver vil plukke opp endringen i CallLog automatisk.
        }
    }

    private suspend fun checkForMissedCall() {
        // Ingen delay nødvendig lenger takket være ContentObserver

        val missCall = getLastMissedCall()
        if (missCall == null || missCall.number.isBlank()) return

        if (!isRecentMissedCall(missCall.time)) {
            // Dette er normalt - observeren trigger ved alle endringer i loggen
            return
        }

        if (missCall.time == 0L || processedMissedCalls.contains(missCall.time)) return

        processedMissedCalls.add(missCall.time)
        // Begrens minnebruk - bruk cleanupOldData i stedet for clear
        if (processedMissedCalls.size > MAX_PROCESSED_MISSED_CALLS) {
            cleanupOldData()
        }
        sendCallEmail(missCall.number)
    }

    private fun handleAppNotification(pkg: String, notification: Notification, time: Long) {
        if (!isMonitoredApp(pkg)) return

        val key = "$pkg:$time"
        if (processedNotifications.contains(key)) return
        processedNotifications.add(key)

        // Begrens minnebruk
        if (processedNotifications.size > MAX_PROCESSED_NOTIFICATIONS) {
            cleanupOldData()
        }

        val title = notification.extras?.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString() ?: ""
        val text = notification.extras?.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString() ?: ""

        if (title.isNotBlank() || text.isNotBlank()) {
            scope.launch {
                sendNotificationEmail(pkg, title, text)
            }
        }
    }

    private data class MissedCallInfo(
        val number: String,
        val time: Long
    )

    private fun getLastMissedCall(): MissedCallInfo? {
        return try {
            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE),
                "${CallLog.Calls.TYPE} = ?",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString()),
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    MissedCallInfo(cursor.getString(0), cursor.getLong(1))
                } else null
            }
        } catch (_: SecurityException) {
            Logger.e(TAG, "Mangler CALL_LOG tillatelse")
            null
        }
    }

    // Fjernet duplikatfunksjonene - bruk getLastMissedCall() i stedet

    private fun cleanupOldData() {
        val now = System.currentTimeMillis()
        
        // Trenger ekstern synkronisering for removeIf!
        synchronized(processedMissedCalls) {
            processedMissedCalls.removeIf { callTime ->
                (now - callTime) > ITEM_MAX_AGE_MS
            }
        }
        
        synchronized(processedNotifications) {
            processedNotifications.removeIf { notificationKey ->
                val timestamp = notificationKey.split(":").lastOrNull()?.toLongOrNull() ?: 0L
                (now - timestamp) > ITEM_MAX_AGE_MS
            }
        }
        
        // Defensive cleanup for activeCalls også
        synchronized(activeCalls) {
            activeCalls.entries.removeIf { (_, postTime) ->
                (now - postTime) > ITEM_MAX_AGE_MS
            }
        }
        
        Logger.d(TAG, "Opprydding fullført. activeCalls: ${activeCalls.size}, missedCalls: ${processedMissedCalls.size}, notifications: ${processedNotifications.size}")
    }

    private fun isRecentMissedCall(time: Long): Boolean {
        val now = System.currentTimeMillis()
        return (now - time) < RECENT_CALL_THRESHOLD_MS
    }

    private fun isMonitoredApp(pkg: String): Boolean = cachedMonitoredApps.contains(pkg)

    private suspend fun sendCallEmail(number: String) {
        // Kjør direkte i suspend-konteksten
        val sender = ContactHelper.formatSender(this@NotificationMonitorService, number)
        val subject = getString(R.string.email_call_subject, sender)
        val body = getString(R.string.email_call_body, number, now())

        EmailSender.enqueueEmail(this@NotificationMonitorService, subject, body)
        Logger.i(TAG, "Anrop lagt i kø for videresending")
        
        ForwardingStats.recordCallForwarded(this@NotificationMonitorService)
        AutoReplyHelper.sendCallAutoReply(this@NotificationMonitorService, number)
    }

    private fun sendNotificationEmail(pkg: String, title: String, text: String) {
        val appName = getAppName(pkg)
        val subject = getString(R.string.email_notif_subject, appName)
        val body = getString(R.string.email_notif_body, appName, title, text, now())

        EmailSender.enqueueEmail(this, subject, body)
    }

    private fun getAppName(pkg: String): String = try {
        packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(pkg, 0)
        ).toString()
    } catch (_: Exception) {
        pkg
    }

    private fun now(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
}
