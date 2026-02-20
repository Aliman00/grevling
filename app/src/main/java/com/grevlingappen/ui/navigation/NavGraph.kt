package com.grevlingappen.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.grevlingappen.ui.screens.apps.AppsScreen
import com.grevlingappen.ui.screens.home.HomeScreen
import com.grevlingappen.ui.screens.settings.SettingsScreen

// ============================================================================
// NAV GRAPH - Definerer navigasjons-struktur for hele appen
// ============================================================================

@Composable
fun NavGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier  // ← Viktig for å respektere Scaffold padding
    ) {
        // --------------------------------------------------------------------
        // HOME SCREEN
        // --------------------------------------------------------------------
        composable(route = Screen.Home.route) {
            HomeScreen()
        }

        // --------------------------------------------------------------------
        // APPS SCREEN
        // --------------------------------------------------------------------
        composable(route = Screen.Apps.route) {
            AppsScreen()
        }

        // --------------------------------------------------------------------
        // SETTINGS SCREEN
        // --------------------------------------------------------------------
        composable(route = Screen.Settings.route) {
            SettingsScreen(snackbarHostState = snackbarHostState)
        }
    }
}
