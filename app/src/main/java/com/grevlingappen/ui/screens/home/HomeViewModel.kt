package com.grevlingappen.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grevlingappen.R
import com.grevlingappen.data.PreferencesRepository
import com.grevlingappen.domain.models.ForwardingState
import com.grevlingappen.utils.Logger
import com.grevlingappen.utils.setupDebounceSave
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


// ============================================================================
// HOME VIEWMODEL - Business logic for HomeScreen
// ============================================================================
// Dette er "hjernen" bak HomeScreen:
// - Holder på UI state (StateFlow)
// - Kommuniserer med PreferencesRepository
// - Håndterer auto-save med debounce
// - Validerer input
//
// COMPOSE PATTERN:
// HomeScreen observerer state og kaller ViewModel-funksjoner
// ViewModel oppdaterer state → UI re-renders automatisk

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HomeViewModel"
        private const val AUTO_SAVE_DELAY_MS = 1000L // 1 sekund debounce
    }

    // ------------------------------------------------------------------------
    // REPOSITORY - Data layer
    // ------------------------------------------------------------------------
    // Bruk singleton istedenfor ny instans
    private val repository = PreferencesRepository.getInstance(application)

    // ------------------------------------------------------------------------
    // STATE - UI state som HomeScreen observerer
    // ------------------------------------------------------------------------
    private val _state = MutableStateFlow(ForwardingState())
    val state: StateFlow<ForwardingState> = _state.asStateFlow()

    fun refreshPermissions() {
        repository.refreshState()
    }

    // Flows for auto-save med debounce
    private val unifiedMessageInput = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    private val smsMessageInput = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    private val callMessageInput = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)

    // ------------------------------------------------------------------------
    // INITIALIZATION - Load initial state fra repository
    // ------------------------------------------------------------------------
    init {
        Logger.d(TAG, "HomeViewModel initialisert")

        // Observér repository state og sync til vår _state
        viewModelScope.launch {
            repository.state.collect { repoState ->
                _state.value = repoState
                Logger.d(TAG, "State oppdatert fra repository: enabled=${repoState.isEnabled}")
            }
        }

        setupDebounce()
    }

    private fun setupDebounce() {
        val defaultUnified = getApplication<Application>().getString(R.string.default_unified_message)
        val defaultSms = getApplication<Application>().getString(R.string.default_sms_message)
        val defaultCall = getApplication<Application>().getString(R.string.default_call_message)
        
        setupDebounceSave(unifiedMessageInput, AUTO_SAVE_DELAY_MS, { status -> 
            _state.value = _state.value.copy(saveStatusUnified = status) 
        }, { value -> 
            repository.setUnifiedMessage(value)
        }, defaultUnified)
        
        setupDebounceSave(smsMessageInput, AUTO_SAVE_DELAY_MS, { status -> 
            _state.value = _state.value.copy(saveStatusSms = status) 
        }, { value -> 
            repository.setSmsMessage(value)
        }, defaultSms)
        
        setupDebounceSave(callMessageInput, AUTO_SAVE_DELAY_MS, { status -> 
            _state.value = _state.value.copy(saveStatusCall = status) 
        }, { value -> 
            repository.setCallMessage(value)
        }, defaultCall)
    }

    // ------------------------------------------------------------------------
    // PUBLIC API - HOVEDFUNKSJONER
    // ------------------------------------------------------------------------

    /**
     * Toggle videresending av/på
     */
    fun toggleForwarding() {
        repository.toggleForwarding()
        Logger.d(TAG, "Forwarding toggled")
    }

    /**
     * Toggle auto-reply av/på
     */
    fun toggleAutoReply(enabled: Boolean) {
        repository.setAutoReplyEnabled(enabled)
        Logger.d(TAG, "Auto-reply: $enabled")
    }

    /**
     * Toggle mellom samme melding vs separate meldinger
     */
    fun toggleUseSameMessage(useSame: Boolean) {
        repository.setUseSameMessage(useSame)
        Logger.d(TAG, "Use same message: $useSame")
    }

    // ------------------------------------------------------------------------
    // AUTO-SAVE FUNKSJONER - Med debounce
    // ------------------------------------------------------------------------
    // Disse venter 1 sekund etter siste endring før lagring
    // Unngår å lagre ved hver tasting

    /**
     * Oppdater unified message (auto-saves etter 1 sekund)
     */
    fun updateUnifiedMessage(message: String) {
        _state.value = _state.value.copy(unifiedMessage = message)
        unifiedMessageInput.tryEmit(message)
    }

    /**
     * Oppdater SMS message (auto-saves etter 1 sekund)
     */
    fun updateSmsMessage(message: String) {
        _state.value = _state.value.copy(smsMessage = message)
        smsMessageInput.tryEmit(message)
    }

    /**
     * Oppdater call message (auto-saves etter 1 sekund)
     */
    fun updateCallMessage(message: String) {
        _state.value = _state.value.copy(callMessage = message)
        callMessageInput.tryEmit(message)
    }

    // ------------------------------------------------------------------------
    // LIFECYCLE - Cleanup
    // ------------------------------------------------------------------------
    // Fjernet onDestroy()-kall - singleton lever hele appens levetid
}

// ============================================================================
// HVORFOR AndroidViewModel?
// ============================================================================
// AndroidViewModel gir oss tilgang til Application context som aldri leaker.
// Vi trenger context for å lage PreferencesRepository.
//
// AndroidViewModel vs ViewModel:
// - AndroidViewModel: Har getApplication() context (safe)
// - ViewModel: Ingen context (må passes som parameter til funksjoner)
//
// Vi bruker AndroidViewModel fordi PreferencesRepository trenger Context
// for EncryptedSharedPreferences.
