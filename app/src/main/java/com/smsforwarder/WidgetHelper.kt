package com.smsforwarder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Hjelpeklasse for delt widget-funksjonalitet.
 * Reduserer kodeduplisering mellom ForwardingWidget og ForwardingWidgetMini.
 */
object WidgetHelper {

    private const val TAG = "WidgetHelper"

    /**
     * Toggles forwarding-status og oppdaterer alle widgets.
     * 
     * @param context Application context
     * @param callerTag Logg-tag for kallende widget (for debugging)
     */
    fun toggleForwarding(context: Context, callerTag: String) {
        val prefs = PreferencesManager.getEncryptedPreferences(context)
        val currentState = prefs.getBoolean("enabled", false)
        prefs.edit().putBoolean("enabled", !currentState).apply()

        Logger.d(TAG, "$callerTag toggle: ${!currentState}")

        // Oppdater alle widget-typer
        updateAllWidgetsOfType(context, ForwardingWidget::class.java)
        updateAllWidgetsOfType(context, ForwardingWidgetMini::class.java)
        updateAllWidgetsOfType(context, StatsWidget::class.java)
    }

    /**
     * Oppdaterer alle widget-instanser av en gitt type.
     */
    fun <T : AppWidgetProvider> updateAllWidgetsOfType(context: Context, widgetClass: Class<T>) {
        val intent = Intent(context, widgetClass).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val widgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = widgetManager.getAppWidgetIds(ComponentName(context, widgetClass))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        context.sendBroadcast(intent)
    }

    /**
     * Henter gjeldende forwarding-status.
     */
    fun isForwardingEnabled(context: Context): Boolean {
        val prefs = PreferencesManager.getEncryptedPreferences(context)
        return prefs.getBoolean("enabled", false)
    }

    /**
     * Oppretter PendingIntent for widget-klikk.
     * 
     * @param context Application context
     * @param widgetClass Widget-klassen som skal motta klikket
     * @param action Action-strengen for intent
     * @param requestCode Unik request code for denne widget-typen
     */
    fun <T : AppWidgetProvider> createTogglePendingIntent(
        context: Context,
        widgetClass: Class<T>,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, widgetClass).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
