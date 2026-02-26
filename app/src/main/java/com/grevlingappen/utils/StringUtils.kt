package com.grevlingappen.utils

/**
 * StringUtils - Hjelpefunksjoner for strengmanipulasjon.
 * 
 * Brukes hovedsakelig av EmailSender for:
 * - Sanitering av e-postemner
 * - Escaping av HTML-tegn
 */
object StringUtils {
    
    /**
     * Saniterer e-postemne ved å fjerne newlines og begrense lengde.
     * 
     * Forhindrer header injection angrep ved å fjerne:
     * - Linjeskift (CR/LF)
     * - Ekstra mellomrom
     * - Begrense til 120 tegn
     * 
     * @param input Rå input-streng
     * @return Sanitert streng
     */
    fun sanitizeSubject(input: String): String = 
        input.replace("\r", "").replace("\n", "").trim().take(120)
    
    /**
     * Esker HTML-spesialtegn for sikker visning i e-post.
     * 
     * Konverterer farlige tegn til HTML-entiteter:
     * & → &amp;
     * < → &lt;
     * > → &gt;
     * " → &quot;
     * ' → &#39;
     * 
     * @param input Streng med potensielle HTML-tegn
     * @return Streng med eskapete tegn
     */
    fun escapeHtml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}