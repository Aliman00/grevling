package com.smsforwarder

import android.Manifest

object PermissionsHelper {

    const val PERMISSION_REQUEST_CODE = 123

    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS
    )
}
