package com.grevlingappen.ui.screens.apps

import com.grevlingappen.domain.models.AppInfo

data class AppsState(
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val showOnlySelected: Boolean = false,
    val selectedCount: Int = 0,
    val isLoading: Boolean = true
)
