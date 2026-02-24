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
 * SettingsViewModel - Håndterer forretningslogikk for innstillingsskjermen.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SettingsViewModel"
        private const val DEBOUNCE_MS = 200L
    }

    // Bruk singleton istedenfor ny instans
    private val repository = PreferencesRepository.getInstance(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val recipientEmailInput = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    private val gmailAddressInput = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    private val gmailPasswordInput = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)

    private val _navigationEvent = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val navigationEvent = _navigationEvent.asSharedFlow()

    private var pendingGmailPassword: String = ""

    private var recipientEmailFlush: (() -> Unit)? = null
    private var gmailAddressFlush: (() -> Unit)? = null
    private var gmailPasswordFlush: (() -> Unit)? = null

    init {
        loadSettings()
        checkPermissions()
        setupDebounce()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                recipientEmail = repository.getRecipientEmail(),
                gmailAddress = repository.getGmailAddress(),
                hasGmailPassword = repository.getGmailPassword().isNotEmpty()
            )
        }
    }

    private fun setupDebounce() {
        recipientEmailFlush = setupDebounceSaveWithFlush(recipientEmailInput, DEBOUNCE_MS, {}, { repository.setRecipientEmail(it) }, null)
        gmailAddressFlush = setupDebounceSaveWithFlush(gmailAddressInput, DEBOUNCE_MS, {}, { repository.setGmailAddress(it) }, null)
        gmailPasswordFlush = setupDebounceSaveWithFlush(gmailPasswordInput, DEBOUNCE_MS, { 
            _uiState.value = _uiState.value.copy(hasGmailPassword = repository.getGmailPassword().isNotEmpty())
        }, { 
            repository.setGmailPassword(it)
        }, null)
    }

    fun flushRecipientEmail() { recipientEmailFlush?.invoke() }
    fun flushGmailAddress() { gmailAddressFlush?.invoke() }
    fun flushGmailPassword() { gmailPasswordFlush?.invoke() }

    fun flushAll() {
        flushRecipientEmail()
        flushGmailAddress()
        flushGmailPassword()
    }

    fun updateRecipientEmail(email: String) {
        _uiState.value = _uiState.value.copy(recipientEmail = email)
        recipientEmailInput.tryEmit(email)
    }

    fun updateGmailAddress(email: String) {
        _uiState.value = _uiState.value.copy(gmailAddress = email)
        gmailAddressInput.tryEmit(email)
    }

    fun updateGmailPassword(password: String) {
        pendingGmailPassword = password
        gmailPasswordInput.tryEmit(password)
    }

    /**
     * Utfører test av e-postkonfigurasjon asynkront via coroutines.
     */
    fun testEmail() {
        val state = _uiState.value

        val passwordToUse = pendingGmailPassword.ifEmpty { repository.getGmailPassword() }
        
        // Validering - sjekk både lagret passord og direkte input
        val hasPassword = state.hasGmailPassword || passwordToUse.isNotEmpty()
        if (state.recipientEmail.isEmpty() || state.gmailAddress.isEmpty() || !hasPassword) {
            _uiState.value = state.copy(testEmailResultRes = R.string.test_email_error_empty)
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(state.recipientEmail).matches()) {
            _uiState.value = state.copy(testEmailResultRes = R.string.test_email_error_invalid_recipient)
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(state.gmailAddress).matches()) {
            _uiState.value = state.copy(testEmailResultRes = R.string.test_email_error_invalid_gmail)
            return
        }

        _uiState.value = state.copy(
            isSendingTestEmail = true,
            testEmailResultRes = 0,
            testEmailResultCustom = ""
        )

        // Bruker UI state-verdier direkte i stedet for å vente på debounce
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
            Logger.d(TAG, "Test-email resultat: $result")
        }
    }

    fun clearTestResult() {
        _uiState.value = _uiState.value.copy(
            testEmailResultRes = 0,
            testEmailResultCustom = ""
        )
    }

    /**
     * Sjekker status for alle nødvendige tillatelser og batterioptimalisering.
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

    fun requestIgnoreBatteryOptimizations() {
        val intent = PermissionsHelper.getBatteryOptimizationIntent(getApplication())
        _navigationEvent.tryEmit(intent)
    }

    fun openNotificationSettings() {
        val intent = PermissionsHelper.getNotificationSettingsIntent()
        _navigationEvent.tryEmit(intent)
    }
}

/**
 * Data class for UI state på innstillingsskjermen.
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
