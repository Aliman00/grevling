package com.grevlingappen.utils

import com.grevlingappen.R
import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * ContactHelper - Objekt som slår opp kontaktnavn fra telefonnummer.
 * 
 * Funksjonalitet:
 * - Søker i enhetens kontakter etter navn basert på telefonnummer
 * - Bruker LRU-cache (100 oppføringer) for å unngå gjentatte oppslag
 * - Kjører asynkront på IO-dispatcher for å ikke blokkere UI
 * - Returnerer "Navn (Nummer)" format for visning
 * 
 * Merk: Krever READ_CONTACTS tillatelse for å fungere.
 */
object ContactHelper {
    private const val TAG = "ContactHelper"
    
    // Maksimalt antall kontakter å bufre (LRU - minst brukte fjernes først)
    private const val CACHE_SIZE = 100
    // Sentinel-verdi for numre som ikke finnes i kontakter
    private const val NOT_FOUND = "___NOT_FOUND___"

    // Thread-safe LRU cache for kontaktoppslag
    private val contactCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
                return size > CACHE_SIZE
            }
        }
    )

    /**
     * Søker etter kontaktnavn basert på telefonnummer.
     * 
     * Bruker Android PhoneLookup for effektivt oppslag.
     * Resultater caches for å unngå gjentatte oppslag.
     * 
     * @param context App-kontekst
     * @param phoneNumber Telefonnummer å søke etter
     * @return Kontaktnavn hvis funnet, null hvis ikke funnet/manglende tillatelse
     */
    suspend fun getContactName(context: Context, phoneNumber: String): String? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        // Normaliser nummeret før oppslag
        val normalized = PhoneNumberUtils.normalizePhoneNumber(phoneNumber)
        
        // Sjekk cache først
        val cachedValue = contactCache[normalized]
        if (cachedValue != null) {
            // Returner null hvis dette nummeret er cachet som "ikke funnet"
            return@withContext if (cachedValue == NOT_FOUND) null else cachedValue
        }

        try {
            // Bruk PhoneLookup for å finne kontakt
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon()
                .appendPath(normalized)
                .build()

            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            appContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            // Cache resultatet
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

        // Cache "ikke funnet" for å unngå gjentatte oppslag
        contactCache[normalized] = NOT_FOUND
        null
    }

    /**
     * Formaterer avsendernavn for visning i e-post.
     * 
     * Returnerer "Navn (Nummer)" hvis kontakt finnes i telefonboken,
     * eller bare "Nummer" hvis ikke.
     * 
     * @param context App-kontekst
     * @param phoneNumber Telefonnummer å formatere
     * @return Formattert streng for visning
     */
    suspend fun formatSender(context: Context, phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank()) return context.getString(R.string.unknown_number)

        // Søk etter kontaktnavn
        val contactName = getContactName(context, phoneNumber)
        return if (contactName != null) {
            // Returner "Navn (Nummer)" format
            "$contactName ($phoneNumber)"
        } else {
            // Returner bare nummeret hvis ingen kontakt funnet
            phoneNumber
        }
    }
}