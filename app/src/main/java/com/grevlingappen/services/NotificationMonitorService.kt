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

/**
 * NotificationMonitorService - Android NotificationListener-tjeneste.
 * 
 * Funksjonalitet:
 * - Overvåker alle varsler fra installerte apper
 * - Registrerer tapte anrop via CallLog-observatør
 * - Videresender varsler og tapte anrop til konfigurert e-post
 * - Sender auto-svar ved tapte anrop hvis aktivert
 * 
 * Tjenesten krever at brukeren gir Notification Access i Android-innstillinger.
 * Den kjører som bakgrunnstjeneste så lenge brukeren har gitt tillatelse.
 */
class NotificationMonitorService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationMonitor"
        
        // Maksimalt antall lagrede tapte anrop (for å unngå memory leaks)
        private const val MAX_PROCESSED_MISSED_CALLS = 500
        // Maksimalt antall lagrede varsler (for å unngå memory leaks)
        private const val MAX_PROCESSED_NOTIFICATIONS = 1000
        // Tidsvindu for å anse et anrop som "nylig" (30 sekunder)
        private const val RECENT_CALL_THRESHOLD_MS = 30_000L
        // Hvor lenge data beholdes før opprydding (24 timer)
        private const val ITEM_MAX_AGE_MS = 86_400_000L
    }

    // Repository for å hente app-innstillinger
    private lateinit var prefsRepo: PreferencesRepository
    // Coroutine-scope for asynkrone oppgaver i denne tjenesten
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Thread-safe samlinger for å holde styr på prosesserte hendelser
    // Bruker synchronized collections fordi disse aksesseres fra flere tråder
    private val activeCalls = Collections.synchronizedMap(LinkedHashMap<String, Long>())
    private val processedMissedCalls = Collections.synchronizedSet(LinkedHashSet<Long>())
    private val processedNotifications = Collections.synchronizedSet(LinkedHashSet<String>())

    // Cacher listen over overvåkede apper for å unngå hyppige SharedPreferences-oppslag
    @Volatile
    private var cachedMonitoredApps: Set<String> = emptySet()

    /**
     * ContentObserver som lytter på endringer i anropsloggen.
     * Trigger automatisk når nye anrop legges til i loggen,
     * noe som er mer pålitelig enn å polling med fast interval.
     */
    private val callLogObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            // Kun håndtere endringer i anropsloggen, ikke andre URIer
            if (uri == null || uri == CallLog.Calls.CONTENT_URI) {
                scope.launch {
                    checkForMissedCall()
                }
            }
        }
    }

    /**
     * Kjøres når tjenesten opprettes av systemet.
     * Initialiserer preferences og registrerer CallLog-observer.
     */
    override fun onCreate() {
        super.onCreate()
        prefsRepo = PreferencesRepository.getInstance(applicationContext)
        
        try {
            // Registrer observatør for å fange opp tapte anrop
            contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,  // observerChildren: true for å få beskjed om alle endringer
                callLogObserver
            )
            Logger.i(TAG, "NotificationMonitor tjeneste startet")
        } catch (e: Exception) {
            Logger.e(TAG, "Kunne ikke registrere CallLog-observer", e)
        }
    }

    /**
     * Kjøres når NotificationListener er koblet til systemet.
     * Nå kan vi begynne å motta varsler.
     */
    override fun onListenerConnected() {
        super.onListenerConnected()
        cachedMonitoredApps = prefsRepo.getMonitoredApps()
        
        // Lytt på endringer i overvåkede apper
        scope.launch {
            prefsRepo.state.collect {
                cachedMonitoredApps = prefsRepo.getMonitoredApps()
            }
        }
        
        Logger.i(TAG, "NotificationListener koblet til med ${cachedMonitoredApps.size} overvåkede apper")
    }

    /**
     * Kjøres når tjenesten destrueres.
     * Rydder opp i ressurser: avregistrerer observatører og kansellerer coroutines.
     */
    override fun onDestroy() {
        try {
            contentResolver.unregisterContentObserver(callLogObserver)
        } catch (e: Exception) {
            Logger.w(TAG, "Feil ved avregistrering av observer", e)
        }
        scope.cancel()
        super.onDestroy()
        Logger.i(TAG, "NotificationMonitor tjeneste stoppet")
    }

    /**
     * Mottatt nytt varsel fra en app.
     * Sjekker om det er et anrop-varsel eller vanlig app-varsel.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val state = prefsRepo.state.value
        // Kun videresend hvis aktivert og e-post er konfigurert
        if (!state.isEnabled || !state.hasEmailConfig) return
        // Ignorer varsler fra vår egen app
        if (sbn.packageName == packageName) return

        val notification = sbn.notification ?: return
        val pkg = sbn.packageName
        val cat = notification.category
        val time = sbn.postTime

        // Anrop-varsler håndteres annerledes enn vanlige varsler
        when (cat) {
            Notification.CATEGORY_CALL -> activeCalls[pkg] = time
            else -> handleAppNotification(pkg, notification, time)
        }
    }

    /**
     * Varsel ble fjernet fra systemet.
     * Fjerner fra activeCalls hvis det var et anrop.
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val state = prefsRepo.state.value
        if (!state.isEnabled || !state.hasEmailConfig) return

        val pkg = sbn.packageName
        val cat = sbn.notification?.category

        // Ved anrop: fjern fra aktiv liste - ContentObserver håndterer selve miss-sjekken
        if (cat == Notification.CATEGORY_CALL && activeCalls.containsKey(pkg)) {
            activeCalls.remove(pkg)
            // Merk: Vi trenger ikke kalle checkForMissedCall manuelt her
            // da ContentObserver vil fange opp endringen i CallLog automatisk
        }
    }

    /**
     * Sjekker om det finnes nylige tapte anrop i loggen.
     * Kalles automatisk av ContentObserver ved endringer i anropsloggen.
     */
    private suspend fun checkForMissedCall() {
        // Hent siste tapte anrop fra loggen
        val missCall = getLastMissedCall()
        if (missCall == null || missCall.number.isBlank()) return

        // Sjekk om anropet er innenfor "nylig"-vinduet (30 sekunder)
        if (!isRecentMissedCall(missCall.time)) {
            // Normalt - observeren trigger ved alle endringer i loggen
            return
        }

        // Sjekk om vi allerede har prosessert dette anropet
        if (missCall.time == 0L || processedMissedCalls.contains(missCall.time)) return

        processedMissedCalls.add(missCall.time)
        
        // Rydd opp i minnet hvis det blir for mange lagrede anrop
        if (processedMissedCalls.size > MAX_PROCESSED_MISSED_CALLS) {
            cleanupOldData()
        }
        
        // Send e-post om tapte anrop
        sendCallEmail(missCall.number)
    }

    /**
     * Håndterer vanlig app-varsel (ikke anrop).
     * Videresender varselet til e-post hvis appen er overvåket.
     */
    private fun handleAppNotification(pkg: String, notification: Notification, time: Long) {
        // Kun videresend hvis dette er en overvåket app
        if (!isMonitoredApp(pkg)) return

        // Unngå duplikater ved å bruke unik nøkkel per varsel
        val key = "$pkg:$time"
        if (processedNotifications.contains(key)) return
        processedNotifications.add(key)

        // Begrens minnebruk
        if (processedNotifications.size > MAX_PROCESSED_NOTIFICATIONS) {
            cleanupOldData()
        }

        // Hent varselets innhold (tittel og tekst)
        val title = notification.extras?.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString() ?: ""
        val text = notification.extras?.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString() ?: ""

        // Send e-post hvis det er innhold
        if (title.isNotBlank() || text.isNotBlank()) {
            scope.launch {
                sendNotificationEmail(pkg, title, text)
            }
        }
    }

    /**
     * Data holder for informasjon om tapte anrop.
     */
    private data class MissedCallInfo(
        val number: String,  // Telefonnummer
        val time: Long       // Tidspunkt i millisekunder
    )

    /**
     * Henter siste tapte anrop fra anropsloggen.
     * @return MissedCallInfo eller null hvis ikke funnet/manglende tillatelse
     */
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
            Logger.e(TAG, "Mangler CALL_LOG tillatelse - kan ikke lese anropslogg")
            null
        }
    }

    /**
     * Fjerner gamle oppføringer fra minnet for å unngå memory leaks.
     * Beholder kun data som er nyere enn ITEM_MAX_AGE_MS (24 timer).
     */
    private fun cleanupOldData() {
        val now = System.currentTimeMillis()
        
        // Rydd i tapte anrop
        synchronized(processedMissedCalls) {
            processedMissedCalls.removeIf { callTime ->
                (now - callTime) > ITEM_MAX_AGE_MS
            }
        }
        
        // Rydd i varsler
        synchronized(processedNotifications) {
            processedNotifications.removeIf { notificationKey ->
                val timestamp = notificationKey.split(":").lastOrNull()?.toLongOrNull() ?: 0L
                (now - timestamp) > ITEM_MAX_AGE_MS
            }
        }
        
        // Rydd i aktive anrop
        synchronized(activeCalls) {
            activeCalls.entries.removeIf { (_, postTime) ->
                (now - postTime) > ITEM_MAX_AGE_MS
            }
        }
        
        Logger.d(TAG, "Opprydding fullført. activeCalls: ${activeCalls.size}, missedCalls: ${processedMissedCalls.size}, notifications: ${processedNotifications.size}")
    }

    /**
     * Sjekker om et anrop er innenfor "nylig"-vinduet.
     * @param time Anropets tidspunkt i millisekunder
     * @return true hvis anropet er nyere enn RECENT_CALL_THRESHOLD_MS
     */
    private fun isRecentMissedCall(time: Long): Boolean {
        val now = System.currentTimeMillis()
        return (now - time) < RECENT_CALL_THRESHOLD_MS
    }

    /**
     * Sjekker om en app er i listen over overvåkede apper.
     * @param pkg Package name til appen
     * @return true hvis appen overvåkes
     */
    private fun isMonitoredApp(pkg: String): Boolean = cachedMonitoredApps.contains(pkg)

    /**
     * Sender e-post om tapte anrop.
     * Inkluderer telefonnummer og tidspunkt.
     */
    private suspend fun sendCallEmail(number: String) {
        val sender = ContactHelper.formatSender(this@NotificationMonitorService, number)
        val subject = getString(R.string.email_call_subject, sender)
        val body = getString(R.string.email_call_body, number, now())

        EmailSender.enqueueEmail(this@NotificationMonitorService, subject, body)
        Logger.i(TAG, "Tapte anrop videresendt via e-post")
        
        // Registrer statistikk og send auto-svar
        ForwardingStats.recordCallForwarded(this@NotificationMonitorService)
        AutoReplyHelper.sendCallAutoReply(this@NotificationMonitorService, number)
    }

    /**
     * Sender e-post om app-varsel.
     * Inkluderer appnavn, varseltittel og varseltekst.
     */
    private fun sendNotificationEmail(pkg: String, title: String, text: String) {
        val appName = getAppName(pkg)
        val subject = getString(R.string.email_notif_subject, appName)
        val body = getString(R.string.email_notif_body, appName, title, text, now())

        EmailSender.enqueueEmail(this, subject, body)
    }

    /**
     * Henter lesbart appnavn fra package name.
     * Fallbacker til package name hvis appnavn ikke finnes.
     */
    private fun getAppName(pkg: String): String = try {
        packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(pkg, 0)
        ).toString()
    } catch (_: Exception) {
        pkg
    }

    /**
     * Formaterer gjeldende tidspunkt som norsk datetime-streng.
     * @return String på formatet "dd.MM.yyyy HH:mm:ss"
     */
    private fun now(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
}