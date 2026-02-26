package com.grevlingappen.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grevlingappen.R
import com.grevlingappen.ui.components.GrevlingButton
import com.grevlingappen.ui.components.GrevlingCard
import com.grevlingappen.ui.components.GrevlingHeader
import com.grevlingappen.ui.components.StatusCircle
import com.grevlingappen.ui.theme.StatusActive
import com.grevlingappen.ui.theme.StatusPaused

/**
 * HomeScreen - Hovedskjerm som viser status og auto-reply innstillinger.
 * 
 * Funksjonalitet:
 * - Viser status sirkel (aktiv/pauset/varsel)
 * - Toggle for å aktivere/deaktivere videresending
 * - Auto-reply konfigurasjon med egne meldinger for SMS og anrop
 * - Auto-refresh når bruker kommer tilbake til appen
 * 
 * State-håndtering:
 * - state.statusColor: Farge for status sirkel (grønn=gul=rød)
 * - state.statusMessage: Tekst som beskriver gjeldende status
 * - state.isFullyActive: Om appen er fullt aktiv (kan videresende)
 * - state.canActivate: Om brukeren kan aktivere videresending
 * - state.autoReplyEnabled: Om auto-svar er skrudd på
 * - state.useSameMessage: Om samme melding brukes for SMS og anrop
 * 
 * Lifecycle:
 * - ON_RESUME: Refresh permissions og state når bruker kommer tilbake
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    // ==================================================================
    // STATE - Observér ViewModel state
    // ==================================================================
    // state inneholder alt vi trenger for å vise skjermen:
    // - statusColor/statusMessage: Gjeldende status
    // - isFullyActive/canActivate: Om videresending er aktiv
    // - autoReplyEnabled: Om auto-svar er på
    // - useSameMessage: Om samme melding brukes
    // - Meldingstekster (unifiedMessage, smsMessage, callMessage)
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // ==================================================================
    // LIFECYCLE - Refresh ved app-gjenopptak
    // ==================================================================
    // Når brukeren kommer tilbake til appen (ON_RESUME),
    // sjekker vi på nytt permissions og oppdaterer state.
    // Dette er viktig fordi brukeren kan ha endret permissions
    // i systeminnstillingene mens appen var i bakgrunnen.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ==================================================================
    // LAYOUT - Hovedlayout med scrolling
    // ==================================================================
    // Column med vertikal scrolling som inneholder alle kort.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header med app-tittel og undertittel
        GrevlingHeader(
            title = stringResource(R.string.app_header_title),
            subtitle = stringResource(R.string.homescreen_subtitle)
        )

        // Kort med 16.dp padding
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================================================================
            // STATUS KORT - Viser status og aktiveringsknapp
            // ==================================================================
            // Inneholder:
            // - Status sirkel med farge (grønn=gul=rød)
            // - Status tekst som forklarer gjeldende tilstand
            // - Aktiver/Deaktiver knapp
            GrevlingCard {
                // Rad med sirkel og tekst
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status sirkel (animert farge basert på tilstand)
                    StatusCircle(status = state.statusColor)

                    // Status tekst - forteller brukeren hva som mangler/forventes
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = state.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Aktiver/Deaktiver knapp - veksler videresending
                // Tekst og farge avhenger av om appen er aktiv:
                // - Hvis aktiv: "Pause" med rød/gul farge
                // - Hvis inaktiv: "Aktiver" med grønn farge
                // Knappen er disabled hvis canActivate er false (manglende config/permissions)
                GrevlingButton(
                    text = if (state.isFullyActive)
                        stringResource(R.string.toggle_button_pause)
                    else
                        stringResource(R.string.toggle_button_activate),
                    onClick = { viewModel.toggleForwarding() },
                    enabled = state.canActivate,
                    backgroundColor = if (state.isFullyActive) StatusPaused else StatusActive,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ==================================================================
            // AUTO-SVAR KORT - Konfigurer auto-svar meldinger
            // ==================================================================
            // Inneholder:
            // - Overskrift med switch for å skru av/på auto-svar
            // - Beskrivelse av hva auto-svar gjør
            // - Toggle for å bruke samme melding til SMS og anrop
            // - Ett eller to tekstfelt for meldinger (avhengig av toggle)
            // - Lagre knapp
            GrevlingCard {
                // Overskrift med switch for å skru av/på auto-svar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Tittel: "Auto-svar"
                        Text(
                            text = stringResource(R.string.toggle_auto_reply),
                            style = MaterialTheme.typography.titleMedium
                        )
                        // Beskrivelse: Hva auto-svar gjør
                        Text(
                            text = stringResource(R.string.auto_reply_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Switch for å skru av/på auto-svar
                    Switch(
                        checked = state.autoReplyEnabled,
                        onCheckedChange = { viewModel.toggleAutoReply(it) }
                    )
                }

                // Auto-svar alternativer - vises kun hvis auto-svar er aktivert
                if (state.autoReplyEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Toggle: "Samme melding til SMS og anrop"
                    // Hvis på: Bruk én felles melding
                    // Hvis av: Separate meldinger for SMS og anrop
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.same_message_toggle),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = state.useSameMessage,
                            onCheckedChange = { viewModel.toggleUseSameMessage(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ==================================================================
                    // MELDINGSFELT(ER) - Tekstfelt for meldinger
                    // ==================================================================
                    // Hvis useSameMessage er true: Vis ett felt for felles melding
                    // Hvis false: Vis to separate felt (SMS og anrop)
                    if (state.useSameMessage) {
                        // Enhetlig melding (samme for SMS og anrop)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.message_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Tekstfelt for felles melding
                        TextField(
                            value = state.unifiedMessage,
                            onValueChange = { viewModel.updateUnifiedMessage(it) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                        // Hint hvis feltet er tomt
                        if (state.unifiedMessage.isBlank()) {
                            Text(
                                text = stringResource(R.string.message_empty_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        // Separate meldinger for SMS og anrop

                        // SMS melding - eget tekstfelt
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.auto_reply_sms_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = state.smsMessage,
                            onValueChange = { viewModel.updateSmsMessage(it) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                        if (state.smsMessage.isBlank()) {
                            Text(
                                text = stringResource(R.string.message_empty_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Anrop melding - eget tekstfelt
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.auto_reply_call_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = state.callMessage,
                            onValueChange = { viewModel.updateCallMessage(it) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                        if (state.callMessage.isBlank()) {
                            Text(
                                text = stringResource(R.string.message_empty_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Lagre knapp - lagrer meldingene til SharedPreferences
                // Kjører saveMessages() i ViewModel som deretter
                // delegerer til PreferencesRepository for faktisk lagring
                GrevlingButton(
                    text = stringResource(R.string.save_messages_button),
                    onClick = { viewModel.saveMessages() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}