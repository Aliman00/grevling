package com.grevlingappen.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import com.grevlingappen.MainActivity
import com.grevlingappen.R
import com.grevlingappen.data.PreferenceKeys
import com.grevlingappen.utils.EncryptedPrefsFactory
import com.grevlingappen.utils.Logger
import java.util.UUID

/**
 * WidgetHelper - Hjelpeklasse for delt widget-funksjonalitet.
 * Håndterer tilstandsendringer, sikkerhets-tokens og oppdatering av alle widget-instanser.
 */
object WidgetHelper {
    private const val TAG = "WidgetHelper"
    private const val EXTRA_TOKEN = "toggle_token"

    /** Inneholder ressurser som endres basert på status */
    data class StatusResources(
        val iconRes: Int,
        val colorRes: Int,
        val textRes: Int
    )

    /** Henter gjeldende videresendings-status. */
    fun isForwardingEnabled(context: Context): Boolean {
        val prefs = EncryptedPrefsFactory.get(context.applicationContext)
        return prefs.getBoolean(PreferenceKeys.ENABLED, false)
    }

    /** Henter ikoner og farger basert på om videresending er aktiv */
    fun getStatusResources(isEnabled: Boolean): StatusResources {
        return if (isEnabled) {
            StatusResources(
                iconRes = R.drawable.widget_status_dot_active,
                colorRes = R.color.widget_active,
                textRes = R.string.widget_status_active
            )
        } else {
            StatusResources(
                iconRes = R.drawable.widget_status_dot_inactive,
                colorRes = R.color.widget_inactive,
                textRes = R.string.widget_status_paused
            )
        }
    }

    /** Oppretter en intent for å åpne hovedappen fra en widget */
    fun createOpenAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val appContext = context.applicationContext
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Toggler videresendings-status og oppdaterer alle aktive widgets. */
    fun toggleForwarding(context: Context, callerTag: String) {
        val appContext = context.applicationContext
        val prefs = EncryptedPrefsFactory.get(appContext)
        val currentState = prefs.getBoolean(PreferenceKeys.ENABLED, false)
        val newState = !currentState
        
        prefs.edit { putBoolean(PreferenceKeys.ENABLED, newState) }
        Logger.d(TAG, "$callerTag triggeret toggle: $newState")

        updateAllWidgets(appContext)
    }

    /** Sender en oppdaterings-broadcast til samtlige aktive widget-instanser. */
    fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        listOf(
            ForwardingWidget::class.java,
            ForwardingWidgetMini::class.java,
            StatsWidget::class.java
        ).forEach { updateAllWidgetsOfType(appContext, it) }
    }

    /** Oppdaterer alle instanser av en spesifikk widget-type. */
    fun <T : AppWidgetProvider> updateAllWidgetsOfType(context: Context, widgetClass: Class<T>) {
        val appContext = context.applicationContext
        val widgetManager = AppWidgetManager.getInstance(appContext)
        val ids = widgetManager.getAppWidgetIds(ComponentName(appContext, widgetClass))
        
        if (ids.isNotEmpty()) {
            val intent = Intent(appContext, widgetClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            appContext.sendBroadcast(intent)
        }
    }

    /** Henter eller genererer en unik token for å sikre widget-handlinger. */
    fun getOrCreateToken(context: Context): String {
        val prefs = EncryptedPrefsFactory.get(context.applicationContext)
        return prefs.getString(PreferenceKeys.WIDGET_TOKEN, null) ?: run {
            val newToken = UUID.randomUUID().toString()
            prefs.edit { putString(PreferenceKeys.WIDGET_TOKEN, newToken) }
            newToken
        }
    }

    /** Verifiserer at en intent inneholder gyldig sikkerhets-token. */
    fun isValidToken(context: Context, intent: Intent): Boolean {
        val intentToken = intent.getStringExtra(EXTRA_TOKEN) ?: return false
        val storedToken = EncryptedPrefsFactory.get(context.applicationContext)
            .getString(PreferenceKeys.WIDGET_TOKEN, null)
        return storedToken != null && intentToken == storedToken
    }

    /** Oppretter en PendingIntent for widget-klikk. */
    fun <T : AppWidgetProvider> createTogglePendingIntent(
        context: Context,
        widgetClass: Class<T>,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val appContext = context.applicationContext
        val token = getOrCreateToken(appContext)
        val intent = Intent(appContext, widgetClass).apply {
            this.action = action
            putExtra(EXTRA_TOKEN, token)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
