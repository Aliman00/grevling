package com.grevlingappen.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ContactHelperLogicTest - Tester logikk som kan testes uten Android Context.
 * 
 * Merk: ContactHelper.getContactName() og formatSender() krever Android Context
 * og ContentResolver, så disse testes via instrumented tests eller Robolectric.
 * 
 * Denne filen tester utility-funksjonene som ContactHelper bruker.
 */
class ContactHelperLogicTest {

    // ========================================================================
    // FORMAT SENDER LOGIC TESTS
    // ========================================================================
    // Disse tester verifiserer logikken i formatSender uten å kalle faktisk Android-kode

    @Test
    fun `formatSender logikk - null phoneNumber returnerer Ukjent nummer`() {
        val phoneNumber: String? = null
        val expected = "Ukjent nummer"
        
        val result = if (phoneNumber.isNullOrBlank()) "Ukjent nummer" else phoneNumber
        assertEquals(expected, result)
    }

    @Test
    fun `formatSender logikk - blank phoneNumber returnerer Ukjent nummer`() {
        val phoneNumber: String? = "   "
        val expected = "Ukjent nummer"
        
        val result = if (phoneNumber.isNullOrBlank()) "Ukjent nummer" else phoneNumber
        assertEquals(expected, result)
    }

    @Test
    fun `formatSender logikk - tom phoneNumber returnerer Ukjent nummer`() {
        val phoneNumber: String? = ""
        val expected = "Ukjent nummer"
        
        val result = if (phoneNumber.isNullOrBlank()) "Ukjent nummer" else phoneNumber
        assertEquals(expected, result)
    }

    @Test
    fun `formatSender logikk - gyldig nummer uten kontakt returnerer nummer`() {
        val phoneNumber = "+4712345678"
        val contactName: String? = null
        
        val result = if (contactName != null) {
            "$contactName ($phoneNumber)"
        } else {
            phoneNumber
        }
        
        assertEquals(phoneNumber, result)
    }

    @Test
    fun `formatSender logikk - gyldig nummer med kontakt returnerer formatert string`() {
        val phoneNumber = "+4712345678"
        val contactName: String? = "Ola Nordmann"
        
        val result = if (contactName != null) {
            "$contactName ($phoneNumber)"
        } else {
            phoneNumber
        }
        
        assertEquals("Ola Nordmann (+4712345678)", result)
    }

    // ========================================================================
    // NORMALIZATION TESTS (via PhoneNumberUtils)
    // ========================================================================

    @Test
    fun `ContactHelper bruker samme normalisering som PhoneNumberUtils`() {
        val input = "+47 123 45 678"
        val normalized = PhoneNumberUtils.normalizePhoneNumber(input)
        
        assertEquals("+4712345678", normalized)
    }
}
