package com.grevlingappen.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.grevlingappen.R
import com.grevlingappen.utils.ForwardingStats

/**
 * StatsWidget (3x1) - Viser dagsstatistikk for SMS og anrop, samt toggle-knapp.
 */
class StatsWidget : BaseWidget() {

    override val toggleAction = ACTION_TOGGLE
    override val tag = TAG

    companion object {
        const val ACTION_TOGGLE = "com.grevlingappen.ACTION_TOGGLE_STATS"
        private const val TAG = "StatsWidget"
        private const val REQUEST_CODE_BASE = 200
    }

    override suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val appContext = context.applicationContext
        val views = RemoteViews(appContext.packageName, R.layout.widget_stats)

        val isEnabled = WidgetHelper.isForwardingEnabled(appContext)
        val smsCount = ForwardingStats.getSmsCountToday(appContext)
        val callsCount = ForwardingStats.getCallsCountToday(appContext)
        val resources = WidgetHelper.getStatusResources(isEnabled)

        // Oppdater statistikk
        views.setTextViewText(R.id.widget_stats_line, appContext.getString(R.string.widget_stats_combined, smsCount, callsCount))

        // Velg ressurser basert på status
        val buttonBg = if (isEnabled) R.drawable.widget_button_pause else R.drawable.widget_button_resume
        val buttonText = if (isEnabled) R.string.widget_button_pause else R.string.widget_button_activate

        // Oppdater visuelle elementer
        views.setImageViewResource(R.id.widget_status_dot, resources.iconRes)
        views.setInt(R.id.widget_toggle_button, "setBackgroundResource", buttonBg)
        views.setTextViewText(R.id.widget_button_text, appContext.getString(buttonText))
        views.setTextColor(R.id.widget_button_text, ContextCompat.getColor(appContext, resources.colorRes))

        // Toggle-knapp handling
        val toggleIntent = WidgetHelper.createTogglePendingIntent(appContext, StatsWidget::class.java, ACTION_TOGGLE, REQUEST_CODE_BASE)
        views.setOnClickPendingIntent(R.id.widget_toggle_button, toggleIntent)

        // Klikk på statistikken åpner appen
        val openPendingIntent = WidgetHelper.createOpenAppPendingIntent(appContext, REQUEST_CODE_BASE + 1)
        views.setOnClickPendingIntent(R.id.widget_stats_container, openPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}