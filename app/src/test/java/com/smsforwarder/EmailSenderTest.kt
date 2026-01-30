package com.smsforwarder

import org.junit.Assert.assertEquals
import org.junit.Test

class EmailSenderTest {

    @Test
    fun `escapeHtml should escape script tags`() {
        val input = "<script>alert('XSS')</script>"
        val expected = "&lt;script&gt;alert(&#39;XSS&#39;)&lt;/script&gt;"
        val result = EmailSender.escapeHtml(input)
        assertEquals(expected, result)
    }

    @Test
    fun `escapeHtml should escape ampersand`() {
        val input = "Tom & Jerry"
        val expected = "Tom &amp; Jerry"
        val result = EmailSender.escapeHtml(input)
        assertEquals(expected, result)
    }

    @Test
    fun `escapeHtml should leave plain text unchanged`() {
        val input = "Hello World"
        val expected = "Hello World"
        val result = EmailSender.escapeHtml(input)
        assertEquals(expected, result)
    }

    @Test
    fun `escapeHtml should escape all special characters`() {
        val input = "<div>Test & \"quoted\" text with 'apostrophe'</div>"
        val expected = "&lt;div&gt;Test &amp; &quot;quoted&quot; text with &#39;apostrophe&#39;&lt;/div&gt;"
        val result = EmailSender.escapeHtml(input)
        assertEquals(expected, result)
    }

    @Test
    fun `escapeHtml should handle empty string`() {
        val input = ""
        val expected = ""
        val result = EmailSender.escapeHtml(input)
        assertEquals(expected, result)
    }
}
