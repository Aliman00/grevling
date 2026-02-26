package com.grevlingappen.ui.screens.apps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grevlingappen.R
import com.grevlingappen.domain.models.AppInfo
import com.grevlingappen.ui.components.GrevlingHeader
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AppsScreen - Skjerm for å velge hvilke apper som skal overvåkes.
 * 
 * Funksjonalitet:
 * - Viser liste over alle installerte apper på enheten
 * - Søkefilter for å finne spesifikke apper
 * - Filter chips for å veksle mellom alle apper og kun valgte apper
 * - Checkbox på hver rad for å velge/velge bort apper
 * - Asynkron innlasting av app-ikoner (for bedre ytelse)
 * 
 * State-håndtering:
 * - filteredApps: Listen med apper som vises (etter søk og filtrering)
 * - selectedCount: Antall valgte apper
 * - isLoading: Om app-listen lastes inn
 * - showOnlySelected: Om kun valgte apper skal vises
 * - searchQuery: Gjeldende søketekst
 */
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = viewModel()
) {
    // ==================================================================
    // STATE - Observér state fra ViewModel
    // ==================================================================
    // state inneholder alt vi trenger for å vise skjermen:
    // - filteredApps: Apper som vises etter søk/filtrering
    // - selectedCount: Antall valgte apper
    // - isLoading: Om data lastes
    // - showOnlySelected: Om kun valgte vises
    // - searchQuery: Søketekst
    val state by viewModel.state.collectAsState()

    // ==================================================================
    // LAYOUT - Hovedlayout
    // ==================================================================
    Column(modifier = Modifier.fillMaxSize()) {
        // Header med tittel og undertittel
        GrevlingHeader(
            title = stringResource(R.string.app_selection_title),
            subtitle = stringResource(R.string.app_selection_subtitle)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ==================================================================
            // VALGTE APPER - Teller
            // ==================================================================
            // Viser antall valgte apper øverst
            Text(
                text = stringResource(R.string.selected_apps_count, state.selectedCount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==================================================================
            // SØKEFELT - Filter for app-navn
            // ==================================================================
            // Tekstfelt for søk. Når bruker skriver, oppdateres searchQuery i state.
            // ViewModel filtrerer app-listen automatisk basert på søket.
            TextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_apps_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_description)
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==================================================================
            // FILTER CHIPS - Velg visningsmodus
            // ==================================================================
            // To knapper: "Alle apper" og "Valgte apper"
            // "Alle apper" viser alle installerte apper
            // "Valgte apper" viser kun de som er valgt (med checkbox)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !state.showOnlySelected,
                    onClick = { viewModel.toggleShowOnlySelected(false) },
                    label = { Text(stringResource(R.string.chip_all_apps)) }
                )
                FilterChip(
                    selected = state.showOnlySelected,
                    onClick = { viewModel.toggleShowOnlySelected(true) },
                    label = { Text(stringResource(R.string.chip_selected_apps)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==================================================================
            // APP-LISTE - LazyColumn med installerte apper
            // ==================================================================
            // Hvis isLoading er true, vis en progress indicator.
            // Ellers vis en LazyColumn med alle filtrerte apper.
            // Hver rad er en AppListItem med checkbox.
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(
                        items = state.filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppListItem(
                            app = app,
                            onToggle = { viewModel.toggleApp(app) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * AppListItem - En enkelt rad i app-listen.
 * 
 * Viser app-ikon (venstre), app-navn og package-navn (midt), 
 * samt checkbox (høyre) for å velge/velge bort appen.
 * 
 * Hele raden er klikkbar - trykk hvor som helst på raden
 * for å veksle valg-status.
 */
@Composable
private fun AppListItem(
    app: AppInfo,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon - Lastes asynkront
        AppIcon(
            packageName = app.packageName,
            appName = app.appName,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // App-navn - vises i stor skrift
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            // Package-navn - vises i liten skrift med dempet farge
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Checkbox for å velge/velge bort denne appen
        Checkbox(
            checked = app.isSelected,
            onCheckedChange = { onToggle() }
        )
    }
}

/**
 * AppIcon - Laster app-ikon asynkront fra package manager.
 * 
 * Bruker produceState for asynkron lasting av ikonet på bakgrunnstråd
 * (via Dispatchers.IO) for å ikke blokkere UI-tråden.
 * 
 * Returnerer:
 * - Image med app-ikonet hvis det kan lastes
 * - Placeholder (tom boks med sirkulær form) hvis ikonet ikke kan lastes
 */
@Composable
fun AppIcon(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier
) {
    // ==================================================================
    // ASYNKRON IKON-LASTING
    // ==================================================================
    // Bruker produceState for å laste ikonet asynkront.
    // Key (packageName) sikrer at composable re-kjøres når package endres.
    val context = LocalContext.current
    val icon: Drawable? by produceState<Drawable?>(initialValue = null, packageName) {
        // Kjør på IO-dispatcher for å ikke blokkere UI-tråden
        value = withContext(Dispatchers.IO) {
            try {
                // Prøv å hent ikonet fra PackageManager
                context.packageManager.getApplicationIcon(packageName)
            } catch (e: Exception) {
                // Returner null hvis appen ikke finnes eller ikonet ikke kan lastes
                null
            }
        }
    }

    // ==================================================================
    // VIS IKON ELLER PLACEHOLDER
    // ==================================================================
    // Hvis vi har et ikon, vis det. Ellers vis en placeholder-boks.
    if (icon != null) {
        Image(
            painter = rememberDrawablePainter(icon),
            contentDescription = appName,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        )
    }
}