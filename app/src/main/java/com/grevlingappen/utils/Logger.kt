package com.grevlingappen.utils

import android.util.Log
import com.grevlingappen.BuildConfig

/**
 * Logger - Sentralisert wrapper rundt Android Log.
 * 
 * Forenkler logging ved å:
 * - Kunne skru av debug-logg i release bygg
 * - Ha konsistent tag-bruk
 * - Enklere syntaks
 */
object Logger {
    // Bestemmes av BuildConfig - true i debug, false i release
    private val isDebug: Boolean = BuildConfig.DEBUG

    /** Debug-logg: Kun for utvikling, skrus av i release. */
    fun d(tag: String, message: String) {
        if (isDebug) Log.d(tag, message)
    }

    /** Info-logg: Viktige hendelser i utvikling, skrus av i release. */
    fun i(tag: String, message: String) {
        if (isDebug) Log.i(tag, message)
    }

    /** Warning: Potensielle problemer, logges alltid. */
    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    /** Warning med unntak: Logges alltid, viser stacktrace kun i debug. */
    fun w(tag: String, message: String, throwable: Throwable) {
        if (isDebug) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }

    /** Error: Kritiske feil, logges alltid. */
    fun e(tag: String, message: String) {
        Log.e(tag, message)
    }

    /** Error med unntak: Vises alltid med stacktrace for feilsøking. */
    fun e(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }
}