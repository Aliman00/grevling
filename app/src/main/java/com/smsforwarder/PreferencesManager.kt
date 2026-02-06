package com.smsforwarder

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PreferencesManager {

    private const val TAG = "PreferencesManager"
    private const val PREFS_NAME = "secure_prefs"

    // Sentraliserte preference-nøkler — bruk disse overalt for å unngå typos
    const val KEY_ENABLED = "enabled"
    const val KEY_GMAIL_ADDRESS = "gmail_address"
    const val KEY_GMAIL_PASSWORD = "gmail_password"
    const val KEY_RECIPIENT_EMAIL = "email"
    const val KEY_AUTO_REPLY_ENABLED = "auto_reply_enabled"
    const val KEY_AUTO_REPLY_LOCKED = "auto_reply_locked"
    const val KEY_USE_SAME_MESSAGE = "use_same_message"
    const val KEY_UNIFIED_REPLY_MESSAGE = "unified_reply_message"
    const val KEY_SMS_REPLY_MESSAGE = "sms_reply_message"
    const val KEY_CALL_REPLY_MESSAGE = "call_reply_message"
    const val KEY_MONITORED_APPS = "monitored_apps"
    const val KEY_WIDGET_TOKEN = "widget_toggle_token"

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
            Log.e(TAG, "Kritisk: Kunne ikke opprette kryptert lagring", e)
            throw SecurityException(
                "Kunne ikke opprette sikker lagring. Gmail-legitimasjon kan ikke lagres trygt.", e
            )
        }
    }
}
