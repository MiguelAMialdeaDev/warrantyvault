package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.domain.util.warrantyMonths
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WarrantyDurationTest {

    @Test
    fun `one month bought on January 31st is still one month`() {
        val start = LocalDate.of(2026, 1, 31)
        assertEquals(1L, warrantyMonths(start, start.plusMonths(1)))
    }

    @Test
    fun `two years bought on a leap day are 24 months`() {
        val start = LocalDate.of(2024, 2, 29)
        assertEquals(24L, warrantyMonths(start, start.plusMonths(24)))
    }

    @Test
    fun `regular dates match the calendar distance`() {
        val start = LocalDate.of(2026, 3, 15)
        listOf(1L, 6L, 12L, 36L).forEach { months ->
            assertEquals(months, warrantyMonths(start, start.plusMonths(months)))
        }
    }
}
