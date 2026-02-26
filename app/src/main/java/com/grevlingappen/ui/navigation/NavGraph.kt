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

/**
 * NavGraph - Definerer navigasjonsstrukturen for hele appen.
 * 
 * Setter opp:
 * - Start-destinasjon (Home)
 * - Ruter til alle skjermer
 * - Hvilke composables som skal vises for hver rute
 * 
 * @param navController NavController fra Jetpack Navigation
 * @param snackbarHostState Delts snackbar state for popup-meldinger
 * @param modifier Modifier for layout-tilpasning
 * @param startDestination Start-rute (standard: Home)
 */
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
        modifier = modifier
    ) {
        // Home Screen
        composable(route = Screen.Home.route) {
            HomeScreen()
        }

        // Apps Screen
        composable(route = Screen.Apps.route) {
            AppsScreen()
        }

        // Settings Screen
        composable(route = Screen.Settings.route) {
            SettingsScreen(snackbarHostState = snackbarHostState)
        }
    }
}