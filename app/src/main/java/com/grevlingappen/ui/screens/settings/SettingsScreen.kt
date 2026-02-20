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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

@Composable
fun SettingsScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observer UI State fra ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Lokale dialog-states (UI-spesifikt)
    val showAppPasswordDialog = remember { mutableStateOf(false) }
    val showRestrictedSettingsDialog = remember { mutableStateOf(false) }
    val showBatteryDialog = remember { mutableStateOf(false) }
    val showPermissionRationaleDialog = remember { mutableStateOf(false) }
    val showNotificationRationaleDialog = remember { mutableStateOf(false) }

    // ==================================================================
    // SNACKBAR - Vis testresultat som popup
    // ==================================================================
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
    // LIFECYCLE - Oppdater permissions når appen gjenopptas
    // ==================================================================
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ==================================================================
    // PERMISSIONS LAUNCHER
    // ==================================================================
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkPermissions()
    }

    // ==================================================================
    // DIALOGS
    // ==================================================================
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
    // LAYOUT
    // ==================================================================
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
            // ACCOUNT SETTINGS CARD
            // ============================================================
            GrevlingCard {
                Text(
                    text = stringResource(R.string.settings_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recipient Email
                TextField(
                    value = uiState.recipientEmail,
                    onValueChange = { viewModel.updateRecipientEmail(it) },
                    label = { Text(stringResource(R.string.recipient_email_label)) },
                    placeholder = { Text(stringResource(R.string.email_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Gmail Address
                TextField(
                    value = uiState.gmailAddress,
                    onValueChange = { viewModel.updateGmailAddress(it) },
                    label = { Text(stringResource(R.string.gmail_address_label)) },
                    placeholder = { Text(stringResource(R.string.gmail_address_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Gmail Password
                TextField(
                    value = uiState.gmailPassword,
                    onValueChange = { viewModel.updateGmailPassword(it) },
                    label = { Text(stringResource(R.string.gmail_password_label)) },
                    placeholder = {
                        Text(
                            if (uiState.hasGmailPassword && uiState.gmailPassword.isEmpty()) "••••••••••••••••"
                            else stringResource(R.string.gmail_password_hint)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                // App Password Help
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

                // Test Email Button
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
            // PERMISSIONS CARD
            // ============================================================
            GrevlingCard {
                Text(
                    text = stringResource(R.string.permissions_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Permissions Status
                val statusText = buildString {
                    if (uiState.hasNotificationAccess) {
                        append(stringResource(R.string.permissions_status_notification_ok) + "\n")
                    } else {
                        append(stringResource(R.string.permissions_status_notification_missing) + "\n")
                    }

                    if (uiState.hasAllPermissions) {
                        append(stringResource(R.string.permissions_status_sms_ok) + "\n")
                    } else {
                        append(stringResource(R.string.permissions_status_sms_missing) + "\n")
                    }

                    if (uiState.isIgnoringBatteryOptimizations) {
                        append(stringResource(R.string.permissions_status_battery_ok))
                    } else {
                        append(stringResource(R.string.permissions_status_battery_missing))
                    }
                }

                Text(
                    text = statusText,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.hasNotificationAccess && uiState.hasAllPermissions && uiState.isIgnoringBatteryOptimizations)
                        StatusSuccess
                    else
                        StatusWarning
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Notification Button (skjul hvis granted)
                if (!uiState.hasNotificationAccess) {
                    GrevlingButton(
                        text = stringResource(R.string.notification_access_button),
                        onClick = { showNotificationRationaleDialog.value = true },
                        backgroundColor = StatusWarning,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Request Permissions Button (skjul hvis granted)
                if (!uiState.hasAllPermissions) {
                    GrevlingButton(
                        text = stringResource(R.string.request_permissions_button),
                        onClick = { showPermissionRationaleDialog.value = true },
                        backgroundColor = StatusWarning,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Battery Optimization Button (skjul hvis allerede ignorert)
                if (!uiState.isIgnoringBatteryOptimizations) {
                    Spacer(modifier = Modifier.height(8.dp))
                    GrevlingButton(
                        text = stringResource(R.string.battery_optimization_button),
                        onClick = { showBatteryDialog.value = true },
                        backgroundColor = StatusWarning,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Restricted Settings Help - KUN hvis Android 13+ OG noe mangler
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
