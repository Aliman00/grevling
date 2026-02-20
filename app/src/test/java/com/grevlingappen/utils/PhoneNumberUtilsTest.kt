package com.grevlingappen.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PhoneNumberUtilsTest - Tester telefonnummer-validering og normalisering.
 * Kritisk for auto-svar funksjonalitet.
 */
class PhoneNumberUtilsTest {

    // ========================================================================
    // VALID PHONE NUMBER TESTS
    // ========================================================================

    @Test
    fun `isValidPhoneNumber returnerer true for gyldig nummer`() {
        assertTrue(PhoneNumberUtils.isValidPhoneNumber("+4712345678"))
    }

    @Test
    fun `isValidPhoneNumber returnerer true for nummer uten landskode`() {
        assertTrue(PhoneNumberUtils.isValidPhoneNumber("12345678"))
    }

    @Test
    fun `isValidPhoneNumber returnerer true for nummer med mellomrom`() {
        assertTrue(PhoneNumberUtils.isValidPhoneNumber("+47 123 45 678"))
    }

    @Test
    fun `isValidPhoneNumber returnerer true for nummer med bindestrek`() {
        assertTrue(PhoneNumberUtils.isValidPhoneNumber("123-456-789"))
    }

    @Test
    fun `isValidPhoneNumber returnerer true for nummer med parentes`() {
        assertTrue(PhoneNumberUtils.isValidPhoneNumber("(+47) 12345678"))
    }

    @Test
    fun `isValidPhoneNumber returnerer false for tom string`() {
        assertFalse(PhoneNumberUtils.isValidPhoneNumber(""))
    }

    @Test
    fun `isValidPhoneNumber returnerer false for kun whitespace`() {
        assertFalse(PhoneNumberUtils.isValidPhoneNumber("   "))
    }

    @Test
    fun `isValidPhoneNumber returnerer false for kun bokstaver`() {
        assertFalse(PhoneNumberUtils.isValidPhoneNumber("abcdef"))
    }

    @Test
    fun `isValidPhoneNumber returnerer false for kun spesialtegn`() {
        assertFalse(PhoneNumberUtils.isValidPhoneNumber("+-()"))
    }

    @Test
    fun `isValidPhoneNumber returnerer true for kort nummer`() {
        // Noen tjenestenumre er korte
        assertTrue(PhoneNumberUtils.isValidPhoneNumber("123"))
    }

    // ========================================================================
    // NORMALIZE PHONE NUMBER TESTS
    // ========================================================================

    @Test
    fun `normalizePhoneNumber beholder sifre`() {
        assertEquals("12345678", PhoneNumberUtils.normalizePhoneNumber("12345678"))
    }

    @Test
    fun `normalizePhoneNumber beholder pluss-tegn`() {
        assertEquals("+4712345678", PhoneNumberUtils.normalizePhoneNumber("+4712345678"))
    }

    @Test
    fun `normalizePhoneNumber fjerner mellomrom`() {
        assertEquals("+4712345678", PhoneNumberUtils.normalizePhoneNumber("+47 123 45 678"))
    }

    @Test
    fun `normalizePhoneNumber fjerner bindestreker`() {
        assertEquals("123456789", PhoneNumberUtils.normalizePhoneNumber("123-456-789"))
    }

    @Test
    fun `normalizePhoneNumber fjerner parenteser`() {
        assertEquals("+4712345678", PhoneNumberUtils.normalizePhoneNumber("(+47)12345678"))
    }

    @Test
    fun `normalizePhoneNumber fjerner bokstaver`() {
        assertEquals("123", PhoneNumberUtils.normalizePhoneNumber("abc123def"))
    }

    @Test
    fun `normalizePhoneNumber håndterer tom string`() {
        assertEquals("", PhoneNumberUtils.normalizePhoneNumber(""))
    }

    @Test
    fun `normalizePhoneNumber håndterer komplekst format`() {
        // typisk format: "+47 123 45 678 (kontor)"
        assertEquals("+4712345678", PhoneNumberUtils.normalizePhoneNumber("+47 123 45 678 (kontor)"))
    }
}
