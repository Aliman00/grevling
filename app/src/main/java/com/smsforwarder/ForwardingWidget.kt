package com.smsforwarder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ForwardingWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.smsforwarder.ACTION_TOGGLE_FORWARDING"
        private const val TAG = "ForwardingWidget"

        /**
         * Oppdaterer alle widget-instanser.
         * Kan kalles fra andre deler av appen når status endres.
         */
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, ForwardingWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val widgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = widgetManager.getAppWidgetIds(
                ComponentName(context, ForwardingWidget::class.java)
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

        Logger.d(TAG, "Widget toggle: ${!currentState}")

        // Oppdater alle widgets
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, ForwardingWidget::class.java)
        )
        for (widgetId in widgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = PreferencesManager.getEncryptedPreferences(context)
        val isEnabled = prefs.getBoolean("enabled", false)

        val views = RemoteViews(context.packageName, R.layout.widget_forwarding)

        // Oppdater utseende basert på status
        if (isEnabled) {
            views.setTextViewText(R.id.widget_status_text, context.getString(R.string.widget_status_active))
            views.setTextViewText(R.id.widget_icon, "📱")
            views.setInt(R.id.widget_background, "setBackgroundResource", R.drawable.widget_background_active)
        } else {
            views.setTextViewText(R.id.widget_status_text, context.getString(R.string.widget_status_paused))
            views.setTextViewText(R.id.widget_icon, "⏸️")
            views.setInt(R.id.widget_background, "setBackgroundResource", R.drawable.widget_background_inactive)
        }

        // Sett opp klikk-handling
        val toggleIntent = Intent(context, ForwardingWidget::class.java).apply {
            action = ACTION_TOGGLE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_background, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}