package com.grevlingappen.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.grevlingappen.R
import com.grevlingappen.utils.ForwardingStats

/**
 * StatsWidget - Widget som viser detaljert dagsstatistikk.
 * 
 * Viser:
 * - Antall SMS videresendt i dag
 * - Antall anrop videresendt i dag
 * - Toggle-knapp for å aktivere/deaktivere videresending
 * 
 * Layout: 3x1 (bred)
 * 
 * Bruker:
 * - Klikk på toggle-knapp: Veksler videresending
 * - Klikk på statistikk: Åpner hovedappen
 */
class StatsWidget : BaseWidget() {

    override val toggleAction = ACTION_TOGGLE
    override val tag = TAG

    companion object {
        // Broadcast action for denne widgetens toggle
        const val ACTION_TOGGLE = "com.grevlingappen.ACTION_TOGGLE_STATS"
        private const val TAG = "StatsWidget"
        // Basis request code for å unngå konflikter
        private const val REQUEST_CODE_BASE = 200
    }

    /**
     * Oppdaterer widgetens innhold med gjeldende statistikk.
     */
    override suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val appContext = context.applicationContext
        val views = RemoteViews(appContext.packageName, R.layout.widget_stats)

        // Hent data
        val isEnabled = WidgetHelper.isForwardingEnabled(appContext)
        val smsCount = ForwardingStats.getSmsCountToday(appContext)
        val callsCount = ForwardingStats.getCallsCountToday(appContext)
        val resources = WidgetHelper.getStatusResources(isEnabled)

        // Oppdater statistikk-tekst
        views.setTextViewText(R.id.widget_stats_line, appContext.getString(R.string.widget_stats_combined, smsCount, callsCount))

        // Velg ressurser basert på status (aktivert vs pausert)
        val buttonBg = if (isEnabled) R.drawable.widget_button_pause else R.drawable.widget_button_resume
        val buttonText = if (isEnabled) R.string.widget_button_pause else R.string.widget_button_activate

        // Oppdater visuelle elementer
        views.setImageViewResource(R.id.widget_status_dot, resources.iconRes)
        views.setInt(R.id.widget_toggle_button, "setBackgroundResource", buttonBg)
        views.setTextViewText(R.id.widget_button_text, appContext.getString(buttonText))
        views.setTextColor(R.id.widget_button_text, ContextCompat.getColor(appContext, resources.colorRes))

        // Sett opp klikk-handlinger
        // Toggle-knapp: veksler videresending
        val toggleIntent = WidgetHelper.createTogglePendingIntent(appContext, StatsWidget::class.java, ACTION_TOGGLE, REQUEST_CODE_BASE)
        views.setOnClickPendingIntent(R.id.widget_toggle_button, toggleIntent)

        // Statistikk-delen: åpner hovedappen
        val openPendingIntent = WidgetHelper.createOpenAppPendingIntent(appContext, REQUEST_CODE_BASE + 1)
        views.setOnClickPendingIntent(R.id.widget_stats_container, openPendingIntent)

        // Send oppdatering til widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}