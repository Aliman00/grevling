package com.grevlingappen.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grevlingappen.domain.models.SaveStatus
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Felles hjelpefunksjon for å håndtere lagring med debounce og status-oppdatering.
 * Kan brukes av alle ViewModels som trenger auto-save funksjonalitet.
 * 
 * @param flow Flowen som inneholder input-data
 * @param debounceTimeMs Hvor lenge vi skal vente før vi lagrer
 * @param onStatusChange Callback for å oppdatere UI-status (SAVING, SAVED, NONE)
 * @param saveAction Selve lagringshandlingen
 * @param defaultValue Default-verdi som brukes hvis input er tom/blank etter debounce
 */
@OptIn(FlowPreview::class)
fun ViewModel.setupDebounceSave(
    flow: MutableSharedFlow<String>,
    debounceTimeMs: Long = 1000L,
    onStatusChange: (SaveStatus) -> Unit,
    saveAction: suspend (String) -> Unit,
    defaultValue: String? = null
) {
    viewModelScope.launch {
        flow.debounce(debounceTimeMs).collect { value ->
            onStatusChange(SaveStatus.SAVING)
            
            // Hvis tom og vi har default, bruk default
            val valueToSave = if (value.isBlank() && defaultValue != null) {
                defaultValue
            } else {
                value
            }
            
            saveAction(valueToSave)
            
            onStatusChange(SaveStatus.SAVED)
            delay(3000)
            onStatusChange(SaveStatus.NONE)
        }
    }
}
