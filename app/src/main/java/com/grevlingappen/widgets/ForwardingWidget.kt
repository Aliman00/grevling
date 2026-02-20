package com.grevlingappen.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.grevlingappen.R
import com.grevlingappen.utils.ForwardingStats

/**
 * ForwardingWidget - Viser status og statistikk i en 3x1 layout.
 */
class ForwardingWidget : BaseWidget() {

    override val toggleAction = ACTION_TOGGLE
    override val tag = TAG

    companion object {
        const val ACTION_TOGGLE = "com.grevlingappen.ACTION_TOGGLE_FORWARDING"
        private const val TAG = "ForwardingWidget"
        private const val REQUEST_CODE_BASE = 100
    }

    override suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val appContext = context.applicationContext
        val views = RemoteViews(appContext.packageName, R.layout.widget_forwarding)

        val isEnabled = WidgetHelper.isForwardingEnabled(appContext)
        val totalCount = ForwardingStats.getTotalCountToday(appContext)
        val lastTime = ForwardingStats.getLastForwardedTimeAgo(appContext)
        val resources = WidgetHelper.getStatusResources(isEnabled)

        // Oppdater tekster
        views.setTextViewText(R.id.widget_forwarded_count, appContext.getString(R.string.widget_forwarded_today, totalCount))
        views.setTextViewText(R.id.widget_last_time, appContext.getString(R.string.widget_last_time, lastTime))

        // Oppdater status-prikk og tekstfarge
        views.setImageViewResource(R.id.widget_status_dot, resources.iconRes)
        views.setTextViewText(R.id.widget_status_text, appContext.getString(resources.textRes))
        views.setTextColor(R.id.widget_status_text, ContextCompat.getColor(appContext, resources.colorRes))

        // Klikk på status-delen toggler videresending
        val toggleIntent = WidgetHelper.createTogglePendingIntent(appContext, ForwardingWidget::class.java, ACTION_TOGGLE, REQUEST_CODE_BASE)
        views.setOnClickPendingIntent(R.id.widget_status_container, toggleIntent)

        // Klikk på info-delen åpner selve appen
        val openPendingIntent = WidgetHelper.createOpenAppPendingIntent(appContext, REQUEST_CODE_BASE + 1)
        views.setOnClickPendingIntent(R.id.widget_info_container, openPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}