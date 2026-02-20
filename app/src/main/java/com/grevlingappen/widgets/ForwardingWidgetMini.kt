package com.grevlingappen.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import com.grevlingappen.R

/**
 * ForwardingWidgetMini (1x1) - Kompakt på/av-knapp for videresending.
 */
class ForwardingWidgetMini : BaseWidget() {

    override val toggleAction = ACTION_TOGGLE
    override val tag = TAG

    companion object {
        const val ACTION_TOGGLE = "com.grevlingappen.ACTION_TOGGLE_MINI"
        private const val TAG = "ForwardingWidgetMini"
        private const val REQUEST_CODE = 1
    }

    override suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val appContext = context.applicationContext
        val views = RemoteViews(appContext.packageName, R.layout.widget_mini_forwarding)

        val isEnabled = WidgetHelper.isForwardingEnabled(appContext)

        // Oppdater ikon og bakgrunn basert på status fra strings.xml
        if (isEnabled) {
            views.setTextViewText(R.id.widget_mini_icon, appContext.getString(R.string.widget_mini_icon_active))
            views.setInt(R.id.widget_mini_background, "setBackgroundResource", R.drawable.widget_mini_background_active)
        } else {
            views.setTextViewText(R.id.widget_mini_icon, appContext.getString(R.string.widget_mini_icon_paused))
            views.setInt(R.id.widget_mini_background, "setBackgroundResource", R.drawable.widget_mini_background_inactive)
        }

        // Sett opp klikk-handling
        val pendingIntent = WidgetHelper.createTogglePendingIntent(appContext, ForwardingWidgetMini::class.java, ACTION_TOGGLE, REQUEST_CODE)
        views.setOnClickPendingIntent(R.id.widget_mini_background, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}