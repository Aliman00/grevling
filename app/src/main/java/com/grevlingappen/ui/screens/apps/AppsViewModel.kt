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

/**
 * AppsViewModel - ViewModel som håndterer business-logikk for app-velgerskjermen.
 * 
 * Funksjonalitet:
 * - Laster installerte apper fra enheten
 * - Søker og filtrerer app-listen
 * - Håndterer valg av apper for overvåking
 * - Viser antall valgte apper
 * 
 * @see AppsScreen
 * @see AppsState
 * @see AppInfo
 */
class AppsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AppsViewModel"
    }

    // Singleton repositories
    private val repository = PreferencesRepository.getInstance(application)
    private val appRepository = AppRepository(application)

    // UI-state som observeres av AppsScreen
    private val _state = MutableStateFlow(AppsState())
    val state: StateFlow<AppsState> = _state.asStateFlow()

    // Job for filtrering (kan kanselleres ved nye søk)
    private var filterJob: Job? = null

    init {
        loadInstalledApps()
    }

    /**
     * Laster alle installerte apper fra enheten.
     * Viser loading-indikator mens lasting pågår.
     */
    fun loadInstalledApps() {
        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // Hent først hvilke apper som allerede er valgt for overvåking
                val selectedPackages = repository.getMonitoredApps()
                // Hent deretter alle installerte apper
                val apps = appRepository.getInstalledApps(selectedPackages)

                // Oppdater state med alle apper og merk valgte
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

    /**
     * Oppdaterer søkestrengen og filtrerer app-listen.
     * Bruker debounce for å unngå for mange oppdateringer.
     */
    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilters(debounce = true)
    }

    /**
     * Veksler mellom å vise alle apper og kun valgte apper.
     * 
     * @param showOnlySelected true for kun valgte, false for alle
     */
    fun toggleShowOnlySelected(showOnlySelected: Boolean) {
        _state.value = _state.value.copy(showOnlySelected = showOnlySelected)
        applyFilters(debounce = false)
    }

    /**
     * Filtrerer app-listen basert på søk og valg-status.
     * 
     * @param debounce true for å vente 300ms før filtrering
     */
    private fun applyFilters(debounce: Boolean) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            // Vent debounce-tid hvis søk (unngå filtrering ved hvert tastetrykk)
            if (debounce) {
                delay(300)
            }
            
            val currentState = _state.value
            
            // Først: filtrer basert på søk
            var filtered = AppRepository.searchApps(currentState.allApps, currentState.searchQuery)
            
            // Deretter: filtrer basert på valg-status
            filtered = AppRepository.filterSelected(filtered, currentState.showOnlySelected)
            
            // Oppdater UI på hovedtråden
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(filteredApps = filtered)
            }
        }
    }

    /**
     * Veksler overvåking av én app.
     * 
     * @param app AppInfo for appen å toggle
     */
    fun toggleApp(app: AppInfo) {
        // Lagre til persistent lagring
        repository.toggleApp(app.packageName)
        
        // Oppdater UI umiddelbart for bedre brukeropplevelse (optimistic update)
        val updatedAllApps = _state.value.allApps.map {
            if (it.packageName == app.packageName) {
                it.copy(isSelected = !it.isSelected)
            } else it
        }
        
        // Tell antall valgte
        val selectedCount = updatedAllApps.count { it.isSelected }
        
        _state.value = _state.value.copy(
            allApps = updatedAllApps,
            selectedCount = selectedCount
        )
        
        // Refresh filter for å oppdatere visningen
        applyFilters(debounce = false)
        
        Logger.d(TAG, "Toggle app: ${app.packageName}, ny status: ${!app.isSelected}")
    }
}