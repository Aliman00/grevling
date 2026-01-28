package com.smsforwarder

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

class HomeFragment : BaseFragment() {

    private lateinit var toggleSwitch: Switch
    private lateinit var autoReplySwitch: Switch
    private lateinit var sameMessageSwitch: Switch
    private lateinit var sameMessageContainer: LinearLayout
    private lateinit var unifiedMessageEdit: EditText
    private lateinit var unifiedSavedText: TextView
    private lateinit var separateMessagesContainer: LinearLayout
    private lateinit var smsReplyEdit: EditText
    private lateinit var smsSavedText: TextView
    private lateinit var callReplyEdit: EditText
    private lateinit var callSavedText: TextView
    private lateinit var statusText: TextView
    private lateinit var autoReplyLockButton: Button
    private lateinit var prefs: SharedPreferences
    private var isAutoReplyLocked = true

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
        isAutoReplyLocked = prefs.getBoolean("auto_reply_locked", true)

        // Status
        statusText = TextView(requireContext()).apply {
            text = "Status: Avventer innstillinger..."
            textSize = 18f
            setPadding(0, 0, 0, 60)
        }
        layout.addView(statusText)

        // Varsler toggle
        toggleSwitch = Switch(requireContext()).apply {
            text = "SMS & Anrops-varsler"
            textSize = 16f
            minHeight = 100
            setPadding(0, 20, 0, 20)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("enabled", isChecked).apply()
                updateStatus()
            }
        }
        layout.addView(toggleSwitch)

        // Separator med ekstra spacing
        layout.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
            setBackgroundColor(getColor(R.color.separator))
            setPadding(0, 70, 0, 70)
        })

        // Auto-reply toggle
        autoReplySwitch = Switch(requireContext()).apply {
            text = "Auto-svar"
            textSize = 16f
            minHeight = 100
            setPadding(0, 20, 0, 20)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("auto_reply_enabled", isChecked).apply()
                updateAutoReplyVisibility()
            }
        }
        layout.addView(autoReplySwitch)

        autoReplyLockButton = Button(requireContext()).apply {
            text = "🔒 Lås opp auto-svar"
            textSize = 12f
            minHeight = 100
            setPadding(0, 25, 0, 25)
            setOnClickListener { toggleAutoReplyLock() }
        }
        layout.addView(autoReplyLockButton)

        // Same message toggle
        sameMessageSwitch = Switch(requireContext()).apply {
            text = "Bruk samme melding for SMS og anrop"
            textSize = 14f
            minHeight = 90
            setPadding(50, 30, 0, 30)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("use_same_message", isChecked).apply()
                updateAutoReplyVisibility()
            }
        }
        layout.addView(sameMessageSwitch)

        // Unified message container
        sameMessageContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 0, 40)
        }
        sameMessageContainer.addView(TextView(requireContext()).apply {
            text = "Melding:"
            textSize = 12f
            setPadding(0, 0, 0, 10)
            setTextColor(getColor(R.color.text_secondary))
        })

        unifiedSavedText = TextView(requireContext()).apply {
            text = "✓ Lagret"
            textSize = 11f
            setTextColor(getColor(R.color.text_success))
            visibility = View.GONE
        }

        unifiedMessageEdit = EditText(requireContext()).apply {
            minLines = 3
            maxLines = 5
            setPadding(20, 20, 20, 20)
            addTextChangedListener(createAutoSaveWatcher(
                savedIndicator = unifiedSavedText,
                shouldSave = { !isAutoReplyLocked },
                onSave = { msg -> prefs.edit().putString("unified_reply_message", msg).apply() },
                onAfterSave = { updateStatus() }
            ))
        }
        sameMessageContainer.addView(unifiedMessageEdit)
        sameMessageContainer.addView(unifiedSavedText)
        layout.addView(sameMessageContainer)

        // Separate messages container
        separateMessagesContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 0, 40)
        }

        separateMessagesContainer.addView(TextView(requireContext()).apply {
            text = "Auto-svar for SMS:"
            textSize = 12f
            setPadding(0, 0, 0, 10)
            setTextColor(getColor(R.color.text_secondary))
        })

        smsSavedText = TextView(requireContext()).apply {
            text = "✓ Lagret"
            textSize = 11f
            setTextColor(getColor(R.color.text_success))
            visibility = View.GONE
        }

        smsReplyEdit = EditText(requireContext()).apply {
            minLines = 3
            maxLines = 5
            setPadding(20, 20, 20, 20)
            addTextChangedListener(createAutoSaveWatcher(
                savedIndicator = smsSavedText,
                shouldSave = { !isAutoReplyLocked },
                onSave = { msg -> prefs.edit().putString("sms_reply_message", msg).apply() },
                onAfterSave = { updateStatus() }
            ))
        }
        separateMessagesContainer.addView(smsReplyEdit)
        separateMessagesContainer.addView(smsSavedText)

        separateMessagesContainer.addView(TextView(requireContext()).apply {
            text = "Auto-svar for tapt anrop:"
            textSize = 12f
            setPadding(0, 40, 0, 10)
            setTextColor(getColor(R.color.text_secondary))
        })

        callSavedText = TextView(requireContext()).apply {
            text = "✓ Lagret"
            textSize = 11f
            setTextColor(getColor(R.color.text_success))
            visibility = View.GONE
        }

        callReplyEdit = EditText(requireContext()).apply {
            minLines = 3
            maxLines = 5
            setPadding(20, 20, 20, 20)
            addTextChangedListener(createAutoSaveWatcher(
                savedIndicator = callSavedText,
                shouldSave = { !isAutoReplyLocked },
                onSave = { msg -> prefs.edit().putString("call_reply_message", msg).apply() },
                onAfterSave = { updateStatus() }
            ))
        }
        separateMessagesContainer.addView(callReplyEdit)
        separateMessagesContainer.addView(callSavedText)
        layout.addView(separateMessagesContainer)

        scrollView.addView(layout)
        return scrollView
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
        updateAutoReplyLockState()
        updateAutoReplyVisibility()
        updateStatus()
    }

    private fun updateAutoReplyVisibility() {
        val autoReplyEnabled = prefs.getBoolean("auto_reply_enabled", false)
        val useSameMessage = prefs.getBoolean("use_same_message", true)

        sameMessageSwitch.visibility = if (autoReplyEnabled) View.VISIBLE else View.GONE
        autoReplyLockButton.visibility = if (autoReplyEnabled) View.VISIBLE else View.GONE

        if (autoReplyEnabled) {
            if (useSameMessage) {
                sameMessageContainer.visibility = View.VISIBLE
                separateMessagesContainer.visibility = View.GONE
            } else {
                sameMessageContainer.visibility = View.GONE
                separateMessagesContainer.visibility = View.VISIBLE
            }
        } else {
            sameMessageContainer.visibility = View.GONE
            separateMessagesContainer.visibility = View.GONE
        }
    }

    private fun toggleAutoReplyLock() {
        isAutoReplyLocked = !isAutoReplyLocked
        prefs.edit().putBoolean("auto_reply_locked", isAutoReplyLocked).apply()
        updateAutoReplyLockState()
    }

    private fun updateAutoReplyLockState() {
        if (isAutoReplyLocked) {
            autoReplyLockButton.text = getString(R.string.lock_auto_reply)
            unifiedMessageEdit.isEnabled = false
            smsReplyEdit.isEnabled = false
            callReplyEdit.isEnabled = false
            unifiedMessageEdit.setBackgroundColor(getColor(R.color.background_locked))
            smsReplyEdit.setBackgroundColor(getColor(R.color.background_locked))
            callReplyEdit.setBackgroundColor(getColor(R.color.background_locked))
        } else {
            autoReplyLockButton.text = getString(R.string.unlock_auto_reply)
            unifiedMessageEdit.isEnabled = true
            smsReplyEdit.isEnabled = true
            callReplyEdit.isEnabled = true
            unifiedMessageEdit.setBackgroundColor(Color.WHITE)
            smsReplyEdit.setBackgroundColor(Color.WHITE)
            callReplyEdit.setBackgroundColor(Color.WHITE)
        }
    }

    private fun loadSettings() {
        toggleSwitch.isChecked = prefs.getBoolean("enabled", false)
        autoReplySwitch.isChecked = prefs.getBoolean("auto_reply_enabled", false)
        sameMessageSwitch.isChecked = prefs.getBoolean("use_same_message", true)

        val unifiedMsg = prefs.getString("unified_reply_message", "")
        if (unifiedMsg.isNullOrEmpty()) {
            val defaultMsg = getString(R.string.default_unified_message)
            prefs.edit().putString("unified_reply_message", defaultMsg).apply()
            unifiedMessageEdit.setText(defaultMsg)
        } else {
            unifiedMessageEdit.setText(unifiedMsg)
        }

        val smsMsg = prefs.getString("sms_reply_message", "")
        if (smsMsg.isNullOrEmpty()) {
            val defaultMsg = getString(R.string.default_sms_message)
            prefs.edit().putString("sms_reply_message", defaultMsg).apply()
            smsReplyEdit.setText(defaultMsg)
        } else {
            smsReplyEdit.setText(smsMsg)
        }

        val callMsg = prefs.getString("call_reply_message", "")
        if (callMsg.isNullOrEmpty()) {
            val defaultMsg = getString(R.string.default_call_message)
            prefs.edit().putString("call_reply_message", defaultMsg).apply()
            callReplyEdit.setText(defaultMsg)
        } else {
            callReplyEdit.setText(callMsg)
        }
    }

    private fun updateStatus() {
        val enabled = prefs.getBoolean("enabled", false)
        // getString() med default value returnerer aldri null
        val hasGmailAddress = prefs.getString("gmail_address", "")?.isNotEmpty() == true
        val hasGmailPassword = prefs.getString("gmail_password", "")?.isNotEmpty() == true
        val hasRecipientEmail = prefs.getString("email", "")?.isNotEmpty() == true
        val hasNotificationAccess = NotificationHelper.isNotificationServiceEnabled(requireContext())

        when {
            !hasNotificationAccess -> {
                statusText.text = getString(R.string.status_needs_notification)
                statusText.setTextColor(getColor(R.color.text_warning))
            }
            !hasGmailAddress || !hasGmailPassword || !hasRecipientEmail -> {
                statusText.text = getString(R.string.status_missing_config)
                statusText.setTextColor(getColor(R.color.text_warning))
            }
            enabled -> {
                statusText.text = getString(R.string.status_active)
                statusText.setTextColor(getColor(R.color.text_success))
            }
            else -> {
                statusText.text = getString(R.string.status_paused)
                statusText.setTextColor(getColor(R.color.text_disabled))
            }
        }
    }

}
