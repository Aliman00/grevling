package com.grevlingappen.domain.models

/**
 * ForwardingState - UI-state for HomeScreen.
 * Representerer "single source of truth" for hele hjemskjermen.
 */
data class ForwardingState(
    // Hovedstatus
    val isEnabled: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val hasEmailConfig: Boolean = false,

    // Email konfigurasjon
    val recipientEmail: String = "",
    val gmailAddress: String = "",
    val hasGmailPassword: Boolean = false,

    // Auto-reply
    val autoReplyEnabled: Boolean = false,
    val useSameMessage: Boolean = true,
    val unifiedMessage: String = "",
    val smsMessage: String = "",
    val callMessage: String = "",

    // UI feedback
    val statusMessage: String = ""
) {
    /** Kan videresending aktiveres? Krever både konfigurasjon og tilgang. */
    val canActivate: Boolean
        get() = hasEmailConfig && hasNotificationAccess

    /** Er systemet fullt operativt og påskrudd? */
    val isFullyActive: Boolean
        get() = isEnabled && canActivate

    /** Hvilken farge skal status-indikatoren ha? */
    val statusColor: StatusColor
        get() = when {
            isFullyActive -> StatusColor.ACTIVE
            !hasNotificationAccess || !hasEmailConfig -> StatusColor.WARNING
            else -> StatusColor.PAUSED
        }
}

enum class SaveStatus {
    NONE,
    SAVING,
    SAVED
}

/**
 * StatusColor - Definerer de tre hovedtilstandene for status-visningen.
 */
enum class StatusColor {
    ACTIVE,   // Grønn - alt fungerer
    PAUSED,   // Rød - manuelt deaktivert
    WARNING   // Gul - mangler konfigurasjon eller tillatelse
}