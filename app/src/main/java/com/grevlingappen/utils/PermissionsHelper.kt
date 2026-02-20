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
 * PermissionsHelper - Håndterer sjekk og forespørsler om systemtillatelser, batterioptimalisering og varseltilgang.
 */
object PermissionsHelper {
    private const val TAG = "PermissionsHelper"

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
     */
    fun hasAllPermissions(context: Context): Boolean {
        val appContext = context.applicationContext
        return requiredPermissions.all { hasPermission(appContext, it) }
    }

    /**
     * Sjekker om appen er unntatt fra Androids batterioptimalisering (Doze Mode).
     * Dette er kritisk for at appen skal kunne kjøre stabilt i bakgrunnen over lang tid.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val appContext = context.applicationContext
        val powerManager = appContext.getSystemService(PowerManager::class.java)
        return powerManager?.isIgnoringBatteryOptimizations(appContext.packageName) == true
    }

    /**
     * Returnerer en Intent som ber brukeren om å unnta appen fra batterioptimalisering.
     * Faller tilbake til generelle batteriinnstillinger dersom direkte forespørsel ikke er mulig.
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
     * Sjekker om brukeren har aktivert varseltilgang for appen i systeminnstillinger.
     */
    fun isNotificationServiceEnabled(context: Context): Boolean {
        val appContext = context.applicationContext
        val packageName = appContext.packageName

        // Hent liste over aktiverte NotificationListeners fra systeminnstillinger
        val enabledListeners = Settings.Secure.getString(
            appContext.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        // Listen er formatert som "package1/Service1:package2/Service2"
        return enabledListeners.split(":").any {
            val componentName = ComponentName.unflattenFromString(it.trim())
            componentName?.packageName?.equals(packageName, ignoreCase = true) == true
        }
    }

    /**
     * Returnerer en Intent som tar brukeren direkte til systemets meny for varseltilgang.
     */
    fun getNotificationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Returnerer en liste over de påkrevde tillatelsene som ennå ikke er innvilget.
     */
    fun getMissingPermissions(context: Context): List<String> {
        val appContext = context.applicationContext
        return requiredPermissions.filter { !hasPermission(appContext, it) }
    }

    /**
     * Sjekker om en spesifikk tillatelse er innvilget.
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        val appContext = context.applicationContext
        return ContextCompat.checkSelfPermission(
            appContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}