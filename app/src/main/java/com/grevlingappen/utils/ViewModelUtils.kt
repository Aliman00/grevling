package com.grevlingappen.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grevlingappen.domain.models.SaveStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * Setter opp debounced lagring med mulighet for umiddelbar lagring.
 * 
 * Funksjonalitet:
 * - Vent debounceTimeMs etter siste input før automatiske lagring
 * - Returnerer en flush-funksjon som lagrer umiddelbart ved behov
 * - Oppdaterer status underveis (SAVING, SAVED, NONE)
 * 
 * Brukes av SettingsViewModel for å automatiske lagre e-postinnstillinger
 * mens brukeren skriver, uten å lagre ved hvert tastetrykk.
 * 
 * @param flow Flow som mottar input-verdier
 * @param debounceTimeMs Millisekunder å vente etter siste input før lagring
 * @param onStatusChange Callback for å oppdatere lagre-status
 * @param saveAction Suspend-funksjon som faktisk lagrer verdien
 * @param defaultValue Standardverdi å bruke hvis input er tom
 * @return Flush-funksjon som kan kalles for umiddelbar lagring
 */
@Suppress("UNUSED_PARAMETER")
fun ViewModel.setupDebounceSaveWithFlush(
    flow: MutableSharedFlow<String>,
    debounceTimeMs: Long = 500L,
    onStatusChange: (SaveStatus) -> Unit,
    saveAction: suspend (String) -> Unit,
    defaultValue: String? = null
): () -> Unit {
    var pendingValue: String? = null
    var debounceJob: Job? = null
    
    // Lytt på flow kontinuerlig
    viewModelScope.launch {
        flow.collect { value ->
            pendingValue = value
            
            // Avbryt eksisterende debounce-timer
            debounceJob?.cancel()
            
            // Start ny debounce-timer
            debounceJob = launch {
                delay(debounceTimeMs)
                // Hvis vi kommer hit, har brukeren sluttet å skrive i debounceTimeMs
                // -> lagre automatisk
                val valueToSave = if (value.isBlank() && defaultValue != null) defaultValue else value
                onStatusChange(SaveStatus.SAVING)
                saveAction(valueToSave)
                onStatusChange(SaveStatus.SAVED)
                delayaveStatus.SAV(2000)  // Vis "lagret" i 2 sekunder
                onStatusChange(SaveStatus.NONE)
            }
        }
    }
    
    // Flush-funksjon: lagre umiddelbart
    val flush: () -> Unit = {
        // Avbryt debounce for å unngå dobbel lagring
        debounceJob?.cancel()
        
        pendingValue?.let { value ->
            val valueToSave = if (value.isBlank() && defaultValue != null) defaultValue else value
            viewModelScope.launch {
                onStatusChange(SaveStatus.SAVING)
                saveAction(valueToSave)
                onStatusChange(SaveStatus.SAVED)
                delay(2000)
                onStatusChange(SaveStatus.NONE)
            }
        }
    }
    
    return flush
}