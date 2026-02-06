package com.smsforwarder

import android.util.Log

/**
 * Logger wrapper for strukturert logging.
 * Sentralisert logging gjør det enkelt å endre logging-behavior globalt.
 * Debug-logging er kun aktiv i debug builds for bedre ytelse og sikkerhet.
 */
object Logger {

    // BuildConfig.DEBUG er en compile-time constant og overlever R8
    private val isDebug: Boolean = BuildConfig.DEBUG

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
        if (isDebug) {
            // Debug: logg med full stacktrace
            Log.e(tag, message, throwable)
        } else {
            // Release: kun meldingen, ingen throwable-detaljer (kan inneholde sensitiv info)
            Log.e(tag, message)
        }
    }
}
