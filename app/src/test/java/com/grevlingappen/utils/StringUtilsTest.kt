package com.grevlingappen.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * StringUtilsTest - Tester string-utility funksjoner.
 * Disse funksjonene er kritiske for sikkerhet (HTML escaping, subject sanitizing).
 */
class StringUtilsTest {

    // ========================================================================
    // SANITIZE SUBJECT TESTS
    // ========================================================================
    
    @Test
    fun `sanitizeSubject fjerner carriage returns`() {
        val input = "Test\rSubject"
        assertEquals("TestSubject", StringUtils.sanitizeSubject(input))
    }

    @Test
    fun `sanitizeSubject fjerner newlines`() {
        val input = "Test\nSubject"
        assertEquals("TestSubject", StringUtils.sanitizeSubject(input))
    }

    @Test
    fun `sanitizeSubject fjerner både CR og LF`() {
        val input = "Test\r\nSubject\r\nMore"
        assertEquals("TestSubjectMore", StringUtils.sanitizeSubject(input))
    }

    @Test
    fun `sanitizeSubject trimmer whitespace`() {
        val input = "  Test Subject  "
        assertEquals("Test Subject", StringUtils.sanitizeSubject(input))
    }

    @Test
    fun `sanitizeSubject begrenser lengde til 120 tegn`() {
        val input = "x".repeat(150)
        val result = StringUtils.sanitizeSubject(input)
        assertEquals(120, result.length)
    }

    @Test
    fun `sanitizeSubject beholder normale tegn`() {
        val input = "SMS fra Ola Nordgaard (+47 123 45 678)"
        assertEquals(input, StringUtils.sanitizeSubject(input))
    }

    @Test
    fun `sanitizeSubject håndterer tom string`() {
        assertEquals("", StringUtils.sanitizeSubject(""))
    }

    @Test
    fun `sanitizeSubject håndterer kun whitespace`() {
        assertEquals("", StringUtils.sanitizeSubject("   \n\r   "))
    }

    // ========================================================================
    // ESCAPE HTML TESTS
    // ========================================================================

    @Test
    fun `escapeHtml escaper ampersand`() {
        assertEquals("Tom &amp; Jerry", StringUtils.escapeHtml("Tom & Jerry"))
    }

    @Test
    fun `escapeHtml escaper less than`() {
        assertEquals("a &lt; b", StringUtils.escapeHtml("a < b"))
    }

    @Test
    fun `escapeHtml escaper greater than`() {
        assertEquals("a &gt; b", StringUtils.escapeHtml("a > b"))
    }

    @Test
    fun `escapeHtml escaper double quotes`() {
        assertEquals("Hei &quot;verden&quot;", StringUtils.escapeHtml("Hei \"verden\""))
    }

    @Test
    fun `escapeHtml escaper single quotes`() {
        assertEquals("It&#39;s working", StringUtils.escapeHtml("It's working"))
    }

    @Test
    fun `escapeHtml håndterer tom string`() {
        assertEquals("", StringUtils.escapeHtml(""))
    }

    @Test
    fun `escapeHtml håndterer string uten spesialtegn`() {
        val input = "Normal tekst uten spesialtegn"
        assertEquals(input, StringUtils.escapeHtml(input))
    }

    @Test
    fun `escapeHtml håndterer flere spesialtegn samtidig`() {
        val input = "<script>alert('XSS & \"injection\"')</script>"
        val expected = "&lt;script&gt;alert(&#39;XSS &amp; &quot;injection&quot;&#39;)&lt;/script&gt;"
        assertEquals(expected, StringUtils.escapeHtml(input))
    }

    @Test
    fun `escapeHtml forhindrer XSS angrep`() {
        // Vanlig XSS vektor
        val xss = "<img src=x onerror=alert('XSS')>"
        val escaped = StringUtils.escapeHtml(xss)
        assertFalse(escaped.contains("<"))
        assertFalse(escaped.contains(">"))
        assertTrue(escaped.contains("&lt;"))
        assertTrue(escaped.contains("&gt;"))
    }
}
