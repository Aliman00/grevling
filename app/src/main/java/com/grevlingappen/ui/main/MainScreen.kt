package com.grevlingappen.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

/**
 * MainScreen - Hovedlayout med bottom navigation.
 * 
 * Dette er "roten" av hele UI-strukturen.
 * Inneholder:
 * - Bottom navigation med 3 faner (Hjem, Apper, Innstillinger)
 * - SnackbarHost for popup-meldinger
 * - Navigation graph som håndterer visning av ulike skjermer
 */
@Composable
fun MainScreen() {
    // NavController holder styr på navigasjon mellom skjermer
    val navController = rememberNavController()
    
    // SnackbarHostState for popup-meldinger (brukes av ulike skjermer)
    val snackbarHostState = remember { SnackbarHostState() }

    // Observér current destination for å markere riktig fane
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Bottom navigation elementer
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

    // Material 3 Scaffold - grunnleggende layoutstruktur
    Scaffold(
        // Snackbar for meldinger
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
        // Bottom navigation bar
        bottomBar = {
            Column {
                // "Laget av" tekst
                Text(
                    text = stringResource(R.string.developed_by),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(vertical = 5.dp)
                )
                // Navigasjonsbar med ikoner
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        // Sjekk om denne skjermen er aktiv
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
                                // Naviger til valgt skjerm
                                navController.navigate(item.screen.route) {
                                    // Pop til start for å unngå stor back-stack
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Unngå flere kopier av samme destination
                                    launchSingleTop = true
                                    // Gjenopprett state ved tilbakegang
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Hovedinnhold - NavGraph håndterer hvilken skjerm som vises
        NavGraph(
            navController = navController,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * BottomNavItem - Data class som kobler skjerm, ikon og label sammen.
 * Privat fordi den kun brukes internt i denne filen.
 */
private data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)