package com.grevlingappen.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color.kt - Spesialfarger som ikke er en del av standard Material 3 ColorScheme.
 * 
 * MERK: Hovedfargene for appen (Primary, Background, etc.) hentes direkte 
 * fra colors.xml i Theme.kt for å unngå duplisering.
 */

// Statusfarger for HomeScreen (bør matche status_active_color og status_paused_color i colors.xml)
val StatusActive = Color(0xFF4CAF50)
val StatusPaused = Color(0xFFEF5350)

// Ekstra statusfarger (brukes i diverse UI-komponenter)
val StatusSuccess = Color(0xFF1B873B)
val StatusWarning = Color(0xFF7D5800)
val StatusError = Color(0xFFBA1A1A)