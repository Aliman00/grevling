package com.smsforwarder

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Håndterer statistikk for videresendte SMS og anrop.
 * Lagrer daglig statistikk og tidspunkt for siste videresending.
 */
object ForwardingStats {

    private const val PREFS_NAME = "forwarding_stats"
    private const val KEY_SMS_COUNT_TODAY = "sms_count_today"
    private const val KEY_CALLS_COUNT_TODAY = "calls_count_today"
    private const val KEY_LAST_FORWARDED_TIME = "last_forwarded_time"
    private const val KEY_STATS_DATE = "stats_date"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * Sjekker om vi har byttet dag, og resetter statistikk hvis nødvendig.
     */
    private fun resetIfNewDay(context: Context) {
        val prefs = getPrefs(context)
        val savedDate = prefs.getString(KEY_STATS_DATE, null)
        val today = getTodayDateString()

        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_STATS_DATE, today)
                .putInt(KEY_SMS_COUNT_TODAY, 0)
                .putInt(KEY_CALLS_COUNT_TODAY, 0)
                .apply()
        }
    }

    /**
     * Registrerer en videresendt SMS.
     */
    fun recordSmsForwarded(context: Context) {
        resetIfNewDay(context)
        val prefs = getPrefs(context)
        val currentCount = prefs.getInt(KEY_SMS_COUNT_TODAY, 0)
        prefs.edit()
            .putInt(KEY_SMS_COUNT_TODAY, currentCount + 1)
            .putLong(KEY_LAST_FORWARDED_TIME, System.currentTimeMillis())
            .apply()
        
        // Oppdater widgets
        updateAllWidgets(context)
    }

    /**
     * Registrerer et videresendt tapt anrop.
     */
    fun recordCallForwarded(context: Context) {
        resetIfNewDay(context)
        val prefs = getPrefs(context)
        val currentCount = prefs.getInt(KEY_CALLS_COUNT_TODAY, 0)
        prefs.edit()
            .putInt(KEY_CALLS_COUNT_TODAY, currentCount + 1)
            .putLong(KEY_LAST_FORWARDED_TIME, System.currentTimeMillis())
            .apply()
        
        // Oppdater widgets
        updateAllWidgets(context)
    }

    /**
     * Henter antall videresendte SMS i dag.
     */
    fun getSmsCountToday(context: Context): Int {
        resetIfNewDay(context)
        return getPrefs(context).getInt(KEY_SMS_COUNT_TODAY, 0)
    }

    /**
     * Henter antall videresendte anrop i dag.
     */
    fun getCallsCountToday(context: Context): Int {
        resetIfNewDay(context)
        return getPrefs(context).getInt(KEY_CALLS_COUNT_TODAY, 0)
    }

    /**
     * Henter totalt antall videresendinger i dag.
     */
    fun getTotalCountToday(context: Context): Int {
        return getSmsCountToday(context) + getCallsCountToday(context)
    }

    /**
     * Henter tidspunkt for siste videresending som lesbar streng.
     */
    fun getLastForwardedTimeAgo(context: Context): String {
        val lastTime = getPrefs(context).getLong(KEY_LAST_FORWARDED_TIME, 0)
        
        if (lastTime == 0L) {
            return "Aldri"
        }

        val now = System.currentTimeMillis()
        val diffMs = now - lastTime
        val diffMinutes = diffMs / (1000 * 60)
        val diffHours = diffMs / (1000 * 60 * 60)

        return when {
            diffMinutes < 1 -> "Nå nettopp"
            diffMinutes < 60 -> "$diffMinutes min siden"
            diffHours < 24 -> "$diffHours timer siden"
            else -> {
                val diffDays = diffHours / 24
                "$diffDays dager siden"
            }
        }
    }

    private fun updateAllWidgets(context: Context) {
        ForwardingWidget.updateAllWidgets(context)
        ForwardingWidgetMini.updateAllWidgets(context)
        StatsWidget.updateAllWidgets(context)
    }
}
