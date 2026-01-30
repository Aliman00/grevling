package com.smsforwarder

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionsHelperTest {

    @Test
    fun `REQUIRED_PERMISSIONS should contain RECEIVE_SMS`() {
        assertTrue(
            "REQUIRED_PERMISSIONS should contain RECEIVE_SMS",
            PermissionsHelper.REQUIRED_PERMISSIONS.contains(Manifest.permission.RECEIVE_SMS)
        )
    }

    @Test
    fun `REQUIRED_PERMISSIONS should contain READ_SMS`() {
        assertTrue(
            "REQUIRED_PERMISSIONS should contain READ_SMS",
            PermissionsHelper.REQUIRED_PERMISSIONS.contains(Manifest.permission.READ_SMS)
        )
    }

    @Test
    fun `REQUIRED_PERMISSIONS should contain SEND_SMS`() {
        assertTrue(
            "REQUIRED_PERMISSIONS should contain SEND_SMS",
            PermissionsHelper.REQUIRED_PERMISSIONS.contains(Manifest.permission.SEND_SMS)
        )
    }

    @Test
    fun `REQUIRED_PERMISSIONS should contain READ_CALL_LOG`() {
        assertTrue(
            "REQUIRED_PERMISSIONS should contain READ_CALL_LOG",
            PermissionsHelper.REQUIRED_PERMISSIONS.contains(Manifest.permission.READ_CALL_LOG)
        )
    }

    @Test
    fun `REQUIRED_PERMISSIONS should contain READ_CONTACTS`() {
        assertTrue(
            "REQUIRED_PERMISSIONS should contain READ_CONTACTS",
            PermissionsHelper.REQUIRED_PERMISSIONS.contains(Manifest.permission.READ_CONTACTS)
        )
    }

    @Test
    fun `REQUIRED_PERMISSIONS should have exactly 5 permissions`() {
        assertEquals(
            "REQUIRED_PERMISSIONS should have exactly 5 permissions",
            5,
            PermissionsHelper.REQUIRED_PERMISSIONS.size
        )
    }
}
