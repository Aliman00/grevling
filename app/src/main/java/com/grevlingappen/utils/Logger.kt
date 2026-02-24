package com.grevlingappen.utils

import android.util.Log
import com.grevlingappen.BuildConfig

/**
 * Logger - Sentralisert wrapper rundt Android Log.
 */
object Logger {
    private val isDebug: Boolean = BuildConfig.DEBUG

    /** Debug: Kun for utvikling, fjernes i release. */
    fun d(tag: String, message: String) {
        if (isDebug) Log.d(tag, message)
    }

    /** Info: Viktige hendelser i utvikling, fjernes i release. */
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

    /** Error med unntak: Vises alltid med stacktrace for å lette feilsøking. */
    fun e(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }
}