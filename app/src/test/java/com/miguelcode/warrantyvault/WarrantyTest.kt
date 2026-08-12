package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.notifications.NotificationConstants
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.model.WarrantyStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Cubre las propiedades calculadas de [Warranty] (`status`, `daysRemaining`,
 * `progressFraction`), que dependen de `LocalDate.now()`. Todas las fechas son
 * relativas a hoy para que los tests no caduquen.
 */
class WarrantyTest {

    private fun warranty(
        purchaseDate: LocalDate = LocalDate.now().minusMonths(6),
        warrantyEndDate: LocalDate
    ) = Warranty(
        id = 1,
        productName = "MacBook Pro",
        category = WarrantyCategory.ELECTRONICS,
        store = "Apple Store",
        price = 2499.0,
        purchaseDate = purchaseDate,
        warrantyEndDate = warrantyEndDate
    )

    // region status

    @Test
    fun `status is EXPIRING_SOON when warranty ends today`() {
        val w = warranty(warrantyEndDate = LocalDate.now())

        assertEquals(WarrantyStatus.EXPIRING_SOON, w.status)
    }

    @Test
    fun `status is EXPIRED when warranty ended yesterday`() {
        val w = warranty(warrantyEndDate = LocalDate.now().minusDays(1))

        assertEquals(WarrantyStatus.EXPIRED, w.status)
    }

    @Test
    fun `status is EXPIRING_SOON when warranty ends tomorrow`() {
        val w = warranty(warrantyEndDate = LocalDate.now().plusDays(1))

        assertEquals(WarrantyStatus.EXPIRING_SOON, w.status)
    }

    @Test
    fun `status is EXPIRED when warranty ended long ago`() {
        val w = warranty(warrantyEndDate = LocalDate.now().minusMonths(2))

        assertEquals(WarrantyStatus.EXPIRED, w.status)
    }

    @Test
    fun `status is EXPIRING_SOON at 29 days remaining`() {
        val w = warranty(warrantyEndDate = LocalDate.now().plusDays(29))

        assertEquals(WarrantyStatus.EXPIRING_SOON, w.status)
    }

    /**
     * El día del primer aviso, el estado ya tiene que acompañar.
     *
     * `NotificationConstants.ALERT_DAYS_BEFORE` dispara la primera notificación a los 30
     * días. Antes el umbral de `status` era `now.plusDays(30).isAfter(end)`, estricto, así
     * que `EXPIRING_SOON` cubría 0..29: el usuario recibía el aviso, abría la app y veía
     * la insignia verde de ACTIVE, y el filtro "Por vencer" tampoco la listaba.
     * Corregido en `Warranty.EXPIRING_SOON_DAYS`. Ver docs/AUDIT-2026-08-01.md (P2-7).
     */
    @Test
    fun `status is EXPIRING_SOON at exactly 30 days remaining, matching the first alert`() {
        val w = warranty(warrantyEndDate = LocalDate.now().plusDays(30))

        assertEquals(30L, w.daysRemaining)
        assertEquals(WarrantyStatus.EXPIRING_SOON, w.status)
    }

    /** El umbral se deriva de la constante, así que moverla no deja este test obsoleto. */
    @Test
    fun `the EXPIRING_SOON threshold matches the first notification alert day`() {
        assertEquals(
            NotificationConstants.ALERT_DAYS_BEFORE.max(),
            Warranty.EXPIRING_SOON_DAYS
        )
    }

    @Test
    fun `status is ACTIVE at 31 days remaining`() {
        val w = warranty(warrantyEndDate = LocalDate.now().plusDays(31))

        assertEquals(WarrantyStatus.ACTIVE, w.status)
    }

    @Test
    fun `status is ACTIVE when warranty ends in a year`() {
        val w = warranty(warrantyEndDate = LocalDate.now().plusYears(1))

        assertEquals(WarrantyStatus.ACTIVE, w.status)
    }

    // endregion

    // region daysRemaining

    @Test
    fun `daysRemaining is zero when warranty ends today`() {
        val w = warranty(warrantyEndDate = LocalDate.now())

        assertEquals(0L, w.daysRemaining)
    }

    @Test
    fun `daysRemaining is one when warranty ends tomorrow`() {
        val w = warranty(warrantyEndDate = LocalDate.now().plusDays(1))

        assertEquals(1L, w.daysRemaining)
    }

    @Test
    fun `daysRemaining is negative when warranty already expired`() {
        val w = warranty(warrantyEndDate = LocalDate.now().minusDays(5))

        assertEquals(-5L, w.daysRemaining)
    }

    @Test
    fun `daysRemaining counts full days for a future end date`() {
        val w = warranty(warrantyEndDate = LocalDate.now().plusDays(120))

        assertEquals(120L, w.daysRemaining)
    }

    // endregion

    // region progressFraction

    @Test
    fun `progressFraction is zero for a warranty purchased today`() {
        val w = warranty(
            purchaseDate = LocalDate.now(),
            warrantyEndDate = LocalDate.now().plusDays(365)
        )

        assertEquals(0f, w.progressFraction, 0.0001f)
    }

    @Test
    fun `progressFraction is one half at the midpoint of the warranty`() {
        val w = warranty(
            purchaseDate = LocalDate.now().minusDays(100),
            warrantyEndDate = LocalDate.now().plusDays(100)
        )

        assertEquals(0.5f, w.progressFraction, 0.0001f)
    }

    @Test
    fun `progressFraction is coerced to one and never above for an expired warranty`() {
        val w = warranty(
            purchaseDate = LocalDate.now().minusDays(400),
            warrantyEndDate = LocalDate.now().minusDays(35)
        )

        assertEquals(1f, w.progressFraction, 0.0001f)
        assertTrue(w.progressFraction <= 1f)
    }

    @Test
    fun `progressFraction is one exactly on the end date`() {
        val w = warranty(
            purchaseDate = LocalDate.now().minusDays(365),
            warrantyEndDate = LocalDate.now()
        )

        assertEquals(1f, w.progressFraction, 0.0001f)
    }

    @Test
    fun `progressFraction is one when purchase and end date are the same day`() {
        val today = LocalDate.now()
        val w = warranty(purchaseDate = today, warrantyEndDate = today)

        assertEquals(1f, w.progressFraction, 0.0001f)
    }

    /**
     * Fechas invertidas: el dato está mal introducido, no es una garantía consumida.
     * Devolvía 1 (barra llena), que se lee como "agotada" y ocultaba el error.
     */
    @Test
    fun `progressFraction is zero when end date is before purchase date`() {
        val w = warranty(
            purchaseDate = LocalDate.now(),
            warrantyEndDate = LocalDate.now().minusDays(10)
        )

        assertEquals(0f, w.progressFraction, 0.0001f)
    }

    @Test
    fun `progressFraction is coerced to zero for a purchase date in the future`() {
        val w = warranty(
            purchaseDate = LocalDate.now().plusDays(10),
            warrantyEndDate = LocalDate.now().plusDays(375)
        )

        assertEquals(0f, w.progressFraction, 0.0001f)
    }

    @Test
    fun `progressFraction is three quarters after three of four years elapsed`() {
        val w = warranty(
            purchaseDate = LocalDate.now().minusDays(300),
            warrantyEndDate = LocalDate.now().plusDays(100)
        )

        assertEquals(0.75f, w.progressFraction, 0.0001f)
    }

    // endregion
}
