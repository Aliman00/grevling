package com.grevlingappen.utils

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * ContactHelper - Slår opp kontaktnavn fra telefonnummer.
 * Bruker en LRU-cache og Coroutines for optimal ytelse uten å blokkere main thread.
 */
object ContactHelper {
    private const val TAG = "ContactHelper"
    private const val CACHE_SIZE = 100
    private const val NOT_FOUND = "___NOT_FOUND___"

    private val contactCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
                return size > CACHE_SIZE
            }
        }
    )

    /**
     * Hent kontaktnavn fra telefonnummer (suspend-funksjon).
     */
    suspend fun getContactName(context: Context, phoneNumber: String): String? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val normalized = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
        
        val cachedValue = contactCache[normalized]
        if (cachedValue != null) {
            return@withContext if (cachedValue == NOT_FOUND) null else cachedValue
        }

        try {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon()
                .appendPath(phoneNumber)
                .build()

            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            appContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            contactCache[normalized] = name
                            return@withContext name
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Logger.e(TAG, "Tilgang nektet til kontakter (manglende tillatelse)", e)
        } catch (e: Exception) {
            Logger.e(TAG, "Feil ved kontaktoppslag", e)
        }

        contactCache[normalized] = NOT_FOUND
        null
    }

    /**
     * Formaterer avsender som "Navn (Nummer)" eller bare "Nummer" (suspend-funksjon).
     */
    suspend fun formatSender(context: Context, phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank()) return "Ukjent nummer"

        val contactName = getContactName(context, phoneNumber)
        return if (contactName != null) {
            "$contactName ($phoneNumber)"
        } else {
            phoneNumber
        }
    }
}
