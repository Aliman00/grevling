package com.grevlingappen.utils

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * PermissionsHelper - Objekt som håndterer alle tillatelser og systeminnstillinger.
 * 
 * Funksjonalitet:
 * - Sjekker om nødvendige tillatelser er gitt
 * - Sjekker batterioptimalisering-status
 * - Sjekker Notification Access-status
 * - Gir Intents for å åpne relevante systeminnstillinger
 * 
 * Appen krever flere tillatelser for å fungere:
 * - RECEIVE_SMS: Motta innkommende SMS
 * - READ_SMS: Lese SMS-innhold
 * - SEND_SMS: Sende auto-svar
 * - READ_CALL_LOG: Lese anropslogg for tapte anrop
 * - READ_CONTACTS: Slå opp kontaktnavn
 */
object PermissionsHelper {
    private const val TAG = "PermissionsHelper"
    
    // Systeminnstillinger-nøkkel for aktiverte NotificationListeners
    // Denne strengen er konstant i Android-systemet
    private companion object {
        private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    }

    /**
     * Liste over alle kritiske tillatelser som kreves for at videresending skal fungere.
     */
    val requiredPermissions = listOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS
    )

    /**
     * Sjekker om samtlige påkrevde tillatelser er innvilget av brukeren.
     * 
     * @param context App-kontekst
     * @return true hvis alle tillatelser er gitt
     */
    fun hasAllPermissions(context: Context): Boolean {
        val appContext = context.applicationContext
        return requiredPermissions.all { hasPermission(appContext, it) }
    }

    /**
     * Sjekker om appen er unntatt fra Androids batterioptimalisering (Doze Mode).
     * 
     * Dette er kritisk for at appen skal kunne kjøre stabilt i bakgrunnen.
     * Uten dette kan Android stoppe appen når skjermen er av.
     * 
     * @param context App-kontekst
     * @return true hvis batterioptimalisering er deaktivert for appen
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val appContext = context.applicationContext
        val powerManager = appContext.getSystemService(PowerManager::class.java)
        return powerManager?.isIgnoringBatteryOptimizations(appContext.packageName) == true
    }

    /**
     * Returnerer en Intent som ber brukeren om å unnta appen fra batterioptimalisering.
     * 
     * Viser direkte dialog der brukeren kan godkjenne unntak.
     * Hvis dette ikke er tilgjengelig (noen eldre enheter), fallback til generelle innstillinger.
     * 
     * @param context App-kontekst
     * @return Intent for batterioptimaliseringsdialog
     */
    fun getBatteryOptimizationIntent(context: Context): Intent {
        val appContext = context.applicationContext
        return try {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:${appContext.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Kunne ikke åpne direkte batteri-prompt, bruker fallback", e)
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Sjekker om brukeren har aktivert Notification Access for appen.
     * 
     * Dette er påkrevt for å fange opp varsler fra andre apper.
     * Må aktiveres manuelt av brukeren i Android-innstillinger.
     * 
     * @param context App-kontekst
     * @return true hvis Notification Access er gitt
     */
    fun isNotificationServiceEnabled(context: Context): Boolean {
        val appContext = context.applicationContext
        val packageName = appContext.packageName

        // Hent liste over aktiverte NotificationListeners fra systeminnstillinger
        val enabledListeners = Settings.Secure.getString(
            appContext.contentResolver,
            ENABLED_NOTIFICATION_LISTENERS
        ) ?: return false

        // Listen er formatert som "package1/Service1:package2/Service2"
        return enabledListeners.split(":").any {
            val componentName = ComponentName.unflattenFromString(it.trim())
            componentName?.packageName?.equals(packageName, ignoreCase = true) == true
        }
    }

    /**
     * Returnerer en Intent som tar brukeren direkte til systemets meny for varseltilgang.
     * 
     * @return Intent for Notification Access-innstillinger
     */
    fun getNotificationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Returnerer en liste over de påkrevde tillatelsene som ennå ikke er innvilget.
     * 
     * @param context App-kontekst
     * @return Liste med manglende tillatelser
     */
    fun getMissingPermissions(context: Context): List<String> {
        val appContext = context.applicationContext
        return requiredPermissions.filter { !hasPermission(appContext, it) }
    }

    /**
     * Sjekker om en spesifikk tillatelse er innvilget.
     * 
     * @param context App-kontekst
     * @param permission Tillatelsen å sjekke
     * @return true hvis tillatelsen er gitt
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        val appContext = context.applicationContext
        return ContextCompat.checkSelfPermission(
            appContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}