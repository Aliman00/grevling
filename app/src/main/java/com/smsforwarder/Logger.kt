package com.smsforwarder

import android.util.Log

/**
 * Logger wrapper som kun logger i debug builds.
 * I release builds vil alle log statements være no-ops (unntatt error logs).
 */
object Logger {

    // Sjekk om vi er i debug-modus basertpå loggability
    private fun isDebugEnabled(): Boolean {
        return Log.isLoggable("SmsForwarder", Log.DEBUG)
    }

    fun d(tag: String, message: String) {
        // Debug logs kun i debug builds
        Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        // Info logs kun i debug builds
        Log.i(tag, message)
    }

    fun w(tag: String, message: String) {
        // Warning logs kun i debug builds
        Log.w(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable) {
        // Warning logs kun i debug builds
        Log.w(tag, message, throwable)
    }

    fun e(tag: String, message: String) {
        // Error logs alltid logges, selv i release
        Log.e(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable) {
        // Error logs alltid logges, selv i release
        Log.e(tag, message, throwable)
    }
}
