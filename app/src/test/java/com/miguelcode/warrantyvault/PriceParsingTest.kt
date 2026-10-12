package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.domain.util.toPriceInput
import com.miguelcode.warrantyvault.domain.util.toPriceOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * El campo de precio se escribe a mano y en España el separador decimal es la coma.
 * `toDoubleOrNull()` solo entiende el punto, así que "1299,99" devolvía `null` y el precio
 * se descartaba en silencio: la garantía se guardaba sin él y el usuario no veía ningún
 * aviso. Estos tests fijan el comportamiento para que no vuelva a pasar.
 */
class PriceParsingTest {

    @Test
    fun `parses a comma as decimal separator`() {
        assertEquals(1299.99, "1299,99".toPriceOrNull()!!, 0.001)
    }

    @Test
    fun `parses a dot as decimal separator`() {
        assertEquals(1299.99, "1299.99".toPriceOrNull()!!, 0.001)
    }

    @Test
    fun `parses a plain integer`() {
        assertEquals(1299.0, "1299".toPriceOrNull()!!, 0.001)
    }

    @Test
    fun `parses spanish format with dot as thousands and comma as decimal`() {
        assertEquals(1299.99, "1.299,99".toPriceOrNull()!!, 0.001)
    }

    @Test
    fun `parses english format with comma as thousands and dot as decimal`() {
        assertEquals(1299.99, "1,299.99".toPriceOrNull()!!, 0.001)
    }

    @Test
    fun `treats a lone comma before three digits as a thousands separator`() {
        assertEquals(1299.0, "1,299".toPriceOrNull()!!, 0.001)
    }

    @Test
    fun `treats a lone comma before two digits as a decimal separator`() {
        assertEquals(1.29, "1,29".toPriceOrNull()!!, 0.001)
    }

    @Test
    fun `treats a lone dot before three digits as a thousands separator`() {
        assertEquals(1299.0, "1.299".toPriceOrNull()!!, 0.001)
    }

    @Test
    fun `treats repeated dots as thousands separators`() {
        assertEquals(1299000.0, "1.299.000".toPriceOrNull()!!, 0.001)
    }

    @Test
    fun `treats a lone dot before two digits as a decimal separator`() {
        assertEquals(1.29, "1.29".toPriceOrNull()!!, 0.001)
    }

    @Test
    fun `ignores surrounding whitespace`() {
        assertEquals(49.9, "  49,9  ".toPriceOrNull()!!, 0.001)
    }

    @Test
    fun `ignores the non breaking space some keyboards insert`() {
        assertEquals(1299.99, "1 299,99".toPriceOrNull()!!, 0.001)
    }

    @Test
    fun `returns null for an empty or blank field`() {
        assertNull("".toPriceOrNull())
        assertNull("   ".toPriceOrNull())
    }

    @Test
    fun `returns null for text that is not a number`() {
        assertNull("abc".toPriceOrNull())
        assertNull("12abc".toPriceOrNull())
    }

    @Test
    fun `returns null for a negative price`() {
        assertNull("-10".toPriceOrNull())
    }

    @Test
    fun `accepts zero`() {
        assertEquals(0.0, "0".toPriceOrNull()!!, 0.001)
    }

    // Editar rellena el campo con el precio guardado y lo vuelve a parsear al guardar.
    // Con Double.toString(), 1299.999 se escribía "1299.999" y volvía como 1299999.

    @Test
    fun `price input round trips through the parser`() {
        listOf(0.0, 0.5, 49.9, 549.0, 1000.0, 1299.99, 1_000_000.0, 1.0E7).forEach { price ->
            assertEquals(price, price.toPriceInput().toPriceOrNull()!!, 0.001)
        }
    }

    @Test
    fun `price input rounds three decimals instead of multiplying by a thousand`() {
        assertEquals("1300", 1299.999.toPriceInput())
        assertEquals(1300.0, 1299.999.toPriceInput().toPriceOrNull()!!, 0.001)
    }

    @Test
    fun `price input never uses scientific notation`() {
        assertEquals("10000000", 1.0E7.toPriceInput())
    }
}
