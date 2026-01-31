package com.smsforwarder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Mini-widget (1x1) - Bare et ikon som fungerer som på/av-knapp.
 * Enkel og kompakt toggle for hjemskjermen.
 */
class ForwardingWidgetMini : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.smsforwarder.ACTION_TOGGLE_MINI"
        private const val TAG = "ForwardingWidgetMini"

        /**
         * Oppdaterer alle mini-widget-instanser.
         */
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, ForwardingWidgetMini::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val widgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = widgetManager.getAppWidgetIds(
                ComponentName(context, ForwardingWidgetMini::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE) {
            toggleForwarding(context)
        }
    }

    private fun toggleForwarding(context: Context) {
        val prefs = PreferencesManager.getEncryptedPreferences(context)
        val currentState = prefs.getBoolean("enabled", false)
        prefs.edit().putBoolean("enabled", !currentState).apply()

        Logger.d(TAG, "Mini widget toggle: ${!currentState}")

        // Oppdater alle mini-widgets
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, ForwardingWidgetMini::class.java)
        )
        for (widgetId in widgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }

        // Oppdater også de vanlige widgets
        ForwardingWidget.updateAllWidgets(context)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = PreferencesManager.getEncryptedPreferences(context)
        val isEnabled = prefs.getBoolean("enabled", false)

        val views = RemoteViews(context.packageName, R.layout.widget_mini_forwarding)

        // Oppdater utseende basert på status
        if (isEnabled) {
            views.setTextViewText(R.id.widget_mini_icon, "✅")
            views.setInt(R.id.widget_mini_background, "setBackgroundResource", R.drawable.widget_mini_background_active)
        } else {
            views.setTextViewText(R.id.widget_mini_icon, "⏸️")
            views.setInt(R.id.widget_mini_background, "setBackgroundResource", R.drawable.widget_mini_background_inactive)
        }

        // Sett opp klikk-handling
        val toggleIntent = Intent(context, ForwardingWidgetMini::class.java).apply {
            action = ACTION_TOGGLE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1, // Bruker 1 for å unngå konflikt med hovedwidget
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_mini_background, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
