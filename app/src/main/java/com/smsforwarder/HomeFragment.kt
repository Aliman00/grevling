package com.smsforwarder

import android.content.SharedPreferences
import android.content.res.ColorStateList
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
    private lateinit var statusCircle: MaterialCardView
    private lateinit var statusCircleText: TextView
    private lateinit var statusLabel: TextView
    private lateinit var statusText: TextView
    private lateinit var toggleButton: MaterialButton
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
        isAutoReplyLocked = prefs.getBoolean(PreferencesManager.KEY_AUTO_REPLY_LOCKED, true)

        // Bind views
        statusCard = view.findViewById(R.id.statusCard)
        statusCircle = view.findViewById(R.id.statusCircle)
        statusCircleText = view.findViewById(R.id.statusCircleText)
        statusLabel = view.findViewById(R.id.statusLabel)
        statusText = view.findViewById(R.id.statusText)
        toggleButton = view.findViewById(R.id.toggleButton)
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
        toggleButton.setOnClickListener {
            val currentEnabled = prefs.getBoolean(PreferencesManager.KEY_ENABLED, false)
            prefs.edit().putBoolean(PreferencesManager.KEY_ENABLED, !currentEnabled).apply()
            updateStatus()
            // Oppdater alle widgets når status endres i appen
            context?.let { ctx ->
                WidgetHelper.updateAllWidgetsOfType(ctx, ForwardingWidget::class.java)
                WidgetHelper.updateAllWidgetsOfType(ctx, ForwardingWidgetMini::class.java)
                WidgetHelper.updateAllWidgetsOfType(ctx, StatsWidget::class.java)
            }
        }

        autoReplySwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PreferencesManager.KEY_AUTO_REPLY_ENABLED, isChecked).apply()
            updateAutoReplyVisibility()
        }

        autoReplyLockButton.setOnClickListener { toggleAutoReplyLock() }

        sameMessageSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PreferencesManager.KEY_USE_SAME_MESSAGE, isChecked).apply()
            updateAutoReplyVisibility()
        }

        // Setup text watchers for auto-save
        unifiedMessageEdit.addTextChangedListener(createAutoSaveWatcher(
            savedIndicator = unifiedSavedText,
            shouldSave = { !isAutoReplyLocked },
            onSave = { msg -> prefs.edit().putString(PreferencesManager.KEY_UNIFIED_REPLY_MESSAGE, msg).apply() },
            onAfterSave = { updateStatus() }
        ))

        smsReplyEdit.addTextChangedListener(createAutoSaveWatcher(
            savedIndicator = smsSavedText,
            shouldSave = { !isAutoReplyLocked },
            onSave = { msg -> prefs.edit().putString(PreferencesManager.KEY_SMS_REPLY_MESSAGE, msg).apply() },
            onAfterSave = { updateStatus() }
        ))

        callReplyEdit.addTextChangedListener(createAutoSaveWatcher(
            savedIndicator = callSavedText,
            shouldSave = { !isAutoReplyLocked },
            onSave = { msg -> prefs.edit().putString(PreferencesManager.KEY_CALL_REPLY_MESSAGE, msg).apply() },
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
        val autoReplyEnabled = prefs.getBoolean(PreferencesManager.KEY_AUTO_REPLY_ENABLED, false)
        val useSameMessage = prefs.getBoolean(PreferencesManager.KEY_USE_SAME_MESSAGE, true)

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
        prefs.edit().putBoolean(PreferencesManager.KEY_AUTO_REPLY_LOCKED, isAutoReplyLocked).apply()
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
        autoReplySwitch.isChecked = prefs.getBoolean(PreferencesManager.KEY_AUTO_REPLY_ENABLED, false)
        sameMessageSwitch.isChecked = prefs.getBoolean(PreferencesManager.KEY_USE_SAME_MESSAGE, true)

        // Last meldinger — lagre standardverdi til prefs hvis den ikke finnes,
        // slik at AutoReplyHelper alltid kan lese meldingen.
        val unifiedMsg = prefs.getString(PreferencesManager.KEY_UNIFIED_REPLY_MESSAGE, null)
            ?: getString(R.string.default_unified_message).also {
                prefs.edit().putString(PreferencesManager.KEY_UNIFIED_REPLY_MESSAGE, it).apply()
            }
        unifiedMessageEdit.setText(unifiedMsg)

        val smsMsg = prefs.getString(PreferencesManager.KEY_SMS_REPLY_MESSAGE, null)
            ?: getString(R.string.default_sms_message).also {
                prefs.edit().putString(PreferencesManager.KEY_SMS_REPLY_MESSAGE, it).apply()
            }
        smsReplyEdit.setText(smsMsg)

        val callMsg = prefs.getString(PreferencesManager.KEY_CALL_REPLY_MESSAGE, null)
            ?: getString(R.string.default_call_message).also {
                prefs.edit().putString(PreferencesManager.KEY_CALL_REPLY_MESSAGE, it).apply()
            }
        callReplyEdit.setText(callMsg)
    }

    private fun updateStatus() {
        val enabled = prefs.getBoolean(PreferencesManager.KEY_ENABLED, false)
        val hasGmailAddress = prefs.getString(PreferencesManager.KEY_GMAIL_ADDRESS, "")?.isNotEmpty() == true
        val hasGmailPassword = prefs.getString(PreferencesManager.KEY_GMAIL_PASSWORD, "")?.isNotEmpty() == true
        val hasRecipientEmail = prefs.getString(PreferencesManager.KEY_RECIPIENT_EMAIL, "")?.isNotEmpty() == true
        val recipientEmail = prefs.getString(PreferencesManager.KEY_RECIPIENT_EMAIL, "") ?: ""
        val hasNotificationAccess = NotificationHelper.isNotificationServiceEnabled(requireContext())

        // Update circle and text based on enabled state
        val activeColor = getColor(R.color.status_active_color)
        val pausedColor = getColor(R.color.status_paused_color)
        
        if (enabled && hasNotificationAccess && hasGmailAddress && hasGmailPassword && hasRecipientEmail) {
            // Active state - show pause button (red)
            statusCircle.setStrokeColor(ColorStateList.valueOf(activeColor))
            statusCircleText.setTextColor(activeColor)
            statusCircleText.text = getString(R.string.status_circle_active)
            statusLabel.text = getString(R.string.status_label_forwards_to)
            statusText.text = recipientEmail
            
            // Button: Red with pause icon
            toggleButton.text = getString(R.string.toggle_button_pause)
            toggleButton.setIconResource(R.drawable.ic_pause)
            toggleButton.setBackgroundColor(pausedColor)
        } else {
            // Paused/inactive state - show activate button (green)
            statusCircle.setStrokeColor(ColorStateList.valueOf(pausedColor))
            statusCircleText.setTextColor(pausedColor)
            statusCircleText.text = getString(R.string.status_circle_paused)
            
            // Button: Green with play icon
            toggleButton.text = getString(R.string.toggle_button_activate)
            toggleButton.setIconResource(R.drawable.ic_play)
            toggleButton.setBackgroundColor(activeColor)
            
            when {
                !hasNotificationAccess -> {
                    statusLabel.text = getString(R.string.status_needs_notification)
                    statusText.text = ""
                }
                !hasGmailAddress || !hasGmailPassword || !hasRecipientEmail -> {
                    statusLabel.text = getString(R.string.status_missing_config)
                    statusText.text = ""
                }
                else -> {
                    statusLabel.text = getString(R.string.status_label_forwards_to)
                    statusText.text = if (recipientEmail.isNotEmpty()) recipientEmail else getString(R.string.status_no_email)
                }
            }
        }
    }
}