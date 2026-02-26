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

/**
 * HomeViewModel - ViewModel som håndterer business-logikk for HomeScreen.
 * 
 * Funksjonalitet:
 * - Holder UI-state som HomeScreen observerer via StateFlow
 * - Kommuniserer med PreferencesRepository for data
 * - Håndterer lagring av auto-svar meldinger
 * - Veksler mellom aktivert/deaktivert videresending
 * 
 * Arkitektur:
 * HomeScreen observerer state og kaller ViewModel-funksjoner
 * ViewModel oppdaterer state → UI re-renders automatisk
 * 
 * @see HomeScreen
 * @see ForwardingState
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    // Repository for tilgang til brukerdata
    private val repository = PreferencesRepository.getInstance(application)

    // UI-state som observeres av HomeScreen
    private val _state = MutableStateFlow(ForwardingState())
    val state: StateFlow<ForwardingState> = _state.asStateFlow()

    /**
     * Refresher tillatelser og status.
     * Kalles når brukeren kommer tilbake til appen for å sikre oppdatert data.
     */
    fun refreshPermissions() {
        repository.refreshState()
    }

    /**
     * Initialiserer ViewModel og starter observasjon av repository-state.
     * Setter opp collect som oppdaterer UI-state ved endringer.
     */
    init {
        Logger.d(TAG, "HomeViewModel initialisert")

        viewModelScope.launch {
            repository.state.collect { repoState ->
                // Ved første init: kopier hele tilstanden
                if (_state.value == ForwardingState()) {
                    _state.value = repoState
                } else {
                    // Ved oppdateringer: bevar eksisterende meldinger, oppdater kun statusfelter
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

    /**
     * Veksler videresending av/på.
     * Kaller repository som faktisk endrer tilstanden.
     */
    fun toggleForwarding() {
        repository.toggleForwarding()
        Logger.d(TAG, "Videresending togglet")
    }

    /**
     * Aktiverer eller deaktiverer auto-svar.
     * 
     * @param enabled true for å aktivere, false for å deaktivere
     */
    fun toggleAutoReply(enabled: Boolean) {
        repository.setAutoReplyEnabled(enabled)
        Logger.d(TAG, "Auto-svar: $enabled")
    }

    /**
     * Velger om samme melding skal brukes for SMS og anrop.
     * 
     * @param useSame true for å bruke enhetlig melding, false for separate
     */
    fun toggleUseSameMessage(useSame: Boolean) {
        repository.setUseSameMessage(useSame)
        Logger.d(TAG, "Bruk samme melding: $useSame")
    }

    /**
     * Oppdaterer enhetlig auto-svar melding (UI-state kun).
     * Må kalle saveMessages() for å lagre til disk.
     */
    fun updateUnifiedMessage(message: String) {
        _state.value = _state.value.copy(unifiedMessage = message)
    }

    /**
     * Oppdaterer SMS auto-svar melding (UI-state kun).
     * Må kalle saveMessages() for å lagre til disk.
     */
    fun updateSmsMessage(message: String) {
        _state.value = _state.value.copy(smsMessage = message)
    }

    /**
     * Oppdaterer anrop auto-svar melding (UI-state kun).
     * Må kalle saveMessages() for å lagre til disk.
     */
    fun updateCallMessage(message: String) {
        _state.value = _state.value.copy(callMessage = message)
    }

    /**
     * Lagrer alle auto-svar meldinger til persistent lagring.
     * Bruker standard-meldinger hvis feltene er tomme.
     */
    fun saveMessages() {
        val app = getApplication<Application>()
        
        // Bruk standard-meldinger hvis brukerens melding er tom
        val unified = _state.value.unifiedMessage.ifBlank { app.getString(R.string.default_unified_message) }
        val sms = _state.value.smsMessage.ifBlank { app.getString(R.string.default_sms_message) }
        val call = _state.value.callMessage.ifBlank { app.getString(R.string.default_call_message) }
        
        // Lagre til repository
        repository.setUnifiedMessage(unified)
        repository.setSmsMessage(sms)
        repository.setCallMessage(call)
        
        // Oppdater local state
        _state.value = _state.value.copy(unifiedMessage = unified, smsMessage = sms, callMessage = call)
        Logger.d(TAG, "Meldinger lagret")
    }
}