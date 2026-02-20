package com.grevlingappen.data

import com.grevlingappen.domain.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AppRepositoryTest - Unit-tester for AppRepository logikk.
 * Dette tester Punkt 11 i Senior Checklist ved å verifisere "pure functions"
 * i companion object.
 */
class AppRepositoryTest {

    private val testApps = listOf(
        AppInfo("WhatsApp", "com.whatsapp", isSelected = true),
        AppInfo("Facebook", "com.facebook.katana", isSelected = false),
        AppInfo("Gmail", "com.google.android.gm", isSelected = true)
    )

    @Test
    fun `searchApps returnerer alle apper ved tom query`() {
        val result = AppRepository.searchApps(testApps, "")
        assertEquals(3, result.size)
    }

    @Test
    fun `searchApps filtrerer korrekt på navn`() {
        val result = AppRepository.searchApps(testApps, "WhatsApp")
        assertEquals(1, result.size)
        assertEquals("WhatsApp", result[0].appName)
    }

    @Test
    fun `searchApps er case-insensitive`() {
        val result = AppRepository.searchApps(testApps, "gmail")
        assertEquals(1, result.size)
        assertEquals("Gmail", result[0].appName)
    }

    @Test
    fun `filterSelected returnerer kun valgte apper`() {
        val result = AppRepository.filterSelected(testApps, showOnlySelected = true)
        assertEquals(2, result.size)
        assertTrue(result.all { it.isSelected })
    }

    @Test
    fun `filterSelected returnerer alle apper når filter er av`() {
        val result = AppRepository.filterSelected(testApps, showOnlySelected = false)
        assertEquals(3, result.size)
    }
}
