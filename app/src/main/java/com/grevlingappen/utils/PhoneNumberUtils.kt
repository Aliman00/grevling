package com.grevlingappen.utils

/**
 * PhoneNumberUtils - Utility funksjoner for telefonnummer-håndtering.
 */
object PhoneNumberUtils {
    
    /**
     * Sjekker om et telefonnummer er gyldig (ikke tomt og inneholder minst ett siffer).
     */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.isNotBlank() && phoneNumber.any { it.isDigit() }
    }
    
    /**
     * Normaliserer et telefonnummer ved å beholde kun sifre og +.
     */
    fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.filter { it.isDigit() || it == '+' }
    }
}
