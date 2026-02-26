package com.grevlingappen.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.grevlingappen.R
import com.grevlingappen.utils.ForwardingStats

/**
 * ForwardingWidget - App-widget som viser videresendingsstatus og statistikk.
 * 
 * Viser:
 * - Antall meldinger/anrop videresendt i dag
 * - Tidspunkt for siste videresending
 * - Status-indikator (aktiv/pausert)
 * 
 * Layout: 3x1 (bred)
 * 
 * Bruker:
 * - Klikk på status: Veksler videresending av/på
 * - Klikk på info: Åpner hovedappen
 */
class ForwardingWidget : BaseWidget() {

    override val toggleAction = ACTION_TOGGLE
    override val tag = TAG

    companion object {
        // Broadcast action for toggle-handling
        const val ACTION_TOGGLE = "com.grevlingappen.ACTION_TOGGLE_FORWARDING"
        private const val TAG = "ForwardingWidget"
        // Basis request code for å unngå konflikter mellom ulike widgets
        private const val REQUEST_CODE_BASE = 100
    }

    /**
     * Oppdaterer widgetens innhold.
     * Henter gjeldende status og statistikk, og oppdaterer RemoteViews.
     */
    override suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val appContext = context.applicationContext
        val views = RemoteViews(appContext.packageName, R.layout.widget_forwarding)

        // Hent data fra systemet
        val isEnabled = WidgetHelper.isForwardingEnabled(appContext)
        val totalCount = ForwardingStats.getTotalCountToday(appContext)
        val lastTime = ForwardingStats.getLastForwardedTimeAgo(appContext)
        val resources = WidgetHelper.getStatusResources(isEnabled)

        // Oppdater statistikk-tekster
        views.setTextViewText(R.id.widget_forwarded_count, appContext.getString(R.string.widget_forwarded_today, totalCount))
        views.setTextViewText(R.id.widget_last_time, appContext.getString(R.string.widget_last_time, lastTime))

        // Oppdater status-indikator (farge og tekst)
        views.setImageViewResource(R.id.widget_status_dot, resources.iconRes)
        views.setTextViewText(R.id.widget_status_text, appContext.getString(resources.textRes))
        views.setTextColor(R.id.widget_status_text, ContextCompat.getColor(appContext, resources.colorRes))

        // Sett opp klikk-handlinger
        // Klikk på status-delen toggler videresending av/på
        val toggleIntent = WidgetHelper.createTogglePendingIntent(appContext, ForwardingWidget::class.java, ACTION_TOGGLE, REQUEST_CODE_BASE)
        views.setOnClickPendingIntent(R.id.widget_status_container, toggleIntent)

        // Klikk på info-delen åpner hovedappen
        val openPendingIntent = WidgetHelper.createOpenAppPendingIntent(appContext, REQUEST_CODE_BASE + 1)
        views.setOnClickPendingIntent(R.id.widget_info_container, openPendingIntent)

        // Send oppdateringer til widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}