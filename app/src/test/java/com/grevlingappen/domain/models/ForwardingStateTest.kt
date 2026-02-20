package com.grevlingappen.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ForwardingStateTest - Verifiserer den sentrale tilstandslogikken i appen.
 * Dette sikrer at UI-indikasjoner og aktiveringsregler er korrekte.
 */
class ForwardingStateTest {

    @Test
    fun `canActivate er false hvis konfigurering mangler`() {
        val state = ForwardingState(
            hasEmailConfig = false,
            hasNotificationAccess = true
        )
        assertFalse(state.canActivate)
    }

    @Test
    fun `canActivate er false hvis varseltilgang mangler`() {
        val state = ForwardingState(
            hasEmailConfig = true,
            hasNotificationAccess = false
        )
        assertFalse(state.canActivate)
    }

    @Test
    fun `canActivate er true når alt er på plass`() {
        val state = ForwardingState(
            hasEmailConfig = true,
            hasNotificationAccess = true
        )
        assertTrue(state.canActivate)
    }

    @Test
    fun `statusColor er ACTIVE når alt fungerer og er påskrudd`() {
        val state = ForwardingState(
            isEnabled = true,
            hasEmailConfig = true,
            hasNotificationAccess = true
        )
        assertEquals(StatusColor.ACTIVE, state.statusColor)
    }

    @Test
    fun `statusColor er WARNING hvis konfigurasjon mangler selv om enabled er true`() {
        // Dette scenariet kan skje hvis bruker sletter email-adresse mens appen er på
        val state = ForwardingState(
            isEnabled = true,
            hasEmailConfig = false,
            hasNotificationAccess = true
        )
        assertEquals(StatusColor.WARNING, state.statusColor)
    }

    @Test
    fun `statusColor er PAUSED når manuelt deaktivert selv om alt er klart`() {
        val state = ForwardingState(
            isEnabled = false,
            hasEmailConfig = true,
            hasNotificationAccess = true
        )
        assertEquals(StatusColor.PAUSED, state.statusColor)
    }

    @Test
    fun `isFullyActive er true kun når alle kriterier er oppfylt`() {
        val activeState = ForwardingState(isEnabled = true, hasEmailConfig = true, hasNotificationAccess = true)
        val inactiveState = ForwardingState(isEnabled = false, hasEmailConfig = true, hasNotificationAccess = true)
        
        assertTrue(activeState.isFullyActive)
        assertFalse(inactiveState.isFullyActive)
    }
}
