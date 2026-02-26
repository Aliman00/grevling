package com.grevlingappen.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.grevlingappen.R
import com.grevlingappen.widgets.WidgetHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ForwardingStats - Håndterer dagsstatistikk for videresendte meldinger og anrop.
 * 
 * Funksjonalitet:
 * - Teller antall SMS og anrop videresendt i dag
 * - Nullstiller tellere automatisk ved ny dag
 * - Holder styr på tidspunkt for siste videresending
 * - Oppdaterer widgets ved nye hendelser
 * 
 * Alle funksjoner er suspend og kjører på IO-dispatcher for async-operasjoner.
 */
object ForwardingStats {
    private const val PREFS_NAME = "forwarding_stats"
    private const val KEY_SMS_COUNT_TODAY = "sms_count_today"
    private const val KEY_CALLS_COUNT_TODAY = "calls_count_today"
    private const val KEY_LAST_FORWARDED_TIME = "last_forwarded_time"
    private const val KEY_STATS_DATE = "stats_date"

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // Lock for synkronisering av data mellom tråder
    private val lock = Any()

    // Cacher dagens dato for å unngå unødvendige kall
    @Volatile
    private var cachedDate: String? = null

    /**
     * Henter SharedPreferences for statistikk.
     * Bruker egen prefs-fil atskilt fra hovedinnstillinger.
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Registrerer en videresendt SMS.
     * Øker telleren med 1 og oppdaterer widgets.
     */
    suspend fun recordSmsForwarded(context: Context) = withContext(Dispatchers.IO) {
        recordEvent(context, KEY_SMS_COUNT_TODAY)
    }

    /**
     * Registrerer et videresendt tapte anrop.
     * Øker telleren med 1 og oppdaterer widgets.
     */
    suspend fun recordCallForwarded(context: Context) = withContext(Dispatchers.IO) {
        recordEvent(context, KEY_CALLS_COUNT_TODAY)
    }

    /**
     * Interne: Registrerer en videresendingshendelse.
     * Sjekker for ny dag, øker telleren, og oppdaterer widgets.
     */
    private fun recordEvent(context: Context, key: String) {
        synchronized(lock) {
            val appContext = context.applicationContext
            resetIfNewDayInternal(appContext)
            val prefs = getPrefs(appContext)
            val currentCount = prefs.getInt(key, 0)

            prefs.edit {
                putInt(key, currentCount + 1)
                putLong(KEY_LAST_FORWARDED_TIME, System.currentTimeMillis())
            }

            // Oppdater widgets for å vise ny statistikk
            updateAllWidgets(appContext)
        }
    }

    /**
     * Interne: Nullstiller tellere hvis det er ny dag.
     * Bruker cached verdi for å unngå unødvendige prefs-lesinger.
     */
    private fun resetIfNewDayInternal(context: Context) {
        val today = LocalDate.now().format(dateFormatter)
        if (cachedDate == today) return

        val prefs = getPrefs(context)
        if (prefs.getString(KEY_STATS_DATE, null) != today) {
            prefs.edit {
                putString(KEY_STATS_DATE, today)
                putInt(KEY_SMS_COUNT_TODAY, 0)
                putInt(KEY_CALLS_COUNT_TODAY, 0)
            }
        }
        cachedDate = today
    }

    /**
     * Henter antall SMS videresendt i dag.
     * @return Antall SMS som videresendt i dag
     */
    suspend fun getSmsCountToday(context: Context): Int = withContext(Dispatchers.IO) {
        synchronized(lock) {
            resetIfNewDayInternal(context)
            getPrefs(context).getInt(KEY_SMS_COUNT_TODAY, 0)
        }
    }

    /**
     * Henter antall anrop videresendt i dag.
     * @return Antall anrop som videresendt i dag
     */
    suspend fun getCallsCountToday(context: Context): Int = withContext(Dispatchers.IO) {
        synchronized(lock) {
            resetIfNewDayInternal(context)
            getPrefs(context).getInt(KEY_CALLS_COUNT_TODAY, 0)
        }
    }

    /**
     * Henter totalt antall hendelser (SMS + anrop) videresendt i dag.
     * @return Totalt antall videresendinger i dag
     */
    suspend fun getTotalCountToday(context: Context): Int = withContext(Dispatchers.IO) {
        synchronized(lock) {
            resetIfNewDayInternal(context)
            getPrefs(context).getInt(KEY_SMS_COUNT_TODAY, 0) +
            getPrefs(context).getInt(KEY_CALLS_COUNT_TODAY, 0)
        }
    }

    /**
     * Henter tidspunkt for siste videresending som lesbar tekst.
     * @return f.eks. "Akurat nå", "5 minutter siden", "2 timer siden"
     */
    suspend fun getLastForwardedTimeAgo(context: Context): String = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val lastTime = getPrefs(context).getLong(KEY_LAST_FORWARDED_TIME, 0)
            if (lastTime == 0L) return@withContext context.getString(R.string.stats_time_never)

            val diffMinutes = (System.currentTimeMillis() - lastTime) / 1000 / 60
            
            when {
                diffMinutes < 1 -> context.getString(R.string.stats_time_just_now)
                diffMinutes < 60 -> context.getString(R.string.stats_time_minutes_ago, diffMinutes.toInt())
                diffMinutes < 1440 -> context.getString(R.string.stats_time_hours_ago, (diffMinutes / 60).toInt())
                else -> context.getString(R.string.stats_time_days_ago, (diffMinutes / 1440).toInt())
            }
        }
    }

    /**
     * Interne: Oppdaterer alle widgets for å vise ny statistikk.
     */
    private fun updateAllWidgets(context: Context) {
        WidgetHelper.updateAllWidgets(context)
    }
}