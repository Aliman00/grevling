package com.grevlingappen.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import com.grevlingappen.R

/**
 * ForwardingWidgetMini - Kompakt 1x1 widget som fungerer som på/av-knapp.
 * 
 * Viser:
 * - Ikon som indikerer aktiv/pausert status
 * - Bakgrunnsfarge som viser status
 * 
 * Layout: 1x1 (kvadratisk)
 * 
 * Funksjonalitet:
 * - Klikk på widget toggler videresending av/på
 * - Enkel og diskret måte å kontrollere appen på
 */
class ForwardingWidgetMini : BaseWidget() {

    override val toggleAction = ACTION_TOGGLE
    override val tag = TAG

    companion object {
        // Broadcast action for denne widgetens toggle
        const val ACTION_TOGGLE = "com.grevlingappen.ACTION_TOGGLE_MINI"
        private const val TAG = "ForwardingWidgetMini"
        // Unik request code for denne widgeten
        private const val REQUEST_CODE = 1
    }

    /**
     * Oppdaterer widgetens utseende basert på videresendings-status.
     */
    override suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val appContext = context.applicationContext
        val views = RemoteViews(appContext.packageName, R.layout.widget_mini_forwarding)

        // Sjekk gjeldende status
        val isEnabled = WidgetHelper.isForwardingEnabled(appContext)

        // Oppdater ikon og bakgrunn basert på status
        if (isEnabled) {
            // Aktivt: grønn bakgrunn og "play" ikon
            views.setTextViewText(R.id.widget_mini_icon, appContext.getString(R.string.widget_mini_icon_active))
            views.setInt(R.id.widget_mini_background, "setBackgroundResource", R.drawable.widget_mini_background_active)
        } else {
            // Pausert: grå bakgrunn og "pause" ikon
            views.setTextViewText(R.id.widget_mini_icon, appContext.getString(R.string.widget_mini_icon_paused))
            views.setInt(R.id.widget_mini_background, "setBackgroundResource", R.drawable.widget_mini_background_inactive)
        }

        // Sett opp klikk-handling for å toggel videresending
        val pendingIntent = WidgetHelper.createTogglePendingIntent(appContext, ForwardingWidgetMini::class.java, ACTION_TOGGLE, REQUEST_CODE)
        views.setOnClickPendingIntent(R.id.widget_mini_background, pendingIntent)

        // Send oppdatering til widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}