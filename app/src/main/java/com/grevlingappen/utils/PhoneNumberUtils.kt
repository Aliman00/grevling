package com.grevlingappen.utils

/**
 * PhoneNumberUtils - Utility-funksjoner for telefonnummer-håndtering.
 * 
 * Inneholder hjelpefunksjoner for validering og normalisering av telefonnummer.
 * Brukes av auto-svar og kontaktoppslag for å sikre konsistent nummerformat.
 */
object PhoneNumberUtils {
    
    /**
     * Sjekker om et telefonnummer er gyldig.
     * 
     * Et gyldig nummer må:
     * - Ikke være tomt/blankt
     * - Inneholde minst ett siffer
     * 
     * @param phoneNumber Nummeret å validere
     * @return true hvis gyldig, false ellers
     */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.isNotBlank() && phoneNumber.any { it.isDigit() }
    }
    
    /**
     * Normaliserer et telefonnummer ved å fjerne alle tegn unntatt siffer og pluss-tegn.
     * 
     * Fjerner mellomrom, bindestreker, paranteser, etc.
     * Beholder + for internasjonalt prefix.
     * 
     * Eksempel: "+47 912 34 567" -> "+4791234567"
     * 
     * @param phoneNumber Nummeret å normalisere
     * @return Normalisert nummer
     */
    fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.filter { it.isDigit() || it == '+' }
    }
}