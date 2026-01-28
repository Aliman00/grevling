package com.smsforwarder

import android.content.Context
import android.database.SQLException
import android.provider.ContactsContract

object ContactHelper {

    private const val TAG = "ContactHelper"

    fun getContactName(context: Context, phoneNumber: String): String? {
        try {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon()
                .appendPath(phoneNumber)
                .build()

            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: SecurityException) {
            Logger.e(TAG, "Tilgang nektet til kontakter", e)
        } catch (e: SQLException) {
            Logger.e(TAG, "Database-feil ved kontaktoppslag", e)
        } catch (e: IllegalArgumentException) {
            Logger.e(TAG, "Ugyldig telefonnummer: $phoneNumber", e)
        }
        return null
    }
    
    fun formatSender(context: Context, phoneNumber: String?): String {
        // Valider input først
        if (phoneNumber.isNullOrBlank()) {
            return "Ukjent nummer"
        }

        val contactName = getContactName(context, phoneNumber)
        return if (contactName != null) {
            "$contactName ($phoneNumber)"
        } else {
            phoneNumber
        }
    }
}
