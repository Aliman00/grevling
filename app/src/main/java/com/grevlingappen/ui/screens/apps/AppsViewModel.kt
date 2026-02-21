package com.grevlingappen.ui.screens.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grevlingappen.data.AppRepository
import com.grevlingappen.data.PreferencesRepository
import com.grevlingappen.domain.models.AppInfo
import com.grevlingappen.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AppsViewModel"
    }

    // Bruk singleton istedenfor ny instans
    private val repository = PreferencesRepository.getInstance(application)
    private val appRepository = AppRepository(application)

    private val _state = MutableStateFlow(AppsState())
    val state: StateFlow<AppsState> = _state.asStateFlow()

    private var filterJob: Job? = null

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val selectedPackages = repository.getMonitoredApps()
                val apps = appRepository.getInstalledApps(selectedPackages)

                _state.value = _state.value.copy(
                    allApps = apps,
                    filteredApps = apps,
                    selectedCount = selectedPackages.size,
                    isLoading = false
                )

                Logger.d(TAG, "Lastet ${apps.size} apper (${selectedPackages.size} valgt)")

            } catch (_: Exception) {
                Logger.e(TAG, "Feil ved lasting av apper")
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilters(debounce = true)
    }

    fun toggleShowOnlySelected(showOnlySelected: Boolean) {
        _state.value = _state.value.copy(showOnlySelected = showOnlySelected)
        applyFilters(debounce = false)
    }

    private fun applyFilters(debounce: Boolean) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            if (debounce) {
                delay(300) // Vent 300ms før filtrering (debounce)
            }
            
            val currentState = _state.value
            var filtered = AppRepository.searchApps(currentState.allApps, currentState.searchQuery)
            filtered = AppRepository.filterSelected(filtered, currentState.showOnlySelected)
            
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(filteredApps = filtered)
            }
        }
    }

    fun toggleApp(app: AppInfo) {
        // Lagre til disk
        repository.toggleApp(app.packageName, getApplication<Application>())
        
        // Oppdater i minnet umiddelbart for bedre ytelse (optimistic update)
        val updatedAllApps = _state.value.allApps.map {
            if (it.packageName == app.packageName) {
                it.copy(isSelected = !it.isSelected)
            } else it
        }
        
        val selectedCount = updatedAllApps.count { it.isSelected }
        
        _state.value = _state.value.copy(
            allApps = updatedAllApps,
            selectedCount = selectedCount
        )
        
        // Apply filter på nytt umiddelbart
        applyFilters(debounce = false)
        
        Logger.d(TAG, "Toggle app: ${app.packageName}, ny status: ${!app.isSelected}")
    }

    // Fjernet onDestroy()-kall - singleton lever hele appens levetid
}
