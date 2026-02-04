package com.smsforwarder

import android.animation.ObjectAnimator
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText

class HomeFragment : BaseFragment() {

    private lateinit var statusText: TextView
    private lateinit var statusRing: View
    private lateinit var forwardingToText: TextView
    private lateinit var toggleButton: LinearLayout
    private lateinit var toggleButtonIcon: ImageView
    private lateinit var toggleButtonText: TextView
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

        // Bind views - new hero header
        statusText = view.findViewById(R.id.statusText)
        statusRing = view.findViewById(R.id.statusRing)
        forwardingToText = view.findViewById(R.id.forwardingToText)
        toggleButton = view.findViewById(R.id.toggleButton)
        toggleButtonIcon = view.findViewById(R.id.toggleButtonIcon)
        toggleButtonText = view.findViewById(R.id.toggleButtonText)
        
        // Bind views - auto reply section
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

        // Setup toggle button click
        toggleButton.setOnClickListener {
            val isEnabled = prefs.getBoolean("enabled", false)
            prefs.edit().putBoolean("enabled", !isEnabled).apply()
            updateStatus()
            context?.let { ForwardingWidget.updateAllWidgets(it) }
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
        updateForwardingInfo()
    }

    private fun updateForwardingInfo() {
        // Update forwarding destination
        val recipientEmail = prefs.getString("email", "") ?: ""
        forwardingToText.text = if (recipientEmail.isNotEmpty()) {
            getString(R.string.forwarding_to_format, recipientEmail)
        } else {
            getString(R.string.forwarding_to_placeholder)
        }
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
        // Load auto-reply settings
        autoReplySwitch.isChecked = prefs.getBoolean("auto_reply_enabled", false)
        sameMessageSwitch.isChecked = prefs.getBoolean("use_same_message", true)

        // Last meldinger - bruk default kun for visning, ikke skriv til prefs før bruker endrer
        val unifiedMsg = prefs.getString("unified_reply_message", null)
            ?: getString(R.string.default_unified_message)
        unifiedMessageEdit.setText(unifiedMsg)

        val smsMsg = prefs.getString("sms_reply_message", null)
            ?: getString(R.string.default_sms_message)
        smsReplyEdit.setText(smsMsg)

        val callMsg = prefs.getString("call_reply_message", null)
            ?: getString(R.string.default_call_message)
        callReplyEdit.setText(callMsg)
    }

    private fun updateStatus() {
        val enabled = prefs.getBoolean("enabled", false)
        val hasGmailAddress = prefs.getString("gmail_address", "")?.isNotEmpty() == true
        val hasGmailPassword = prefs.getString("gmail_password", "")?.isNotEmpty() == true
        val hasRecipientEmail = prefs.getString("email", "")?.isNotEmpty() == true
        val hasNotificationAccess = NotificationHelper.isNotificationServiceEnabled(requireContext())

        val hasWarning = !hasNotificationAccess || !hasGmailAddress || !hasGmailPassword || !hasRecipientEmail

        when {
            hasWarning -> {
                // Show warning state
                statusText.text = getString(R.string.status_display_warning)
                statusText.setTextColor(getColor(R.color.status_warning))
                statusRing.setBackgroundResource(R.drawable.circle_ring_paused)
                toggleButton.setBackgroundResource(R.drawable.button_activate_background)
                toggleButtonIcon.setImageResource(R.drawable.ic_play)
                toggleButtonText.text = getString(R.string.button_activate)
            }
            enabled -> {
                // Active state - green ring
                statusText.text = getString(R.string.status_display_active)
                statusText.setTextColor(getColor(R.color.status_active_green))
                statusRing.setBackgroundResource(R.drawable.circle_ring_active)
                toggleButton.setBackgroundResource(R.drawable.button_pause_background)
                toggleButtonIcon.setImageResource(R.drawable.ic_pause)
                toggleButtonText.text = getString(R.string.button_pause)
            }
            else -> {
                // Paused state - red ring
                statusText.text = getString(R.string.status_display_paused)
                statusText.setTextColor(getColor(R.color.status_paused_red))
                statusRing.setBackgroundResource(R.drawable.circle_ring_paused)
                toggleButton.setBackgroundResource(R.drawable.button_activate_background)
                toggleButtonIcon.setImageResource(R.drawable.ic_play)
                toggleButtonText.text = getString(R.string.button_activate)
            }
        }
    }
}