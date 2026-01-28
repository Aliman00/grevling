package com.smsforwarder

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SettingsFragment : BaseFragment() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var gmailAddressEditText: TextInputEditText
    private lateinit var gmailPasswordEditText: TextInputEditText
    private lateinit var appPasswordHelpButton: MaterialButton
    private lateinit var testEmailButton: MaterialButton
    private lateinit var notificationButton: MaterialButton
    private lateinit var requestPermButton: MaterialButton
    private lateinit var restrictedSettingsButton: MaterialButton
    private lateinit var permissionsStatusText: TextView
    private lateinit var prefs: SharedPreferences

    // Modern permissions API (erstatter deprecated onRequestPermissionsResult)
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        updatePermissionsStatus()

        val deniedCount = permissions.values.count { !it }
        if (deniedCount > 0) {
            Toast.makeText(
                requireContext(),
                getString(R.string.permissions_denied_toast, deniedCount),
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.permissions_granted_toast),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        
        prefs = getEncryptedPreferences()

        // Bind views
        emailEditText = view.findViewById(R.id.recipientEmailEdit)
        gmailAddressEditText = view.findViewById(R.id.gmailAddressEdit)
        gmailPasswordEditText = view.findViewById(R.id.gmailPasswordEdit)
        appPasswordHelpButton = view.findViewById(R.id.appPasswordHelpButton)
        testEmailButton = view.findViewById(R.id.testEmailButton)
        notificationButton = view.findViewById(R.id.notificationButton)
        requestPermButton = view.findViewById(R.id.requestPermButton)
        restrictedSettingsButton = view.findViewById(R.id.restrictedSettingsButton)
        permissionsStatusText = view.findViewById(R.id.permissionsStatusText)

        // Setup listeners
        emailEditText.addTextChangedListener(createAutoSaveWatcher(
            onSave = { email -> prefs.edit().putString("email", email).apply() }
        ))

        gmailAddressEditText.addTextChangedListener(createAutoSaveWatcher(
            onSave = { address -> prefs.edit().putString("gmail_address", address).apply() }
        ))

        gmailPasswordEditText.addTextChangedListener(createAutoSaveWatcher(
            onSave = { password -> prefs.edit().putString("gmail_password", password).apply() }
        ))

        appPasswordHelpButton.setOnClickListener { showAppPasswordGuide() }
        testEmailButton.setOnClickListener { sendTestEmail() }
        notificationButton.setOnClickListener { openNotificationSettings() }
        requestPermButton.setOnClickListener { requestPermissions() }
        restrictedSettingsButton.setOnClickListener { showRestrictedSettingsGuide() }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
        updatePermissionsStatus()
        updateHelpButtonsVisibility()
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
        Toast.makeText(requireContext(), getString(R.string.notification_settings_toast), Toast.LENGTH_LONG).show()
    }

    private fun loadSettings() {
        emailEditText.setText(prefs.getString("email", ""))
        gmailAddressEditText.setText(prefs.getString("gmail_address", ""))
        gmailPasswordEditText.setText(prefs.getString("gmail_password", ""))
    }

    private fun sendTestEmail() {
        val recipientEmail = emailEditText.text.toString().trim()
        val gmailAddress = gmailAddressEditText.text.toString().trim()
        val gmailPassword = gmailPasswordEditText.text.toString().trim()

        // Valider at felter ikke er tomme
        if (recipientEmail.isEmpty() || gmailAddress.isEmpty() || gmailPassword.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.test_email_error_empty),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Valider email-format
        if (!Patterns.EMAIL_ADDRESS.matcher(recipientEmail).matches()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.test_email_error_invalid_recipient),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(gmailAddress).matches()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.test_email_error_invalid_gmail),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Sjekk om det er Gmail eller Google Workspace
        val isGmail = gmailAddress.lowercase().endsWith("@gmail.com")
        val isGoogleWorkspace = gmailAddress.contains("@") && !isGmail

        if (isGoogleWorkspace) {
            // Advarsel men tillat fortsettelse for Google Workspace
            Toast.makeText(
                requireContext(),
                getString(R.string.test_email_warning_workspace),
                Toast.LENGTH_LONG
            ).show()
        }

        testEmailButton.isEnabled = false
        testEmailButton.text = getString(R.string.test_email_sending)

        EmailSender.testEmailConfig(
            gmailAddress,
            gmailPassword,
            recipientEmail
        ) { success, message ->
            // Bruk activity?.runOnUiThread for null-safety
            activity?.runOnUiThread {
                // Sjekk at fragment fortsatt er attached og view er tilgjengelig
                if (!isAdded || view == null) return@runOnUiThread

                testEmailButton.isEnabled = true
                testEmailButton.text = getString(R.string.test_email_button)

                Toast.makeText(
                    requireContext(),
                    message,
                    if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updatePermissionsStatus() {
        val hasNotificationAccess = NotificationHelper.isNotificationServiceEnabled(requireContext())

        // Tell tillatelsesgrupper (slik brukeren ser dem i Android)
        val hasSmsPermissions = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val hasCallLogPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        val hasContactsPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val permissionGroups = listOf(hasSmsPermissions, hasCallLogPermission, hasContactsPermission)
        val grantedGroupsCount = permissionGroups.count { it }
        val allPermsGranted = grantedGroupsCount == 3

        val statusText = buildString {
            if (hasNotificationAccess) {
                append("✅ Varseltilgang: OK\n")
            } else {
                append("❌ Varseltilgang: Mangler\n")
            }

            if (allPermsGranted) {
                append("✅ SMS/Anrop tillatelser: OK")
            } else {
                append("❌ SMS/Anrop tillatelser: $grantedGroupsCount/3")
            }
        }

        permissionsStatusText.text = statusText
        permissionsStatusText.setTextColor(
            if (hasNotificationAccess && allPermsGranted) {
                getColor(R.color.text_success)
            } else {
                getColor(R.color.text_warning)
            }
        )

        // Skjul knapper hvis tillatelser er gitt
        notificationButton.visibility = if (hasNotificationAccess) View.GONE else View.VISIBLE
        requestPermButton.visibility = if (allPermsGranted) View.GONE else View.VISIBLE
        
        // Skjul hjelpeknapp for begrensede innstillinger hvis alle tillatelser er gitt
        val allGranted = hasNotificationAccess && allPermsGranted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            restrictedSettingsButton.visibility = if (allGranted) View.GONE else View.VISIBLE
        } else {
            restrictedSettingsButton.visibility = View.GONE
        }
    }

    private fun updateHelpButtonsVisibility() {
        // Skjul "Hvordan få Gmail App Password?" hvis passord allerede er satt
        val hasPassword = prefs.getString("gmail_password", "")?.length == 16
        appPasswordHelpButton.visibility = if (hasPassword) View.GONE else View.VISIBLE
    }

    private fun showAppPasswordGuide() {
        val guideText = """
            📱 Slik får du Gmail App Password:

            1️⃣ Aktiver 2-faktor autentisering (2FA)
               • Gå til myaccount.google.com/security
               • Velg "2-Step Verification"
               • Følg instruksjonene

            2️⃣ Opprett App Password
               • Gå til myaccount.google.com/apppasswords
               • Velg "Mail" som app
               • Velg "Other" som enhet
               • Skriv "SMS Forwarder"
               • Klikk "Generate"

            3️⃣ Kopier passordet
               • Google viser et 16-tegn passord
               • Kopier det (fjern mellomrom)
               • Lim det inn i "Gmail App Password"-feltet

            4️⃣ Test konfigurasjonen
               • Trykk "📧 Send test-email"
               • Sjekk at du mottar emailen

            ⚠️ VIKTIG: Du MÅ ha aktivert 2FA på Google-kontoen først!
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Hvordan få Gmail App Password")
            .setMessage(guideText)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun requestPermissions() {
        val permissionsToRequest = PermissionsHelper.REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.permissions_already_granted),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showRestrictedSettingsGuide() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.restricted_settings_title))
            .setMessage(getString(R.string.restricted_settings_guide))
            .setPositiveButton(getString(R.string.open_app_settings)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }
}
