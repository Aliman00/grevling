package com.smsforwarder

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionsHelperTest {

    @Test
    fun `REQUIRED_PERMISSIONS should contain all expected permissions`() {
        val requiredPermissions = PermissionsHelper.REQUIRED_PERMISSIONS
        val expectedPermissions = setOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )

        // Verify we have exactly 5 permissions
        assertEquals(
            "REQUIRED_PERMISSIONS should have exactly 5 permissions",
            5,
            requiredPermissions.size
        )

        // Verify all expected permissions are present
        expectedPermissions.forEach { permission ->
            assertTrue(
                "REQUIRED_PERMISSIONS should contain $permission",
                requiredPermissions.contains(permission)
            )
        }
    }
}
