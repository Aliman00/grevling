package com.grevlingappen.ui.navigation

/**
 * Screen - Sealed class som definerer alle navigasjonsruter i appen.
 * 
 * Fordeler med sealed class:
 * - Type-sikker navigasjon (ingen String-typos)
 * - Autocomplete i IDE
 * - Enklere refactoring
 * - Compile-time sjekk av alle ruter
 * 
 * Hver skjerm har en route (String) som identifiserer den i NavController.
 */
sealed class Screen(val route: String) {

    /**
     * Home Screen - Hovedskjerm med status og toggle.
     * Viser videresendingsstatus og lar brukeren aktivere/deaktivere.
     * 
     * Route: "home"
     */
    data object Home : Screen("home")

    /**
     * Apps Screen - Velg hvilke apper som skal overvåkes.
     * Viser liste over installerte apper og lar brukeren velge.
     * 
     * Route: "apps"
     */
    data object Apps : Screen("apps")

    /**
     * Settings Screen - E-postkonfigurasjon og tillatelser.
     * Lar brukeren sette opp Gmail, e-postmottaker, og gi nødvendige tillatelser.
     * 
     * Route: "settings"
     */
    data object Settings : Screen("settings")
}