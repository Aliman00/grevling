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
 * HomeScreen - Hovedskjerm med status og auto-reply
 *
 * FEATURES:
 * - Status circle (aktiv/pauset)
 * - Toggle forwarding on/off
 * - Auto-reply konfigurasjon
 * - Auto-refresh når bruker kommer tilbake til appen
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    // ------------------------------------------------------------------------
    // STATE - Observér ViewModel state
    // ------------------------------------------------------------------------
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // ------------------------------------------------------------------------
    // LIFECYCLE - Re-sjekk permissions når bruker kommer tilbake
    // ------------------------------------------------------------------------
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh permissions når bruker kommer tilbake til appen
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ------------------------------------------------------------------------
    // COMPUTED VALUES (Fjernet lokal statusText beregning for å bruke state.statusMessage)
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // LAYOUT
    // ------------------------------------------------------------------------
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ... (Header koden er uendret)
        GrevlingHeader(
            title = stringResource(R.string.app_header_title),
            subtitle = stringResource(R.string.homescreen_subtitle)
        )

        // ====================================================================
        // CONTENT
        // ====================================================================
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================================================================
            // STATUS CARD
            // ================================================================
            GrevlingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Circle
                    StatusCircle(status = state.statusColor)

                    // Status Info
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

                // Toggle Button
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

            // ====================================================================
            // AUTO-REPLY CARD
            // ====================================================================
            GrevlingCard {
                // ------------------------------------------------------------
                // AUTO-REPLY HEADER
                // ------------------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.toggle_auto_reply),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.auto_reply_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = state.autoReplyEnabled,
                        onCheckedChange = { viewModel.toggleAutoReply(it) }
                    )
                }

                // ------------------------------------------------------------
                // AUTO-REPLY OPTIONS (vises kun hvis enabled)
                // ------------------------------------------------------------
                if (state.autoReplyEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Same Message Toggle
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

                    // ------------------------------------------------------------
                    // MESSAGE FIELDS
                    // ------------------------------------------------------------
                    if (state.useSameMessage) {
                        // UNIFIED MESSAGE (samme for SMS og anrop)
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
                        TextField(
                            value = state.unifiedMessage,
                            onValueChange = { viewModel.updateUnifiedMessage(it) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                        if (state.unifiedMessage.isBlank()) {
                            Text(
                                text = stringResource(R.string.message_empty_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        // SEPARATE MESSAGES (forskjellig for SMS og anrop)

                        // SMS Message
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

                        // Call Message
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
                
                GrevlingButton(
                    text = stringResource(R.string.save_messages_button),
                    onClick = { viewModel.saveMessages() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
