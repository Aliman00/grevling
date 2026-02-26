package com.grevlingappen.ui.screens.settings

import android.app.Application
import android.content.Intent
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grevlingappen.R
import com.grevlingappen.data.PreferencesRepository
import com.grevlingappen.utils.EmailSender
import com.grevlingappen.utils.Logger
import com.grevlingappen.utils.PermissionsHelper
import com.grevlingappen.utils.setupDebounceSaveWithFlush
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SettingsViewModel - ViewModel som håndterer business-logikk for innstillingsskjermen.
 * 
 * Funksjonalitet:
 * - Laster og lagrer e-postkonfigurasjon
 * - Håndterer debounced automatisk lagring av input
 * - Tester e-postkonfigurasjon
 * - Sjekker og håndterer tillatelser
 * - Navigerer til systeminnstillinger ved behov
 * 
 * @see SettingsScreen
 * @see SettingsUiState
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SettingsViewModel"
        // Debounce-tid i millisekunder før automatisk lagring
        private const val DEBOUNCE_MS = 200L
    }

    // Singleton repository for brukerdata
    private val repository = PreferencesRepository.getInstance(application)

    // UI-state som observeres av SettingsScreen
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Flows for debounced input-håndtering
    private val recipientEmailInput = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    private val gmailAddressInput = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    private val gmailPasswordInput = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)

    // Flow for navigasjons-hendelser (åpne systeminnstillinger)
    private val _navigationEvent = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val navigationEvent = _navigationEvent.asSharedFlow()

    // Midlertidig lagring av passord før det sendes til debounce-flow
    private var pendingGmailPassword: String = ""

    // Flush-funksjoner for manuell lagring (f.eks. når felt mister fokus)
    private var recipientEmailFlush: (() -> Unit)? = null
    private var gmailAddressFlush: (() -> Unit)? = null
    private var gmailPasswordFlush: (() -> Unit)? = null

    init {
        loadSettings()
        checkPermissions()
        setupDebounce()
    }

    /**
     * Laster lagrede innstillinger fra repository.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                recipientEmail = repository.getRecipientEmail(),
                gmailAddress = repository.getGmailAddress(),
                hasGmailPassword = repository.getGmailPassword().isNotEmpty()
            )
        }
    }

    /**
     * Setter opp debounced lagring for hvert input-felt.
     * Automatisk lagring skjer 200ms etter siste tastetrykk.
     */
    private fun setupDebounce() {
        // Mottaker e-post - lagres automatisk etter debounce
        recipientEmailFlush = setupDebounceSaveWithFlush(recipientEmailInput, DEBOUNCE_MS, {}, { repository.setRecipientEmail(it) }, null)
        
        // Gmail adresse - lagres automatisk etter debounce
        gmailAddressFlush = setupDebounceSaveWithFlush(gmailAddressInput, DEBOUNCE_MS, {}, { repository.setGmailAddress(it) }, null)
        
        // Gmail passord - oppdaterer også hasGmailPassword i UI når lagret
        gmailPasswordFlush = setupDebounceSaveWithFlush(gmailPasswordInput, DEBOUNCE_MS, { 
            _uiState.value = _uiState.value.copy(hasGmailPassword = repository.getGmailPassword().isNotEmpty())
        }, { 
            repository.setGmailPassword(it)
        }, null)
    }

    /**
     * Tvinger lagring av mottaker e-post umiddelbart.
     */
    fun flushRecipientEmail() { recipientEmailFlush?.invoke() }
    
    /**
     * Tvinger lagring av Gmail adresse umiddelbart.
     */
    fun flushGmailAddress() { gmailAddressFlush?.invoke() }
    
    /**
     * Tvinger lagring av Gmail passord umiddelbart.
     */
    fun flushGmailPassword() { gmailPasswordFlush?.invoke() }

    /**
     * Tvinger lagring av alle felt umiddelbart.
     * Brukes når skjermen forlates eller pauser.
     */
    fun flushAll() {
        flushRecipientEmail()
        flushGmailAddress()
        flushGmailPassword()
    }

    /**
     * Oppdaterer mottaker e-post i UI-state og sender til debounce-flow.
     */
    fun updateRecipientEmail(email: String) {
        _uiState.value = _uiState.value.copy(recipientEmail = email)
        recipientEmailInput.tryEmit(email)
    }

    /**
     * Oppdaterer Gmail adresse i UI-state og sender til debounce-flow.
     */
    fun updateGmailAddress(email: String) {
        _uiState.value = _uiState.value.copy(gmailAddress = email)
        gmailAddressInput.tryEmit(email)
    }

    /**
     * Oppdaterer Gmail passord (midlertidig) og sender til debounce-flow.
     */
    fun updateGmailPassword(password: String) {
        pendingGmailPassword = password
        gmailPasswordInput.tryEmit(password)
    }

    /**
     * Tester e-postkonfigurasjon ved å sende en test-e-post.
     * Viser last indicator under sending og resultat etterpå.
     */
    fun testEmail() {
        val state = _uiState.value

        // Bestem hvilket passord å bruke (input eller lagret)
        val passwordToUse = pendingGmailPassword.ifEmpty { repository.getGmailPassword() }
        
        // Valider at alle felt er fylt ut
        val hasPassword = state.hasGmailPassword || passwordToUse.isNotEmpty()
        if (state.recipientEmail.isEmpty() || state.gmailAddress.isEmpty() || !hasPassword) {
            _uiState.value = state.copy(testEmailResultRes = R.string.test_email_error_empty)
            return
        }

        // Valider e-postadresse-format
        if (!Patterns.EMAIL_ADDRESS.matcher(state.recipientEmail).matches()) {
            _uiState.value = state.copy(testEmailResultRes = R.string.test_email_error_invalid_recipient)
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(state.gmailAddress).matches()) {
            _uiState.value = state.copy(testEmailResultRes = R.string.test_email_error_invalid_gmail)
            return
        }

        // Sett loading-state og start test
        _uiState.value = state.copy(
            isSendingTestEmail = true,
            testEmailResultRes = 0,
            testEmailResultCustom = ""
        )

        // Kjør test asynkront
        viewModelScope.launch {
            val result = EmailSender.testEmailConfigWithParams(
                context = getApplication(),
                gmailAddress = state.gmailAddress,
                gmailPassword = passwordToUse,
                recipientEmail = state.recipientEmail
            )
            _uiState.value = _uiState.value.copy(
                isSendingTestEmail = false,
                testEmailResultCustom = result
            )
            Logger.d(TAG, "Test-epost resultat: $result")
        }
    }

    /**
     * Fjerner test-resultat fra UI.
     * Kalles etter at resultat er vist til bruker.
     */
    fun clearTestResult() {
        _uiState.value = _uiState.value.copy(
            testEmailResultRes = 0,
            testEmailResultCustom = ""
        )
    }

    /**
     * Sjekker status for alle nødvendige tillatelser og batterioptimalisering.
     * Oppdaterer UI-state med resultatene.
     */
    fun checkPermissions() {
        val appContext = getApplication<Application>()
        
        _uiState.value = _uiState.value.copy(
            hasAllPermissions = PermissionsHelper.hasAllPermissions(appContext),
            hasNotificationAccess = PermissionsHelper.isNotificationServiceEnabled(appContext),
            missingPermissionsCount = PermissionsHelper.getMissingPermissions(appContext).size,
            isIgnoringBatteryOptimizations = PermissionsHelper.isIgnoringBatteryOptimizations(appContext)
        )
    }

    /**
     * Ber brukeren om å deaktivere batterioptimalisering for appen.
     * Sender navigasjons-hendelse med riktig intent.
     */
    fun requestIgnoreBatteryOptimizations() {
        val intent = PermissionsHelper.getBatteryOptimizationIntent(getApplication())
        _navigationEvent.tryEmit(intent)
    }

    /**
     * Åpner systeminnstillinger for Notification Access.
     * Sender navigasjons-hendelse med riktig intent.
     */
    fun openNotificationSettings() {
        val intent = PermissionsHelper.getNotificationSettingsIntent()
        _navigationEvent.tryEmit(intent)
    }
}

/**
 * Data class som holder komplett UI-state for innstillingsskjermen.
 * 
 * @property recipientEmail E-postadressen det videresendes til
 * @property gmailAddress Gmail-adressen som brukes til sending
 * @property hasGmailPassword Om passord er lagret (ikke selve passordet)
 * @property isSendingTestEmail Om test-epost sendes akkurat nå
 * @property testEmailResultRes Resource ID for test-resultat (0 hvis custom)
 * @property testEmailResultCustom Custom test-resultat tekst
 * @property hasAllPermissions Om alle nødvendige tillatelser er gitt
 * @property hasNotificationAccess Om Notification Access er gitt
 * @property missingPermissionsCount Antall manglende tillatelser
 * @property isIgnoringBatteryOptimizations Om batterioptimalisering er deaktivert
 */
data class SettingsUiState(
    val recipientEmail: String = "",
    val gmailAddress: String = "",
    val hasGmailPassword: Boolean = false,
    val isSendingTestEmail: Boolean = false,
    val testEmailResultRes: Int = 0,
    val testEmailResultCustom: String = "",
    val hasAllPermissions: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val missingPermissionsCount: Int = 0,
    val isIgnoringBatteryOptimizations: Boolean = true
)