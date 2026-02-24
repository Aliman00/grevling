package com.grevlingappen.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grevlingappen.domain.models.SaveStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * Setup debounce with immediate save on demand.
 * Returns a flush function that can be called to save immediately.
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
    
    // Collect immediately to update pendingValue and handle debounce
    viewModelScope.launch {
        flow.collect { value ->
            pendingValue = value
            
            // Cancel any existing debounce timer
            debounceJob?.cancel()
            
            // Start a new debounce timer
            debounceJob = launch {
                delay(debounceTimeMs)
                // If we get here, the user stopped typing for debounceTimeMs -> Save automatically
                val valueToSave = if (value.isBlank() && defaultValue != null) defaultValue else value
                onStatusChange(SaveStatus.SAVING)
                saveAction(valueToSave)
                onStatusChange(SaveStatus.SAVED)
                delay(2000)
                onStatusChange(SaveStatus.NONE)
            }
        }
    }
    
    val flush: () -> Unit = {
        // Cancel pending debounce to avoid double save
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
