package com.grevlingappen.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.grevlingappen.R
import com.grevlingappen.ui.navigation.NavGraph
import com.grevlingappen.ui.navigation.Screen

// ============================================================================
// MAIN SCREEN - Hovedlayout med bottom navigation
// ============================================================================
// Dette er "roten" av hele UI-strukturen med Material 3 design

@Composable
fun MainScreen() {
    // NavController holder styr på navigation state
    val navController = rememberNavController()
    
    // SnackbarHostState for popup-meldinger
    val snackbarHostState = remember { SnackbarHostState() }

    // Observér current destination for å highlighte riktig tab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // ------------------------------------------------------------------------
    // BOTTOM NAV ITEMS - Bruker remember for å unngå re-allokering
    // ------------------------------------------------------------------------
    val homeLabel = stringResource(R.string.nav_home)
    val appsLabel = stringResource(R.string.nav_apps)
    val settingsLabel = stringResource(R.string.nav_settings)
    
    val bottomNavItems = remember(homeLabel, appsLabel, settingsLabel) {
        listOf(
            BottomNavItem(
                screen = Screen.Home,
                icon = Icons.Default.Home,
                label = homeLabel
            ),
            BottomNavItem(
                screen = Screen.Apps,
                icon = Icons.Default.Apps,
                label = appsLabel
            ),
            BottomNavItem(
                screen = Screen.Settings,
                icon = Icons.Default.Settings,
                label = settingsLabel
            )
        )
    }

    // ------------------------------------------------------------------------
    // SCAFFOLD - Material 3 layout structure
    // ------------------------------------------------------------------------
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxWidth(),
                snackbar = { data ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 32.dp, vertical = 16.dp)
                                .wrapContentWidth(),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            tonalElevation = 4.dp
                        ) {
                            Text(
                                text = data.visuals.message,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                // Loop gjennom alle bottom nav items
                bottomNavItems.forEach { item ->
                    // Sjekk om denne screen er aktiv
                    val isSelected = currentDestination?.hierarchy?.any {
                        it.route == item.screen.route
                    } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = isSelected,
                        onClick = {
                            // Naviger til selected screen
                            navController.navigate(item.screen.route) {
                                // Unngå å bygge opp stor back-stack:
                                // Alltid pop til start når vi bytter tab
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Unngå multiple copies av samme destination
                                launchSingleTop = true
                                // Restore state når vi går tilbake til en tab
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Content area - NavGraph håndterer hvilken screen som vises
        NavGraph(
            navController = navController,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// ============================================================================
// BOTTOM NAV ITEM - Data class for bottom navigation items
// ============================================================================
// Kobler screen, icon og label sammen
// Må være private data class (ikke kan brukes utenfor denne filen)

private data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)
