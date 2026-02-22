package com.grevlingappen.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grevlingappen.R
import com.grevlingappen.data.PreferencesRepository
import com.grevlingappen.domain.models.ForwardingState
import com.grevlingappen.utils.Logger
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
// - Håndterer lagring av meldinger
//
// COMPOSE PATTERN:
// HomeScreen observerer state og kaller ViewModel-funksjoner
// ViewModel oppdaterer state → UI re-renders automatisk

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    // ------------------------------------------------------------------------
    // REPOSITORY - Data layer
    // ------------------------------------------------------------------------
    private val repository = PreferencesRepository.getInstance(application)

    // ------------------------------------------------------------------------
    // STATE - UI state som HomeScreen observerer
    // ------------------------------------------------------------------------
    private val _state = MutableStateFlow(ForwardingState())
    val state: StateFlow<ForwardingState> = _state.asStateFlow()

    fun refreshPermissions() {
        repository.refreshState()
    }

    // ------------------------------------------------------------------------
    // INITIALIZATION - Load initial state fra repository
    // ------------------------------------------------------------------------
    init {
        Logger.d(TAG, "HomeViewModel initialisert")

        viewModelScope.launch {
            repository.state.collect { repoState ->
                if (_state.value == ForwardingState()) {
                    _state.value = repoState
                } else {
                    _state.value = _state.value.copy(
                        isEnabled = repoState.isEnabled,
                        hasNotificationAccess = repoState.hasNotificationAccess,
                        hasEmailConfig = repoState.hasEmailConfig,
                        autoReplyEnabled = repoState.autoReplyEnabled,
                        useSameMessage = repoState.useSameMessage,
                        statusMessage = repoState.statusMessage
                    )
                }
                Logger.d(TAG, "State oppdatert fra repository: enabled=${repoState.isEnabled}")
            }
        }
    }

    // ------------------------------------------------------------------------
    // PUBLIC API - HOVEDFUNKSJONER
    // ------------------------------------------------------------------------

    fun toggleForwarding() {
        repository.toggleForwarding()
        Logger.d(TAG, "Forwarding toggled")
    }

    fun toggleAutoReply(enabled: Boolean) {
        repository.setAutoReplyEnabled(enabled)
        Logger.d(TAG, "Auto-reply: $enabled")
    }

    fun toggleUseSameMessage(useSame: Boolean) {
        repository.setUseSameMessage(useSame)
        Logger.d(TAG, "Use same message: $useSame")
    }

    fun updateUnifiedMessage(message: String) {
        _state.value = _state.value.copy(unifiedMessage = message)
    }

    fun updateSmsMessage(message: String) {
        _state.value = _state.value.copy(smsMessage = message)
    }

    fun updateCallMessage(message: String) {
        _state.value = _state.value.copy(callMessage = message)
    }

    fun saveMessages() {
        val app = getApplication<Application>()
        val unified = _state.value.unifiedMessage.ifBlank { app.getString(R.string.default_unified_message) }
        val sms = _state.value.smsMessage.ifBlank { app.getString(R.string.default_sms_message) }
        val call = _state.value.callMessage.ifBlank { app.getString(R.string.default_call_message) }
        
        repository.setUnifiedMessage(unified)
        repository.setSmsMessage(sms)
        repository.setCallMessage(call)
        
        _state.value = _state.value.copy(unifiedMessage = unified, smsMessage = sms, callMessage = call)
        Logger.d(TAG, "Meldinger lagret")
    }
}
