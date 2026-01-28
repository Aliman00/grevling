package com.smsforwarder

import android.content.Context
import android.provider.Settings

/**
 * Helper for notification-relaterte operasjoner
 */
object NotificationHelper {

    /**
     * Sjekker om NotificationListenerService er aktivert for appen
     */
    fun isNotificationServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledServices?.contains(context.packageName) == true
    }
}
