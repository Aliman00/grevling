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
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class SettingsFragment : BaseFragment() {

    private lateinit var emailEditText: EditText
    private lateinit var gmailAddressEditText: EditText
    private lateinit var gmailPasswordEditText: EditText
    private lateinit var testEmailButton: Button
    private lateinit var notificationButton: Button
    private lateinit var requestPermButton: Button
    private var restrictedSettingsButton: Button? = null
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
        val scrollView = ScrollView(requireContext())
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        prefs = getEncryptedPreferences()

        // Header for innstillinger
        layout.addView(TextView(requireContext()).apply {
            text = "Konto-innstillinger"
            textSize = 18f
            setPadding(0, 0, 0, 20)
        })

        // Mottaker Email
        layout.addView(TextView(requireContext()).apply {
            text = "Mottaker email (hvor varsler sendes):"
            textSize = 14f
        })
        emailEditText = EditText(requireContext()).apply {
            hint = "din@email.no"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            addTextChangedListener(createAutoSaveWatcher(
                onSave = { email -> prefs.edit().putString("email", email).apply() }
            ))
        }
        layout.addView(emailEditText)

        // Gmail adresse
        layout.addView(TextView(requireContext()).apply {
            text = "Gmail-adresse (sender):"
            textSize = 14f
            setPadding(0, 30, 0, 0)
        })
        gmailAddressEditText = EditText(requireContext()).apply {
            hint = "dinbrukernavn@gmail.com"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            addTextChangedListener(createAutoSaveWatcher(
                onSave = { address -> prefs.edit().putString("gmail_address", address).apply() }
            ))
        }
        layout.addView(gmailAddressEditText)

        // Gmail App Password
        layout.addView(TextView(requireContext()).apply {
            text = "Gmail App Password:"
            textSize = 14f
            setPadding(0, 20, 0, 0)
        })
        layout.addView(Button(requireContext()).apply {
            text = "❓ Hvordan få Gmail App Password?"
            textSize = 12f
            setPadding(0, 10, 0, 10)
            setOnClickListener { showAppPasswordGuide() }
        })
        gmailPasswordEditText = EditText(requireContext()).apply {
            hint = "16-tegn App Password (uten mellomrom)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            transformationMethod = PasswordTransformationMethod.getInstance()
            addTextChangedListener(createAutoSaveWatcher(
                onSave = { password -> prefs.edit().putString("gmail_password", password).apply() }
            ))
        }
        layout.addView(gmailPasswordEditText)

        // Test Email knapp
        testEmailButton = Button(requireContext()).apply {
            text = "📧 Send test-email"
            setPadding(0, 20, 0, 0)
            setOnClickListener { sendTestEmail() }
        }
        layout.addView(testEmailButton)

        // Separator
        layout.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
            setBackgroundColor(getColor(R.color.separator))
            setPadding(0, 40, 0, 30)
        })

        // Tillatelser seksjon
        layout.addView(TextView(requireContext()).apply {
            text = "Tillatelser"
            textSize = 18f
            setPadding(0, 0, 0, 20)
        })

        permissionsStatusText = TextView(requireContext()).apply {
            text = "Sjekker tillatelser..."
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }
        layout.addView(permissionsStatusText)

        // Notification button
        notificationButton = Button(requireContext()).apply {
            text = "Gi varseltilgang"
            setPadding(0, 0, 0, 20)
            setOnClickListener { openNotificationSettings() }
        }
        layout.addView(notificationButton)

        // Request permissions button
        requestPermButton = Button(requireContext()).apply {
            text = "Be om SMS/Anrop-tillatelser"
            setPadding(0, 0, 0, 0)
            setOnClickListener { requestPermissions() }
        }
        layout.addView(requestPermButton)

        // Hjelpeknapp for begrensede innstillinger (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            restrictedSettingsButton = Button(requireContext()).apply {
                text = getString(R.string.restricted_settings_button)
                textSize = 12f
                setPadding(0, 30, 0, 0)
                setOnClickListener { showRestrictedSettingsGuide() }
            }
            layout.addView(restrictedSettingsButton)
        }

        scrollView.addView(layout)
        return scrollView
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
        updatePermissionsStatus()
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
        restrictedSettingsButton?.visibility = if (allGranted) View.GONE else View.VISIBLE
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
