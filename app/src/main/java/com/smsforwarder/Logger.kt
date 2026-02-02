package com.smsforwarder

import android.util.Log

/**
 * Logger wrapper for strukturert logging.
 * Sentralisert logging gjør det enkelt å endre logging-behavior globalt.
 * Debug-logging er kun aktiv i debug builds for bedre ytelse og sikkerhet.
 */
object Logger {

    // Sjekk om vi kjører i debug-modus (bruker appens BuildConfig)
    private val isDebug: Boolean by lazy {
        try {
            // Bruk reflection for å unngå compile-time avhengighet
            val buildConfigClass = Class.forName("com.smsforwarder.BuildConfig")
            val debugField = buildConfigClass.getField("DEBUG")
            debugField.getBoolean(null)
        } catch (e: Exception) {
            // Fallback til true hvis BuildConfig ikke er tilgjengelig
            true
        }
    }

    fun d(tag: String, message: String) {
        if (isDebug) {
            Log.d(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        if (isDebug) {
            Log.i(tag, message)
        }
    }

    fun w(tag: String, message: String) {
        // Warnings logges alltid
        Log.w(tag, message)
    }

    fun e(tag: String, message: String) {
        // Errors logges alltid
        Log.e(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable) {
        // Errors logges alltid
        Log.e(tag, message, throwable)
    }
}
