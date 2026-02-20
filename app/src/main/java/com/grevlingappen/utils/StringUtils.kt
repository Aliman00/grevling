package com.grevlingappen.utils

/**
 * StringUtils - Testbare string-utility funksjoner.
 * Brukes av EmailSender for sanitizing og escaping.
 */
object StringUtils {
    
    /**
     * Saniterer email-subjekt ved å fjerne newlines og begrense lengde.
     * Hindrer header injection angrep.
     */
    fun sanitizeSubject(input: String): String = 
        input.replace("\r", "").replace("\n", "").trim().take(120)
    
    /**
     * Escaper HTML-spesialtegn for sikker visning i email.
     */
    fun escapeHtml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
