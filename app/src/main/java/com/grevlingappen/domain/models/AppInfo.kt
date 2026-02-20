package com.grevlingappen.domain.models

/**
 * AppInfo - Dataklasse som representerer en installert applikasjon på enheten.
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val isSelected: Boolean = false
)
