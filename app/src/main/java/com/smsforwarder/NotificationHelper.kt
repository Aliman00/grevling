package com.smsforwarder

import android.content.Context
import androidx.core.app.NotificationManagerCompat

/**
 * Helper for notification-relaterte operasjoner
 */
object NotificationHelper {

    /**
     * Sjekker om NotificationListenerService er aktivert for appen.
     * Bruker NotificationManagerCompat for pålitelig matching (unngår falsk positiv fra substring).
     */
    fun isNotificationServiceEnabled(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }
}
