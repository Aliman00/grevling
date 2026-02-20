package com.grevlingappen.utils

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * PermissionsHelperTest - Tester tillatelses-håndtering.
 * Bruker Robolectric for å simulere Android-miljø.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class PermissionsHelperTest {

    private lateinit var context: Context
    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        context = application
    }

    // ========================================================================
    // REQUIRED PERMISSIONS LIST TESTS
    // ========================================================================

    @Test
    fun `requiredPermissions inneholder RECEIVE_SMS`() {
        assertTrue(PermissionsHelper.requiredPermissions.contains(Manifest.permission.RECEIVE_SMS))
    }

    @Test
    fun `requiredPermissions inneholder READ_SMS`() {
        assertTrue(PermissionsHelper.requiredPermissions.contains(Manifest.permission.READ_SMS))
    }

    @Test
    fun `requiredPermissions inneholder SEND_SMS`() {
        assertTrue(PermissionsHelper.requiredPermissions.contains(Manifest.permission.SEND_SMS))
    }

    @Test
    fun `requiredPermissions inneholder READ_CALL_LOG`() {
        assertTrue(PermissionsHelper.requiredPermissions.contains(Manifest.permission.READ_CALL_LOG))
    }

    @Test
    fun `requiredPermissions inneholder READ_CONTACTS`() {
        assertTrue(PermissionsHelper.requiredPermissions.contains(Manifest.permission.READ_CONTACTS))
    }

    @Test
    fun `requiredPermissions har riktig antall`() {
        assertEquals(5, PermissionsHelper.requiredPermissions.size)
    }

    // ========================================================================
    // HAS ALL PERMISSIONS TESTS
    // ========================================================================

    @Test
    fun `hasAllPermissions returnerer false når ingen tillatelser er gitt`() {
        assertFalse(PermissionsHelper.hasAllPermissions(context))
    }

    @Test
    fun `hasAllPermissions returnerer false når noen tillatelser mangler`() {
        val shadowApp = Shadows.shadowOf(application)
        shadowApp.grantPermissions(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        
        assertFalse(PermissionsHelper.hasAllPermissions(context))
    }

    @Test
    fun `hasAllPermissions returnerer true når alle tillatelser er gitt`() {
        val shadowApp = Shadows.shadowOf(application)
        shadowApp.grantPermissions(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )
        
        assertTrue(PermissionsHelper.hasAllPermissions(context))
    }

    // ========================================================================
    // GET MISSING PERMISSIONS TESTS
    // ========================================================================

    @Test
    fun `getMissingPermissions returnerer alle når ingen er gitt`() {
        val missing = PermissionsHelper.getMissingPermissions(context)
        
        assertEquals(5, missing.size)
        assertTrue(missing.contains(Manifest.permission.RECEIVE_SMS))
        assertTrue(missing.contains(Manifest.permission.READ_SMS))
        assertTrue(missing.contains(Manifest.permission.SEND_SMS))
        assertTrue(missing.contains(Manifest.permission.READ_CALL_LOG))
        assertTrue(missing.contains(Manifest.permission.READ_CONTACTS))
    }

    @Test
    fun `getMissingPermissions returnerer tom liste når alle er gitt`() {
        val shadowApp = Shadows.shadowOf(application)
        shadowApp.grantPermissions(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )
        
        val missing = PermissionsHelper.getMissingPermissions(context)
        assertTrue(missing.isEmpty())
    }

    @Test
    fun `getMissingPermissions returnerer kun manglende tillatelser`() {
        val shadowApp = Shadows.shadowOf(application)
        shadowApp.grantPermissions(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )
        
        val missing = PermissionsHelper.getMissingPermissions(context)
        
        assertEquals(3, missing.size)
        assertTrue(missing.contains(Manifest.permission.READ_SMS))
        assertTrue(missing.contains(Manifest.permission.READ_CALL_LOG))
        assertTrue(missing.contains(Manifest.permission.READ_CONTACTS))
    }

    // ========================================================================
    // HAS PERMISSION TESTS
    // ========================================================================

    @Test
    fun `hasPermission returnerer false når tillatelse ikke er gitt`() {
        assertFalse(PermissionsHelper.hasPermission(context, Manifest.permission.SEND_SMS))
    }

    @Test
    fun `hasPermission returnerer true når tillatelse er gitt`() {
        val shadowApp = Shadows.shadowOf(application)
        shadowApp.grantPermissions(Manifest.permission.SEND_SMS)
        
        assertTrue(PermissionsHelper.hasPermission(context, Manifest.permission.SEND_SMS))
    }

    // ========================================================================
    // NOTIFICATION SERVICE INTENT TESTS
    // ========================================================================

    @Test
    fun `getNotificationSettingsIntent returnerer gyldig intent`() {
        val intent = PermissionsHelper.getNotificationSettingsIntent()
        
        assertEquals("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS", intent.action)
    }
}
