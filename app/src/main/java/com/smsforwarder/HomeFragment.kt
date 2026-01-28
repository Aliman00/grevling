package com.smsforwarder

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText

class HomeFragment : BaseFragment() {

    private lateinit var statusCard: MaterialCardView
    private lateinit var statusText: TextView
    private lateinit var toggleSwitch: MaterialSwitch
    private lateinit var autoReplySwitch: MaterialSwitch
    private lateinit var autoReplyOptionsContainer: LinearLayout
    private lateinit var autoReplyLockButton: MaterialButton
    private lateinit var sameMessageSwitch: MaterialSwitch
    private lateinit var sameMessageContainer: LinearLayout
    private lateinit var unifiedMessageEdit: TextInputEditText
    private lateinit var unifiedSavedText: TextView
    private lateinit var separateMessagesContainer: LinearLayout
    private lateinit var smsReplyEdit: TextInputEditText
    private lateinit var smsSavedText: TextView
    private lateinit var callReplyEdit: TextInputEditText
    private lateinit var callSavedText: TextView
    private lateinit var prefs: SharedPreferences
    private var isAutoReplyLocked = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        prefs = getEncryptedPreferences()
        isAutoReplyLocked = prefs.getBoolean("auto_reply_locked", true)

        // Bind views
        statusCard = view.findViewById(R.id.statusCard)
        statusText = view.findViewById(R.id.statusText)
        toggleSwitch = view.findViewById(R.id.toggleSwitch)
        autoReplySwitch = view.findViewById(R.id.autoReplySwitch)
        autoReplyOptionsContainer = view.findViewById(R.id.autoReplyOptionsContainer)
        autoReplyLockButton = view.findViewById(R.id.autoReplyLockButton)
        sameMessageSwitch = view.findViewById(R.id.sameMessageSwitch)
        sameMessageContainer = view.findViewById(R.id.sameMessageContainer)
        unifiedMessageEdit = view.findViewById(R.id.unifiedMessageEdit)
        unifiedSavedText = view.findViewById(R.id.unifiedSavedText)
        separateMessagesContainer = view.findViewById(R.id.separateMessagesContainer)
        smsReplyEdit = view.findViewById(R.id.smsReplyEdit)
        smsSavedText = view.findViewById(R.id.smsSavedText)
        callReplyEdit = view.findViewById(R.id.callReplyEdit)
        callSavedText = view.findViewById(R.id.callSavedText)

        // Setup listeners
        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enabled", isChecked).apply()
            updateStatus()
        }

        autoReplySwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_reply_enabled", isChecked).apply()
            updateAutoReplyVisibility()
        }

        autoReplyLockButton.setOnClickListener { toggleAutoReplyLock() }

        sameMessageSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("use_same_message", isChecked).apply()
            updateAutoReplyVisibility()
        }

        // Setup text watchers for auto-save
        unifiedMessageEdit.addTextChangedListener(createAutoSaveWatcher(
            savedIndicator = unifiedSavedText,
            shouldSave = { !isAutoReplyLocked },
            onSave = { msg -> prefs.edit().putString("unified_reply_message", msg).apply() },
            onAfterSave = { updateStatus() }
        ))

        smsReplyEdit.addTextChangedListener(createAutoSaveWatcher(
            savedIndicator = smsSavedText,
            shouldSave = { !isAutoReplyLocked },
            onSave = { msg -> prefs.edit().putString("sms_reply_message", msg).apply() },
            onAfterSave = { updateStatus() }
        ))

        callReplyEdit.addTextChangedListener(createAutoSaveWatcher(
            savedIndicator = callSavedText,
            shouldSave = { !isAutoReplyLocked },
            onSave = { msg -> prefs.edit().putString("call_reply_message", msg).apply() },
            onAfterSave = { updateStatus() }
        ))

        return view
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

        autoReplyOptionsContainer.visibility = if (autoReplyEnabled) View.VISIBLE else View.GONE

        if (autoReplyEnabled) {
            if (useSameMessage) {
                sameMessageContainer.visibility = View.VISIBLE
                separateMessagesContainer.visibility = View.GONE
            } else {
                sameMessageContainer.visibility = View.GONE
                separateMessagesContainer.visibility = View.VISIBLE
            }
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
        } else {
            autoReplyLockButton.text = getString(R.string.unlock_auto_reply)
            unifiedMessageEdit.isEnabled = true
            smsReplyEdit.isEnabled = true
            callReplyEdit.isEnabled = true
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
        val hasGmailAddress = prefs.getString("gmail_address", "")?.isNotEmpty() == true
        val hasGmailPassword = prefs.getString("gmail_password", "")?.isNotEmpty() == true
        val hasRecipientEmail = prefs.getString("email", "")?.isNotEmpty() == true
        val hasNotificationAccess = NotificationHelper.isNotificationServiceEnabled(requireContext())

        when {
            !hasNotificationAccess -> {
                statusText.text = getString(R.string.status_needs_notification)
                statusCard.setCardBackgroundColor(getColor(R.color.status_warning_container))
            }
            !hasGmailAddress || !hasGmailPassword || !hasRecipientEmail -> {
                statusText.text = getString(R.string.status_missing_config)
                statusCard.setCardBackgroundColor(getColor(R.color.status_warning_container))
            }
            enabled -> {
                statusText.text = getString(R.string.status_active)
                statusCard.setCardBackgroundColor(getColor(R.color.status_success_container))
            }
            else -> {
                statusText.text = getString(R.string.status_paused)
                statusCard.setCardBackgroundColor(getColor(R.color.md_surface_variant))
            }
        }
    }
}
