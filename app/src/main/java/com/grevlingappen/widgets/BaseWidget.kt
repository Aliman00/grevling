package com.grevlingappen.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.grevlingappen.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BaseWidget - Abstrakt klasse som håndterer felles logikk for alle app-widgets.
 * Bruker Coroutines og goAsync() for å garantere at asynkront arbeid fullføres.
 */
abstract class BaseWidget : AppWidgetProvider() {

    protected abstract val toggleAction: String
    protected abstract val tag: String

    companion object {
        // Felles scope for alle widgets for å spare ressurser
        private val widgetScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    /** 
     * Oppdaterer én widget-instans asynkront.
     */
    protected abstract suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    )

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                appWidgetIds.forEach { id ->
                    updateWidget(context, appWidgetManager, id)
                }
            } catch (e: Exception) {
                Logger.e(tag, "Feil ved oppdatering av widget-liste", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != toggleAction) return

        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                if (WidgetHelper.isValidToken(context, intent)) {
                    WidgetHelper.toggleForwarding(context, tag)
                } else {
                    Logger.w(tag, "Ugyldig widget-token, ignorerer toggle")
                }
            } catch (e: Exception) {
                Logger.e(tag, "Feil ved mottak av widget-handling", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
