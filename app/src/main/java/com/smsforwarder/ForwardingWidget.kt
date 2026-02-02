package com.smsforwarder

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ForwardingWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.smsforwarder.ACTION_TOGGLE_FORWARDING"
        private const val TAG = "ForwardingWidget"
        private const val REQUEST_CODE = 0

        /**
         * Oppdaterer alle widget-instanser.
         * Kan kalles fra andre deler av appen når status endres.
         */
        fun updateAllWidgets(context: Context) {
            WidgetHelper.updateAllWidgetsOfType(context, ForwardingWidget::class.java)
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
        val pendingIntent = WidgetHelper.createTogglePendingIntent(
            context, ForwardingWidget::class.java, ACTION_TOGGLE, REQUEST_CODE
        )
        views.setOnClickPendingIntent(R.id.widget_background, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}