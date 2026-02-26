package com.grevlingappen.domain.models

/**
 * ForwardingState - Data class som representerer komplett UI-tilstand for HomeScreen.
 * 
 * Dette er "single source of truth" for hovedskjermen - alt UI trenger å vite
 * om appens tilstand finnes i denne klassen.
 * 
 * @property isEnabled Om videresending er aktivert av brukeren
 * @property hasNotificationAccess Om brukeren har gitt Notification Access
 * @property hasEmailConfig Om e-post er konfigurert (address + password + mottaker)
 * @property recipientEmail E-postadressen det videresendes til
 * @property gmailAddress Gmail-adressen som brukes til sending
 * @property hasGmailPassword Om passord er lagret (ikke passordet selv)
 * @property autoReplyEnabled Om auto-svar er aktivert
 * @property useSameMessage Om samme melding brukes for SMS og anrop
 * @property unifiedMessage Auto-svar melding (når useSameMessage er true)
 * @property smsMessage Auto-svar melding for SMS (når useSameMessage er false)
 * @property callMessage Auto-svar melding for anrop (når useSameMessage er false)
 * @property statusMessage Brukervennlig statustekst for visning i UI
 */
data class ForwardingState(
    // Hovedstatus
    val isEnabled: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val hasEmailConfig: Boolean = false,

    // E-post konfigurasjon
    val recipientEmail: String = "",
    val gmailAddress: String = "",
    val hasGmailPassword: Boolean = false,

    // Auto-svar innstillinger
    val autoReplyEnabled: Boolean = false,
    val useSameMessage: Boolean = true,
    val unifiedMessage: String = "",
    val smsMessage: String = "",
    val callMessage: String = "",

    // UI tilbakemelding
    val statusMessage: String = ""
) {
    /**
     * Kan videresending aktiveres?
     * Krever både gyldig e-postkonfigurasjon OG Notification Access.
     */
    val canActivate: Boolean
        get() = hasEmailConfig && hasNotificationAccess

    /**
     * Er systemet fullt operativt?
     * Betyr at det er aktivert OG konfigurert.
     */
    val isFullyActive: Boolean
        get() = isEnabled && canActivate

    /**
     * Hvilken farge skal status-indikatoren ha?
     * 
     * @return StatusColor.ACTIVE (grønn) hvis alt fungerer
     * @return StatusColor.WARNING (gul) hvis noe mangler
     * @return StatusColor.PAUSED (rød) hvis aktivert men pauset manuelt
     */
    val statusColor: StatusColor
        get() = when {
            isFullyActive -> StatusColor.ACTIVE
            !hasNotificationAccess || !hasEmailConfig -> StatusColor.WARNING
            else -> StatusColor.PAUSED
        }
}

/**
 * SaveStatus - Enum som holder styr på lagre-status for UI.
 * Brukes til å vise tilstand til brukeren (f.eks. "Lagrer...").
 */
enum class SaveStatus {
    NONE,    // Ingenting pågår
    SAVING,  // Lagrer akkurat nå
    SAVED    // Ferdig lagret
}

/**
 * StatusColor - Definerer de tre hovedtilstandene for status-visningen.
 * Brukes til å sette riktig farge på statusindikatoren i UI.
 */
enum class StatusColor {
    ACTIVE,   // Grønn - alt fungerer og videresending er aktiv
    PAUSED,   // Rød - pauset manuelt av brukeren
    WARNING   // Gul/Gul - mangler konfigurasjon eller tillatelser
}