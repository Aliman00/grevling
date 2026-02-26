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
 * BaseWidget - Abstrakt basisklasse som håndterer felles logikk for alle app-widgets.
 * 
 * Funksjonalitet:
 * - Håndterer widget-oppdatering asynkront via coroutines
 * - Validerer sikkerhetstokens ved toggle-handlinger
 * - Bruker goAsync() for å sikre at async-arbeid fullføres
 * - Inneholder felles scope for alle widgets for ressurseffektivitet
 * 
 * Arve fra denne klassen for å lage nye widget-typer.
 */
abstract class BaseWidget : AppWidgetProvider() {

    /**
     * Action-streng som identifiserer toggle-handlinger for denne widgeten.
     * Må implementeres av subklasser.
     */
    protected abstract val toggleAction: String
    
    /**
     * Tag for logging - brukes til å identifisere widgeten i loggene.
     * Må implementeres av subklasser.
     */
    protected abstract val tag: String

    companion object {
        // Delt coroutine-scope for alle widget-instanser
        // Gjenbrukes for å spare ressurser
        private val widgetScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    /**
     * Oppdaterer én widget-instans asynkront.
     * Må implementeres av subklasser med spesifikk oppdateringslogikk.
     * 
     * @param context App-kontekst
     * @param appWidgetManager WidgetManager for oppdateringer
     * @param appWidgetId ID til spesifikk widget-instans
     */
    protected abstract suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    )

    /**
     * Kjøres periodisk for å oppdatere alle instanser av denne widgeten.
     * Bruker goAsync() for å holde broadcast alive under async-oppdatering.
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                // Oppdater hver widget-instans
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

    /**
     * Mottar broadcasts (f.eks. toggle-handlinger fra widgets).
     * Validerer sikkerhetstoken før handling utføres.
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // Kun håndtere vår egen toggle-action
        if (intent.action != toggleAction) return

        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                // Valider sikkerhetstoken før vi utfører handling
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