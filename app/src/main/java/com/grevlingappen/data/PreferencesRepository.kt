package com.grevlingappen.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.grevlingappen.R
import com.grevlingappen.domain.models.ForwardingState
import com.grevlingappen.utils.EncryptedPrefsFactory
import com.grevlingappen.utils.Logger
import com.grevlingappen.utils.PermissionsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PreferencesRepository - Single Source of Truth for applikasjonens innstillinger.
 * 
 * Funksjonalitet:
 * - Holder alle brukerinnstillinger på ett sted
 * - Tilbyr reaktive StateFlow-observatører for UI-oppdateringer
 * - Sjekker systemtillatelser (Notification Access, etc.)
 * - Genererer brukervennlige statusmeldinger
 * 
 * Implementert som singleton for å sikre at alle deler av appen
 * bruker samme data og unngå duplisering av listeners.
 */
class PreferencesRepository private constructor(context: Context) {

    companion object {
        private const val TAG = "PreferencesRepository"
        
        // Singleton-instans med trådsikker initialisering
        @Volatile
        private var instance: PreferencesRepository? = null

        /**
         * Henter singleton-instansen av repositoryet.
         * Oppretter instansen ved første kall hvis den ikke eksisterer.
         */
        fun getInstance(context: Context): PreferencesRepository =
            instance ?: synchronized(this) {
                instance ?: PreferencesRepository(context.applicationContext).also { instance = it }
            }
    }

    // Application context for å unngå memory leaks
    private val appContext = context.applicationContext
    
    // Lazy-initialisert kryptert SharedPreferences
    private val prefs: SharedPreferences by lazy {
        EncryptedPrefsFactory.get(appContext)
    }

    // Coroutine-scope for asynkrone oppgaver i repositoryet
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Reaktiv state som UI kan observere
    private val _state = MutableStateFlow(ForwardingState())
    val state: StateFlow<ForwardingState> = _state.asStateFlow()

    // Lytter som reagerer på endringer i SharedPreferences
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        refreshState()
    }

    init {
        // Registrer preferences-lytter og last initial state asynkront
        repositoryScope.launch(Dispatchers.IO) {
            prefs.registerOnSharedPreferenceChangeListener(prefsListener)
            refreshState() 
        }
    }

    /**
     * Rydder opp i ressurser ved nedstenging.
     * MERK: Denne metoden skal normalt IKKE kalles - singletonen lever
     * så lenge appen lever og ryddes opp av OS ved prosess-død.
     * Beholdes kun for testing og eventuell manuell opprydding.
     */
    @VisibleForTesting
    fun onDestroy() {
        repositoryScope.cancel()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    /**
     * Laster gjeldende tilstand fra SharedPreferences.
     * Sjekker også systemtillatelser og genererer statusmelding.
     * 
     * @return ForwardingState med alle gjeldende innstillinger
     */
    private fun loadCurrentState(): ForwardingState {
        // Hent e-postkonfigurasjon
        val gmailAddress = prefs.getString(PreferenceKeys.GMAIL_ADDRESS, "") ?: ""
        val gmailPassword = prefs.getString(PreferenceKeys.GMAIL_PASSWORD, "") ?: ""
        val recipientEmail = prefs.getString(PreferenceKeys.RECIPIENT_EMAIL, "") ?: ""

        // Sjekk om all nødvendig konfigurasjon er på plass
        val hasEmailConfig = gmailAddress.isNotBlank() && gmailPassword.isNotBlank() && recipientEmail.isNotBlank()
        val hasNotificationAccess = PermissionsHelper.isNotificationServiceEnabled(appContext)
        val isEnabled = prefs.getBoolean(PreferenceKeys.ENABLED, false)

        // Generer brukervennlig statusmelding basert på tilstand
        val statusMessage = when {
            !hasNotificationAccess -> appContext.getString(R.string.status_needs_notification)
            !hasEmailConfig -> appContext.getString(R.string.status_missing_config)
            !isEnabled -> appContext.getString(R.string.status_info_paused)
            else -> {
                val base = appContext.getString(R.string.status_label_forwards_to)
                if (recipientEmail.isNotBlank()) "$base: $recipientEmail" else base
            }
        }

        return ForwardingState(
            isEnabled = isEnabled,
            hasNotificationAccess = hasNotificationAccess,
            hasEmailConfig = hasEmailConfig,
            recipientEmail = recipientEmail,
            gmailAddress = gmailAddress,
            hasGmailPassword = gmailPassword.isNotEmpty(),
            autoReplyEnabled = prefs.getBoolean(PreferenceKeys.AUTO_REPLY_ENABLED, false),
            useSameMessage = prefs.getBoolean(PreferenceKeys.USE_SAME_MESSAGE, true),
            unifiedMessage = prefs.getString(PreferenceKeys.UNIFIED_REPLY_MESSAGE, null)
                ?: appContext.getString(R.string.default_unified_message),
            smsMessage = prefs.getString(PreferenceKeys.SMS_REPLY_MESSAGE, null)
                ?: appContext.getString(R.string.default_sms_message),
            callMessage = prefs.getString(PreferenceKeys.CALL_REPLY_MESSAGE, null)
                ?: appContext.getString(R.string.default_call_message),
            statusMessage = statusMessage
        )
    }

    /**
     * Veksler mellom aktivert/deaktivert videresending.
     */
    fun toggleForwarding() {
        val newValue = !prefs.getBoolean(PreferenceKeys.ENABLED, false)
        prefs.edit { putBoolean(PreferenceKeys.ENABLED, newValue) }
        Logger.d(TAG, "Videresending togglet til: $newValue")
    }

    // --- Settere for e-postkonfigurasjon ---
    
    fun setGmailAddress(address: String) = prefs.edit { putString(PreferenceKeys.GMAIL_ADDRESS, address.trim()) }
    fun setGmailPassword(password: String) = prefs.edit { putString(PreferenceKeys.GMAIL_PASSWORD, password.trim()) }
    fun setRecipientEmail(email: String) = prefs.edit { putString(PreferenceKeys.RECIPIENT_EMAIL, email.trim()) }

    // --- Gettere for e-postkonfigurasjon ---
    
    fun getGmailAddress(): String = prefs.getString(PreferenceKeys.GMAIL_ADDRESS, "") ?: ""
    fun getRecipientEmail(): String = prefs.getString(PreferenceKeys.RECIPIENT_EMAIL, "") ?: ""
    fun getGmailPassword(): String = prefs.getString(PreferenceKeys.GMAIL_PASSWORD, "") ?: ""

    // --- Settere for auto-svar innstillinger ---
    
    fun setAutoReplyEnabled(enabled: Boolean) = prefs.edit { putBoolean(PreferenceKeys.AUTO_REPLY_ENABLED, enabled) }
    fun setUseSameMessage(useSame: Boolean) = prefs.edit { putBoolean(PreferenceKeys.USE_SAME_MESSAGE, useSame) }
    fun setUnifiedMessage(message: String) = prefs.edit { putString(PreferenceKeys.UNIFIED_REPLY_MESSAGE, message) }
    fun setSmsMessage(message: String) = prefs.edit { putString(PreferenceKeys.SMS_REPLY_MESSAGE, message) }
    fun setCallMessage(message: String) = prefs.edit { putString(PreferenceKeys.CALL_REPLY_MESSAGE, message) }

    // --- App-overvåking ---
    
    /**
     * Henter sett med package names for overvåkede apper.
     */
    fun getMonitoredApps(): Set<String> = prefs.getStringSet(PreferenceKeys.MONITORED_APPS, emptySet()) ?: emptySet()

    /**
     * Setter hvilke apper som skal overvåkes.
     */
    fun setMonitoredApps(packageNames: Set<String>) {
        prefs.edit { putStringSet(PreferenceKeys.MONITORED_APPS, packageNames) }
    }

    /**
     * Veksler overvåking av én app.
     * Legger til hvis ikke finnes, fjerner hvis den finnes.
     */
    fun toggleApp(packageName: String) {
        val currentApps = getMonitoredApps().toMutableSet()
        if (currentApps.contains(packageName)) currentApps.remove(packageName) else currentApps.add(packageName)
        setMonitoredApps(currentApps)
    }

    /**
     * Oppdaterer tilstanden asynkront ved å lese fra SharedPreferences.
     * Kalles automatisk ved preferance-endringer og ved behov fra UI.
     */
    fun refreshState() {
        repositoryScope.launch {
            val newState = withContext(Dispatchers.IO) {
                loadCurrentState()
            }
            _state.value = newState
        }
    }
}