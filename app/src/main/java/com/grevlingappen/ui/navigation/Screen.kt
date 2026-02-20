package com.grevlingappen.ui.navigation

// ============================================================================
// SCREEN - Sealed class for navigation routes
// ============================================================================
// Definerer alle skjermer i appen med type-sikre routes.
//
// SEALED CLASS FORDELER:
// - Type-safe navigation (ingen String-typos)
// - Autocomplete i IDE
// - Enklere refactoring
// - Compile-time sjekk av alle routes
//
// HVORDAN DET FUNGERER:
// Hver screen har en route (String) som identifiserer den i NavController

sealed class Screen(val route: String) {

    // ------------------------------------------------------------------------
    // MAIN SCREENS - Tilgjengelige fra bottom navigation
    // ------------------------------------------------------------------------

    /**
     * Home Screen - Hovedskjerm med status og toggle
     * Route: "home"
     * Icon: 🏠
     */
    data object Home : Screen("home")

    /**
     * Apps Screen - Velg hvilke apper som skal overvåkes
     * Route: "apps"
     * Icon: 📱
     */
    data object Apps : Screen("apps")

    /**
     * Settings Screen - Email-konfigurasjon og tillatelser
     * Route: "settings"
     * Icon: ⚙️
     */
    data object Settings : Screen("settings")
}
