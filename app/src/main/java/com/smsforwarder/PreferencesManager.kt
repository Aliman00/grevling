package com.smsforwarder

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PreferencesManager {

    private const val TAG = "PreferencesManager"
    private const val PREFS_NAME = "secure_prefs"
    private const val FALLBACK_PREFS_NAME = "fallback_prefs"

    fun getEncryptedPreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Logging uten å bruke Logger for å unngå sirkulær avhengighet
            Log.e(TAG, "Krypteringsfeil, bruker fallback SharedPreferences", e)
            // Fallback til vanlige prefs - mindre sikkert, men appen fungerer
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
}
