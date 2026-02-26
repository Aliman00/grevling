package com.grevlingappen.domain.models

/**
 * AppInfo - Data class som representerer en installert applikasjon på enheten.
 * 
 * Brukes til å vise app-listen i app-velgerskjermen og holde styr på
 * hvilke apper brukeren har valgt for overvåking.
 * 
 * @property appName Visningsnavn for appen (f.eks. "Facebook")
 * @property packageName Unik identifikator for appen (f.eks. "com.facebook.katana")
 * @property isSelected Om appen er valgt for overvåking
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val isSelected: Boolean = false
)