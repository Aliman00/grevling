package com.grevlingappen.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grevlingappen.domain.models.SaveStatus
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
    debounceTimeMs: Long = 200L,
    onStatusChange: (SaveStatus) -> Unit,
    saveAction: suspend (String) -> Unit,
    defaultValue: String? = null
): () -> Unit {
    var pendingValue: String? = null
    
    val flush: () -> Unit = {
        pendingValue?.let { value ->
            onStatusChange(SaveStatus.SAVING)
            val valueToSave = if (value.isBlank() && defaultValue != null) defaultValue else value
            viewModelScope.launch {
                saveAction(valueToSave)
                onStatusChange(SaveStatus.SAVED)
                delay(3000)
                onStatusChange(SaveStatus.NONE)
            }
        }
        Unit
    }
    
    return flush
}
