package com.grevlingappen.data

/**
 * PreferenceKeys - Sentralisert samling av alle SharedPreferences-nøkler.
 * 
 * Alle nøkler for persistent lagring er definert her.
 * Forhindrer skrivefeil og sikrer konsistens på tvers av appen og widgets.
 */
object PreferenceKeys {
    // ==================== FILNAVN ====================
    
    /** Filnavn for hovedinnstillinger (brukes av EncryptedPrefsFactory) */
    const val PREFS_NAME = "grevling_prefs"

    // ==================== HOVEDINNSTILLINGER ====================
    
    /** Om videresending er aktivert (boolean) */
    const val ENABLED = "enabled"
    /** Gmail-adressen som brukes til å sende e-post (string) */
    const val GMAIL_ADDRESS = "gmail_address"
    /** Gmail-app-passord (string, kryptert) */
    const val GMAIL_PASSWORD = "gmail_password"
    /** E-postadressen det videresendes til (string) */
    const val RECIPIENT_EMAIL = "recipient_email"

    // ==================== AUTO-SVAR ====================
    
    /** Om auto-svar er aktivert (boolean) */
    const val AUTO_REPLY_ENABLED = "auto_reply_enabled"
    /** Om samme melding brukes for SMS og anrop (boolean) */
    const val USE_SAME_MESSAGE = "use_same_message"
    /** Enhetlig auto-svar melding (string) */
    const val UNIFIED_REPLY_MESSAGE = "unified_reply_message"
    /** Auto-svar melding kun for SMS (string) */
    const val SMS_REPLY_MESSAGE = "sms_reply_message"
    /** Auto-svar melding kun for anrop (string) */
    const val CALL_REPLY_MESSAGE = "call_reply_message"

    // ==================== APP-OVERVÅKING ====================
    
    /** Sett med package names for overvåkede apper (StringSet) */
    const val MONITORED_APPS = "monitored_apps"

    // ==================== WIDGETS ====================
    
    /** Sikkerhetstoken for widget-toggle-handlinger (string) */
    const val WIDGET_TOKEN = "widget_toggle_token"
}