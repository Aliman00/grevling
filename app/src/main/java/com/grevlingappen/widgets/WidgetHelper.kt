package com.grevlingappen.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import com.grevlingappen.MainActivity
import com.grevlingappen.R
import com.grevlingappen.data.PreferenceKeys
import com.grevlingappen.utils.EncryptedPrefsFactory
import com.grevlingappen.utils.Logger
import java.util.UUID

/**
 * WidgetHelper - Objekt med delt funksjonalitet for alle widgets.
 * 
 * Funksjonalitet:
 * - Sjekker og toggler videresendings-status
 * - Genererer og validerer sikkerhetstokens for widget-handlinger
 * - Henter status-ressurser (ikoner, farger, tekster)
 * - Oppretter PendingIntents for ulike klikk-handlinger
 * - Sender oppdaterings-broadcasts til alle widget-instanser
 * 
 * Sikkerhet:
 * - Hver widget-handling inneholder et unikt token
 * - Token validertes før handling utføres for å forhindre uautoriserte toggles
 */
object WidgetHelper {
    private const val TAG = "WidgetHelper"
    // Nøkkel for token-ekstra i intents
    private const val EXTRA_TOKEN = "toggle_token"

    /**
     * Data class som holder visuelle ressurser for en gitt status.
     * Brukes av widgets for å一致visualisere aktiv/pausert tilstand.
     */
    data class StatusResources(
        val iconRes: Int,    // Ikon for statusindikator
        val colorRes: Int,   // Farge for tekst
        val textRes: Int    // String resource for status-tekst
    )

    /**
     * Sjekker om videresending er aktivert.
     * 
     * @param context App-kontekst
     * @return true hvis videresending er på
     */
    fun isForwardingEnabled(context: Context): Boolean {
        val prefs = EncryptedPrefsFactory.get(context.applicationContext)
        return prefs.getBoolean(PreferenceKeys.ENABLED, false)
    }

    /**
     * Henter visuelle ressurser basert på videresendings-status.
     * 
     * @param isEnabled Om videresending er aktivert
     * @return StatusResources med ikoner, farger og tekster
     */
    fun getStatusResources(isEnabled: Boolean): StatusResources {
        return if (isEnabled) {
            StatusResources(
                iconRes = R.drawable.widget_status_dot_active,
                colorRes = R.color.widget_active,
                textRes = R.string.widget_status_active
            )
        } else {
            StatusResources(
                iconRes = R.drawable.widget_status_dot_inactive,
                colorRes = R.color.widget_inactive,
                textRes = R.string.widget_status_paused
            )
        }
    }

    /**
     * Oppretter en PendingIntent som åpner hovedappen.
     * 
     * Brukes når brukeren klikker på widget for å åpne appen.
     * 
     * @param context App-kontekst
     * @param requestCode Unik kode for denne pending intent
     * @return PendingIntent for å åpne MainActivity
     */
    fun createOpenAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val appContext = context.applicationContext
        val intent = Intent(appContext, MainActivity::class.java).apply {
            // NEW_TASK: ny task hvis ingen eksisterer
            // CLEAR_TOP: fjern activities mellom
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Veksler videresendings-status og oppdaterer alle widgets.
     * 
     * @param context App-kontekst
     * @param callerTag Tag for logging (hvilken widget som trigget)
     */
    fun toggleForwarding(context: Context, callerTag: String) {
        val appContext = context.applicationContext
        val prefs = EncryptedPrefsFactory.get(appContext)
        val currentState = prefs.getBoolean(PreferenceKeys.ENABLED, false)
        val newState = !currentState
        
        // Toggle tilstanden
        prefs.edit { putBoolean(PreferenceKeys.ENABLED, newState) }
        Logger.d(TAG, "$callerTag triggeret toggle: $newState")

        // Oppdater alle widgets for å vise ny tilstand
        updateAllWidgets(appContext)
    }

    /**
     * Sender oppdaterings-broadcast til alle aktive widget-instanser.
     * 
     * Kalles etter status-endringer for å oppdatere visningen.
     * 
     * @param context App-kontekst
     */
    fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        // Oppdater alle tre widget-typer
        listOf(
            ForwardingWidget::class.java,
            ForwardingWidgetMini::class.java,
            StatsWidget::class.java
        ).forEach { updateAllWidgetsOfType(appContext, it) }
    }

    /**
     * Sender oppdatering til alle instanser av en spesifikk widget-type.
     * 
     * @param context App-kontekst
     * @param widgetClass Widget-klassen som skal oppdateres
     */
    fun <T : AppWidgetProvider> updateAllWidgetsOfType(context: Context, widgetClass: Class<T>) {
        val appContext = context.applicationContext
        val widgetManager = AppWidgetManager.getInstance(appContext)
        
        // Finn alle instanser av denne widget-typen
        val ids = widgetManager.getAppWidgetIds(ComponentName(appContext, widgetClass))
        
        if (ids.isNotEmpty()) {
            // Send broadcast til widget-providern
            val intent = Intent(appContext, widgetClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            appContext.sendBroadcast(intent)
        }
    }

    /**
     * Henter eksisterende token eller genererer nytt.
     * 
     * Token brukes for å sikre at kun våre egne widgets kan trigge handlinger.
     * Token lagres i preferences og beholdes mellom app-start.
     * 
     * @param context App-kontekst
     * @return Unik token-streng
     */
    fun getOrCreateToken(context: Context): String {
        val prefs = EncryptedPrefsFactory.get(context.applicationContext)
        return prefs.getString(PreferenceKeys.WIDGET_TOKEN, null) ?: run {
            // Generer ny UUID hvis ingen token finnes
            val newToken = UUID.randomUUID().toString()
            prefs.edit { putString(PreferenceKeys.WIDGET_TOKEN, newToken) }
            newToken
        }
    }

    /**
     * Validerer at en intent inneholder gyldig sikkerhetstoken.
     * 
     * Forhindrer at andre apper eller uautoriserte kilder kan trigge widgets.
     * 
     * @param context App-kontekst
     * @param Intent å validere
     * @return true hvis token er gyldig
     */
    fun isValidToken(context: Context, intent: Intent): Boolean {
        val intentToken = intent.getStringExtra(EXTRA_TOKEN) ?: return false
        val storedToken = EncryptedPrefsFactory.get(context.applicationContext)
            .getString(PreferenceKeys.WIDGET_TOKEN, null)
        return storedToken != null && intentToken == storedToken
    }

    /**
     * Oppretter en PendingIntent for widget-klikk som toggler videresending.
     * 
     * Inkluderer sikkerhetstoken i intenten.
     * 
     * @param context App-kontekst
     * @param widgetClass Widget-klassen som håndterer klikket
     * @param action Action-streng for handlingen
     * @param requestCode Unik kode for denne pending intent
     * @return PendingIntent med token
     */
    fun <T : AppWidgetProvider> createTogglePendingIntent(
        context: Context,
        widgetClass: Class<T>,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val appContext = context.applicationContext
        // Hent eller lag token
        val token = getOrCreateToken(appContext)
        val intent = Intent(appContext, widgetClass).apply {
            this.action = action
            putExtra(EXTRA_TOKEN, token)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}