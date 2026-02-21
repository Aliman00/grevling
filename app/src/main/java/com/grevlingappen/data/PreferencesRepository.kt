package com.grevlingappen.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.grevlingappen.R
import com.grevlingappen.domain.models.ForwardingState
import com.grevlingappen.services.NotificationMonitorService
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
 * Bruker StateFlow for å tilby reaktive oppdateringer til UI-laget.
 * 
 * Implementert som singleton for å unngå duplisering av coroutine scopes og listeners.
 */
class PreferencesRepository private constructor(context: Context) {

    companion object {
        private const val TAG = "PreferencesRepository"
        
        @Volatile
        private var instance: PreferencesRepository? = null

        fun getInstance(context: Context): PreferencesRepository =
            instance ?: synchronized(this) {
                instance ?: PreferencesRepository(context.applicationContext).also { instance = it }
            }
    }

    private val appContext = context.applicationContext
    
    // Lazy initialization for å unngå nøkkelgenerering på hovedtråden
    private val prefs: SharedPreferences by lazy {
        EncryptedPrefsFactory.get(appContext)
    }

    // Eget scope for repository-oppgaver (lever så lenge appen lever)
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow(ForwardingState())
    val state: StateFlow<ForwardingState> = _state.asStateFlow()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        refreshState()
    }

    init {
        // Initialiser og registrer lytter asynkront
        repositoryScope.launch(Dispatchers.IO) {
            prefs.registerOnSharedPreferenceChangeListener(prefsListener)
            refreshState() 
        }
    }

    /**
     * Rydd opp ressurser når repository ikke lenger trengs.
     * MERK: Denne metoden skal IKKE kalles fra ViewModels lenger.
     * Singletonen lever hele appens levetid og ryddes opp av OS ved prosess-død.
     * Beholdes kun for testing og eventuell manuell opprydding.
     */
    @VisibleForTesting
    fun onDestroy() {
        repositoryScope.cancel()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun loadCurrentState(): ForwardingState {
        val gmailAddress = prefs.getString(PreferenceKeys.GMAIL_ADDRESS, "") ?: ""
        val gmailPassword = prefs.getString(PreferenceKeys.GMAIL_PASSWORD, "") ?: ""
        val recipientEmail = prefs.getString(PreferenceKeys.RECIPIENT_EMAIL, "") ?: ""

        val hasEmailConfig = gmailAddress.isNotBlank() && gmailPassword.isNotBlank() && recipientEmail.isNotBlank()
        val hasNotificationAccess = PermissionsHelper.isNotificationServiceEnabled(appContext)
        val isEnabled = prefs.getBoolean(PreferenceKeys.ENABLED, false)

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

    fun toggleForwarding() {
        val newValue = !prefs.getBoolean(PreferenceKeys.ENABLED, false)
        prefs.edit { putBoolean(PreferenceKeys.ENABLED, newValue) }
        Logger.d(TAG, "Forwarding toggled: $newValue")
    }

    fun setGmailAddress(address: String) = prefs.edit { putString(PreferenceKeys.GMAIL_ADDRESS, address.trim()) }
    fun setGmailPassword(password: String) = prefs.edit { putString(PreferenceKeys.GMAIL_PASSWORD, password.trim()) }
    fun setRecipientEmail(email: String) = prefs.edit { putString(PreferenceKeys.RECIPIENT_EMAIL, email.trim()) }

    fun getGmailAddress(): String = prefs.getString(PreferenceKeys.GMAIL_ADDRESS, "") ?: ""
    fun getRecipientEmail(): String = prefs.getString(PreferenceKeys.RECIPIENT_EMAIL, "") ?: ""
    fun getGmailPassword(): String = prefs.getString(PreferenceKeys.GMAIL_PASSWORD, "") ?: ""

    fun setAutoReplyEnabled(enabled: Boolean) = prefs.edit { putBoolean(PreferenceKeys.AUTO_REPLY_ENABLED, enabled) }
    fun setUseSameMessage(useSame: Boolean) = prefs.edit { putBoolean(PreferenceKeys.USE_SAME_MESSAGE, useSame) }
    fun setUnifiedMessage(message: String) = prefs.edit { putString(PreferenceKeys.UNIFIED_REPLY_MESSAGE, message) }
    fun setSmsMessage(message: String) = prefs.edit { putString(PreferenceKeys.SMS_REPLY_MESSAGE, message) }
    fun setCallMessage(message: String) = prefs.edit { putString(PreferenceKeys.CALL_REPLY_MESSAGE, message) }

    fun getMonitoredApps(): Set<String> = prefs.getStringSet(PreferenceKeys.MONITORED_APPS, emptySet()) ?: emptySet()

    fun setMonitoredApps(packageNames: Set<String>, context: Context? = null) {
        prefs.edit { putStringSet(PreferenceKeys.MONITORED_APPS, packageNames) }

        context?.let { ctx ->
            try {
                val intent = Intent(ctx, NotificationMonitorService::class.java)
                ctx.stopService(intent)
                ctx.startService(intent)
            } catch (e: Exception) {
                Logger.d(TAG, "Service restart skipped: ${e.message}")
            }
        }
    }

    fun toggleApp(packageName: String, context: Context? = null) {
        val currentApps = getMonitoredApps().toMutableSet()
        if (currentApps.contains(packageName)) currentApps.remove(packageName) else currentApps.add(packageName)
        setMonitoredApps(currentApps, context)
    }

    /**
     * Oppdaterer appens tilstand asynkront ved å lese fra kryptert lagring på en bakgrunnstråd.
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
