package com.grevlingappen.ui.screens.apps

import com.grevlingappen.domain.models.AppInfo

/**
 * AppsState - Data class som holder komplett UI-state for app-velgerskjermen.
 * 
 * @property allApps Liste over alle installerte apper
 * @property filteredApps Filtrert liste basert på søk og valg
 * @property searchQuery Gjeldende søkestreng
 * @property showOnlySelected Om kun valgte apper vises
 * @property selectedCount Antall valgte apper for overvåking
 * @property isLoading Om app-listen lastes akkurat nå
 */
data class AppsState(
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val showOnlySelected: Boolean = false,
    val selectedCount: Int = 0,
    val isLoading: Boolean = true
)