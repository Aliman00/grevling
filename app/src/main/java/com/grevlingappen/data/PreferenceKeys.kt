package com.grevlingappen.data

/**
 * PreferenceKeys - Sentralisert samling av alle SharedPreferences-nøkler.
 * Forhindrer skrivefeil og sikrer konsistens på tvers av appen og widgets.
 */
object PreferenceKeys {
    // Filnavn
    const val PREFS_NAME = "grevling_prefs"

    // Hovedinnstillinger
    const val ENABLED = "enabled"
    const val GMAIL_ADDRESS = "gmail_address"
    const val GMAIL_PASSWORD = "gmail_password"
    const val RECIPIENT_EMAIL = "recipient_email"

    // Auto-svar innstillinger
    const val AUTO_REPLY_ENABLED = "auto_reply_enabled"
    const val USE_SAME_MESSAGE = "use_same_message"
    const val UNIFIED_REPLY_MESSAGE = "unified_reply_message"
    const val SMS_REPLY_MESSAGE = "sms_reply_message"
    const val CALL_REPLY_MESSAGE = "call_reply_message"

    // App-overvåking
    const val MONITORED_APPS = "monitored_apps"

    // Widgets
    const val WIDGET_TOKEN = "widget_toggle_token"
}