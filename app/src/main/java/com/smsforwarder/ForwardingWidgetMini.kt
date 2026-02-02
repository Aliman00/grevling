package com.smsforwarder

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
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
        private const val REQUEST_CODE = 1

        /**
         * Oppdaterer alle mini-widget-instanser.
         */
        fun updateAllWidgets(context: Context) {
            WidgetHelper.updateAllWidgetsOfType(context, ForwardingWidgetMini::class.java)
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
        WidgetHelper.toggleForwarding(context, TAG)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val isEnabled = WidgetHelper.isForwardingEnabled(context)
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
        val pendingIntent = WidgetHelper.createTogglePendingIntent(
            context, ForwardingWidgetMini::class.java, ACTION_TOGGLE, REQUEST_CODE
        )
        views.setOnClickPendingIntent(R.id.widget_mini_background, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
