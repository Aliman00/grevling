package com.smsforwarder

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

/**
 * Abstrakt base-klasse for alle widgets.
 * Samler felles onUpdate-løkke og onReceive-validering
 * for å unngå kodeduplisering mellom widget-implementasjoner.
 */
abstract class BaseWidget : AppWidgetProvider() {

    /** Action-strengen som trigger toggle (f.eks. "com.smsforwarder.ACTION_TOGGLE_FORWARDING") */
    protected abstract val toggleAction: String

    /** Logg-tag for denne widget-typen */
    protected abstract val tag: String

    /** Oppdaterer én widget-instans med riktig layout og data */
    protected abstract fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    )

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
        if (intent.action == toggleAction) {
            // Verifiser at intent inneholder gyldig token (hindrer ekstern toggling)
            if (WidgetHelper.isValidToken(context, intent)) {
                WidgetHelper.toggleForwarding(context, tag)
            } else {
                Logger.w(tag, "Ugyldig eller manglende widget-token, ignorerer toggle")
            }
        }
    }
}
