package com.grevlingappen.ui.screens.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grevlingappen.R
import com.grevlingappen.ui.components.GrevlingButton
import com.grevlingappen.ui.components.GrevlingCard
import com.grevlingappen.ui.components.GrevlingHeader
import com.grevlingappen.ui.theme.StatusError
import com.grevlingappen.ui.theme.StatusSuccess
import com.grevlingappen.ui.theme.StatusWarning
import com.grevlingappen.utils.Logger
import com.grevlingappen.utils.PermissionsHelper

/**
 * SettingsScreen - Innstillinger-skjerm for e-post og tilganger.
 * 
 * Funksjonalitet:
 * - E-post konfigurasjon (mottaker, Gmail-addresse, app-passord)
 * - Test e-post for å validere oppsett
 * - Tilgangsstatus og behov for permissions
 * - Battery optimization håndtering
 * 
 * Lifecycle-håndtering:
 * - Sjekker permissions når appen gjenopptas (ON_RESUME)
 * - Lagrer alle endringer når appen pauses (ON_PAUSE)
 * - Lagrer også ved navigasjon bort fra skjermen
 * 
 * Dialoger:
 * - App password dialog: Forklarer hva app-passord er og hvordan få det
 * - Restricted settings dialog: For Android 13+ der man må skru på tilganger manuelt
 * - Battery dialog: Ber bruker om å ignorere battery optimization
 * - Permission rationale dialog: Forklarer hvorfor SMS/telefon-tilgang er nødvendig
 * - Notification rationale dialog: Forklarer hvorfor notification access er nødvendig
 */
@Composable
fun SettingsScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel = viewModel()
) {
    // Context og lifecycle-eier for Compose
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ==================================================================
    // UI STATE - Observér state fra ViewModel
    // ==================================================================
    // uiState inneholder alle data som trengs for å vise skjermen:
    // - E-postadresser (mottaker og Gmail)
    // - Om Gmail-passord er satt
    // - Permissions-status (notification, SMS, battery)
    // - Om test-e-post sendes
    // - Resultat fra test-e-post (suksess/feil)
    val uiState by viewModel.uiState.collectAsState()

    // ==================================================================
    // LOKALE DIALOG-STATES - UI-spesifikk tilstands-håndtering
    // ==================================================================
    // Disse holder styr på om hver dialog skal vises.
    // Bruker remember for å bevare verdien ved recomposition.
    val showAppPasswordDialog = remember { mutableStateOf(false) }
    val showRestrictedSettingsDialog = remember { mutableStateOf(false) }
    val showBatteryDialog = remember { mutableStateOf(false) }
    val showPermissionRationaleDialog = remember { mutableStateOf(false) }
    val showNotificationRationaleDialog = remember { mutableStateOf(false) }

    // ==================================================================
    // PASSORD-INPUT - Lokal state for passord-feltet
    // ==================================================================
    // Trenger egen state fordi vi vil vise •••••••••••••••• når passord er satt,
    // men la feltet være tomt ved første visning. ViewModel lagrer det faktiske passordet.
    var passwordInput by remember { mutableStateOf("") }

    // ==================================================================
    // SNACKBAR - Vis testresultat som popup
    // ==================================================================
    // Testresultat kan komme fra stringResource (testEmailResultRes) eller
    // fra en custom string (testEmailResultCustom). Vi viser det som en snackbar.
    // LaunchedEffect reagerer på endringer i testResultMessage og viser snackbaren.
    val testResultMessage = when {
        uiState.testEmailResultRes != 0 -> stringResource(uiState.testEmailResultRes)
        uiState.testEmailResultCustom.isNotEmpty() -> uiState.testEmailResultCustom
        else -> ""
    }

    LaunchedEffect(testResultMessage) {
        if (testResultMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(testResultMessage)
            viewModel.clearTestResult()
        }
    }

    // ==================================================================
    // NAVIGASJONS-EVENTS - Åpne systeminnstillinger
    // ==================================================================
    // ViewModel kan sende intents for å åpne systeminnstillinger
    // (f.eks. notification settings, battery settings)
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { intent ->
            context.startActivity(intent)
        }
    }

    // ==================================================================
    // LIFECYCLE - Oppdater permissions når appen gjenopptas, lagre når den pauses
    // ==================================================================
    // ON_RESUME: Sjekk om permissions fortsatt er gyldige (bruker kan ha endret dem i systeminnstillinger)
    // ON_PAUSE: Lagre alle endringer (f.eks. når bruker trykker Hjem-knappen)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.checkPermissions()
                Lifecycle.Event.ON_PAUSE -> viewModel.flushAll() // Lagre hvis appen pauses (Hjem-knapp etc)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.flushAll() // Lagre når skjermen forlates (navigasjon)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ==================================================================
    // PERMISSIONS LAUNCHER - Håndterer systemdialoger for permissions
    // ==================================================================
    // Launcher for å be om SMS/telefon-tilganger fra bruker.
    // Etter at dialogen er ferdig, sjekker vi på nytt hvilke permissions som er gitt.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkPermissions()
    }

    // ==================================================================
    // DIALOGS - Popup-vinduer for ulike formål
    // ==================================================================
    // Disse dialogene vises kun når den tilhørende boolean-verdien er sann.
    // De fleste dialogene forklarer hvorfor en tillatelse er nødvendig før vi ber om den.
    
    // Dialog: Hjelp til app-passord (hva det er og hvordan få det)
    if (showAppPasswordDialog.value) {
        AlertDialog(
            onDismissRequest = { showAppPasswordDialog.value = false },
            title = { Text(stringResource(R.string.app_password_dialog_title)) },
            text = {
                Text(text = stringResource(R.string.app_password_dialog_content))
            },
            confirmButton = {
                TextButton(onClick = { showAppPasswordDialog.value = false }) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }

    if (showRestrictedSettingsDialog.value) {
        AlertDialog(
            onDismissRequest = { showRestrictedSettingsDialog.value = false },
            title = { Text(stringResource(R.string.restricted_settings_title)) },
            text = {
                Text(text = stringResource(R.string.restricted_settings_guide))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestrictedSettingsDialog.value = false
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Logger.e("SettingsScreen", "Kunne ikke åpne app-innstillinger")
                        }
                    }
                ) {
                    Text(stringResource(R.string.open_app_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestrictedSettingsDialog.value = false }) {
                    Text(stringResource(R.string.permission_rationale_cancel))
                }
            }
        )
    }

    if (showBatteryDialog.value) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog.value = false },
            title = { Text(stringResource(R.string.battery_rationale_title)) },
            text = {
                Text(text = stringResource(R.string.battery_rationale_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryDialog.value = false
                        viewModel.requestIgnoreBatteryOptimizations()
                    }
                ) {
                    Text(stringResource(R.string.battery_rationale_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDialog.value = false }) {
                    Text(stringResource(R.string.permission_rationale_cancel))
                }
            }
        )
    }

    if (showPermissionRationaleDialog.value) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog.value = false },
            title = { Text(stringResource(R.string.permission_rationale_title)) },
            text = {
                Text(text = stringResource(R.string.permission_rationale_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionRationaleDialog.value = false
                        val permissionsToRequest = PermissionsHelper.requiredPermissions.filter {
                            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                        }

                        if (permissionsToRequest.isNotEmpty()) {
                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
                        }
                    }
                ) {
                    Text(stringResource(R.string.permission_rationale_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationaleDialog.value = false }) {
                    Text(stringResource(R.string.permission_rationale_cancel))
                }
            }
        )
    }

    if (showNotificationRationaleDialog.value) {
        AlertDialog(
            onDismissRequest = { showNotificationRationaleDialog.value = false },
            title = { Text(stringResource(R.string.notification_rationale_title)) },
            text = {
                Text(text = stringResource(R.string.notification_rationale_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationRationaleDialog.value = false
                        viewModel.openNotificationSettings()
                    }
                ) {
                    Text(stringResource(R.string.notification_rationale_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationRationaleDialog.value = false }) {
                    Text(stringResource(R.string.permission_rationale_cancel))
                }
            }
        )
    }

    // ==================================================================
    // LAYOUT - Hovedlayout med scroll og kort
    // ==================================================================
    // Hoved-column med vertikal scrolling.
    // Inneholder to kort: Account Settings og Permissions.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        GrevlingHeader(
            title = stringResource(R.string.settings_screen_title),
            subtitle = stringResource(R.string.settings_screen_subtitle)
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ============================================================
            // ACCOUNT SETTINGS CARD - E-post konfigurasjon
            // ============================================================
            // Inneholder:
            // - Mottaker e-post (hvem skal motta videresendt innhold?)
            // - Gmail-addresse (avsender)
            // - App password (for autentisering mot Gmail)
            // - Test e-post knapp
            GrevlingCard {
                Text(
                    text = stringResource(R.string.settings_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recipient Email - Hvem skal motta videresendt innhold?
                OutlinedTextField(
                    value = uiState.recipientEmail,
                    onValueChange = { viewModel.updateRecipientEmail(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusEvent { focusState ->
                            if (!focusState.isFocused) viewModel.flushRecipientEmail()
                        },
                    label = { Text(stringResource(R.string.recipient_email_label)) },
                    placeholder = { Text(stringResource(R.string.email_hint)) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Gmail Address - E-postadressen som brukes som avsender
                OutlinedTextField(
                    value = uiState.gmailAddress,
                    onValueChange = { viewModel.updateGmailAddress(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusEvent { focusState ->
                            if (!focusState.isFocused) viewModel.flushGmailAddress()
                        },
                    label = { Text(stringResource(R.string.gmail_address_label)) },
                    placeholder = { Text(stringResource(R.string.gmail_address_hint)) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Gmail Password - App-spesifikt passord for Gmail-autentisering
                // Bruker PasswordVisualTransformation for å skjule passordet
                // Viser •••••••••••••••• hvis passordet allerede er satt
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it; viewModel.updateGmailPassword(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusEvent { focusState ->
                            if (!focusState.isFocused) viewModel.flushGmailPassword()
                        },
                    label = { Text(stringResource(R.string.gmail_password_label)) },
                    placeholder = {
                        Text(
                            if (uiState.hasGmailPassword && passwordInput.isEmpty()) "••••••••••••••••"
                            else stringResource(R.string.gmail_password_hint)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                // App Password Help - Lenke til hjelp om hvordan få app-passord
                // Vises kun hvis passordet ikke er satt
                if (!uiState.hasGmailPassword) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.app_password_help_button),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showAppPasswordDialog.value = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Test Email Button - Sender en test-e-post for å validere oppsettet
                // Viser "Sender..." mens e-posten sendes, disables da knappen
                GrevlingButton(
                    text = if (uiState.isSendingTestEmail)
                        stringResource(R.string.test_email_sending)
                    else
                        stringResource(R.string.test_email_button),
                    onClick = { viewModel.testEmail() },
                    enabled = !uiState.isSendingTestEmail,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ============================================================
            // PERMISSIONS CARD - Status og requests for Android-tilganger
            // ============================================================
            // Viser status for:
            // - Notification access (for å lese varsler)
            // - SMS/Telefon (for å lese SMS og anropslogg)
            // - Battery optimization (for å holde appen aktiv)
            // Knapper for å be om manglende tilganger vises kun hvis nødvendig.
            GrevlingCard {
                Text(
                    text = stringResource(R.string.permissions_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Permissions Status - Viser status for hver tillatelse
                // Grønn (StatusSuccess) hvis gitt, gul (StatusWarning) hvis mangler
                
                // 1. Notification access - for å lese varsler fra andre apper
                Text(
                    text = stringResource(
                        if (uiState.hasNotificationAccess) R.string.permissions_status_notification_ok
                        else R.string.permissions_status_notification_missing
                    ),
                    color = if (uiState.hasNotificationAccess) StatusSuccess else StatusWarning,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // 2. SMS/Telefon - for å lese SMS og anropslogg
                Text(
                    text = stringResource(
                        if (uiState.hasAllPermissions) R.string.permissions_status_sms_ok
                        else R.string.permissions_status_sms_missing
                    ),
                    color = if (uiState.hasAllPermissions) StatusSuccess else StatusWarning,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // 3. Battery optimization - for å holde appen aktiv i bakgrunnen
                Text(
                    text = stringResource(
                        if (uiState.isIgnoringBatteryOptimizations) R.string.permissions_status_battery_ok
                        else R.string.permissions_status_battery_missing
                    ),
                    color = if (uiState.isIgnoringBatteryOptimizations) StatusSuccess else StatusWarning,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Notification Button - Ber om notification access
                // Vises kun hvis brukeren ikke allerede har gitt denne tilgangen
                if (!uiState.hasNotificationAccess) {
                    GrevlingButton(
                        text = stringResource(R.string.notification_access_button),
                        onClick = { showNotificationRationaleDialog.value = true },
                        backgroundColor = StatusWarning,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Request Permissions Button - Ber om SMS og telefon-tilganger
                // Vises kun hvis brukeren ikke allerede har gitt disse tilgangene
                if (!uiState.hasAllPermissions) {
                    GrevlingButton(
                        text = stringResource(R.string.request_permissions_button),
                        onClick = { showPermissionRationaleDialog.value = true },
                        backgroundColor = StatusWarning,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Battery Optimization Button - Ber bruker om å ignorere battery optimization
                // Dette er nødvendig for at appen skal kunne kjøre i bakgrunnen
                // Vises kun hvis brukeren ikke allerede har ignorert dette
                if (!uiState.isIgnoringBatteryOptimizations) {
                    Spacer(modifier = Modifier.height(8.dp))
                    GrevlingButton(
                        text = stringResource(R.string.battery_optimization_button),
                        onClick = { showBatteryDialog.value = true },
                        backgroundColor = StatusWarning,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Restricted Settings Help - Kun for Android 13+ (TIRAMISU)
                // På Android 13+ kan ikke appen be om visse tilganger direkte.
                // Brukeren må skru dem på manuelt i app-innstillingene.
                // Vises kun hvis Android 13+ OG minst én tilgang mangler.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                    (!uiState.hasAllPermissions || !uiState.hasNotificationAccess)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.restricted_settings_help_link),
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusError,
                        modifier = Modifier.clickable { showRestrictedSettingsDialog.value = true }
                    )
                }
            }
        }
    }
}
