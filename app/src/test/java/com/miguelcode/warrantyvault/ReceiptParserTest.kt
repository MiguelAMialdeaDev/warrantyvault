package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.feature.addedit.receipt.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {

    private val today = LocalDate.of(2026, 11, 27)

    @Test
    fun `reads store, date and total from a typical Spanish receipt`() {
        val lines = listOf(
            "MEDIA MARKT VALENCIA",
            "C.I.F. A-12345678",
            "C/ Colón 12, Valencia",
            "FACTURA SIMPLIFICADA 0012-345",
            "Fecha: 14/03/2026 Hora: 18:22",
            "TELEVISOR LG 55\" 1 599,00",
            "SUBTOTAL 495,04",
            "IVA 21% 103,96",
            "TOTAL 599,00",
            "ENTREGADO 600,00",
            "CAMBIO 1,00"
        )

        val guess = ReceiptParser.parse(lines, today)

        assertEquals("Media Markt Valencia", guess.store)
        assertEquals(LocalDate.of(2026, 3, 14), guess.purchaseDate)
        assertEquals(599.0, guess.total!!, 0.001)
    }

    @Test
    fun `reads thousands with a dot and a total on the next line`() {
        val lines = listOf("El Corte Inglés", "15-01-25", "IMPORTE TOTAL", "1.299,95 EUR")

        val guess = ReceiptParser.parse(lines, today)

        assertEquals("El Corte Inglés", guess.store)
        assertEquals(LocalDate.of(2025, 1, 15), guess.purchaseDate)
        assertEquals(1299.95, guess.total!!, 0.001)
    }

    @Test
    fun `ignores future and impossible dates`() {
        assertNull(ReceiptParser.parse(listOf("31/02/2026", "01/01/2030"), today).purchaseDate)
    }

    @Test
    fun `returns nothing it is not sure about`() {
        val guess = ReceiptParser.parse(listOf("12345", "****"), today)

        assertNull(guess.store)
        assertNull(guess.purchaseDate)
        assertNull(guess.total)
    }
}
