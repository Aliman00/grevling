package com.smsforwarder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Stats Widget (2x2) - Viser detaljert statistikk og toggle-knapp.
 * Viser antall SMS, anrop, siste videresending og pause/resume knapp.
 */
class StatsWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.smsforwarder.ACTION_TOGGLE_STATS"
        private const val TAG = "StatsWidget"
        private const val REQUEST_CODE = 2

        /**
         * Oppdaterer alle stats widget-instanser.
         */
        fun updateAllWidgets(context: Context) {
            WidgetHelper.updateAllWidgetsOfType(context, StatsWidget::class.java)
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
            WidgetHelper.toggleForwarding(context, TAG)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val isEnabled = WidgetHelper.isForwardingEnabled(context)
        val views = RemoteViews(context.packageName, R.layout.widget_stats)

        // Hent statistikk
        val smsCount = ForwardingStats.getSmsCountToday(context)
        val callsCount = ForwardingStats.getCallsCountToday(context)

        // Oppdater statistikk på én linje
        views.setTextViewText(R.id.widget_stats_line, "$smsCount SMS · $callsCount calls")

        // Oppdater status og knapp
        if (isEnabled) {
            views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_status_dot_active)
            views.setInt(R.id.widget_toggle_button, "setBackgroundResource", R.drawable.widget_button_pause)
            views.setTextViewText(R.id.widget_button_text, context.getString(R.string.widget_button_pause))
        } else {
            views.setImageViewResource(R.id.widget_status_dot, R.drawable.widget_status_dot_inactive)
            views.setInt(R.id.widget_toggle_button, "setBackgroundResource", R.drawable.widget_button_resume)
            views.setTextViewText(R.id.widget_button_text, context.getString(R.string.widget_button_activate))
        }

        // Sett opp klikk-handling for toggle-knappen
        val toggleIntent = WidgetHelper.createTogglePendingIntent(
            context, StatsWidget::class.java, ACTION_TOGGLE, REQUEST_CODE
        )
        views.setOnClickPendingIntent(R.id.widget_toggle_button, toggleIntent)

        // Klikk på hele widgeten åpner appen
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE + 100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_stats_container, openAppPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
