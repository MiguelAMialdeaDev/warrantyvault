package com.miguelcode.warrantyvault

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.miguelcode.warrantyvault.billing.BillingRules
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La ruta del dinero no tenía ni un test: invertir cualquiera de estas reglas le quita
 * premium a alguien que ha pagado, lo regala sin cobrar, o deja compras sin confirmar
 * (Play las reembolsa a las 72 h), y la suite seguía en verde.
 */
class BillingRulesTest {

    private fun purchase(state: Int, acknowledged: Boolean): Purchase = mockk {
        every { purchaseState } returns state
        every { isAcknowledged } returns acknowledged
    }

    private fun phase(price: String): ProductDetails.PricingPhase = mockk {
        every { formattedPrice } returns price
    }

    @Test
    fun `a paid purchase grants premium`() {
        val purchases = listOf(purchase(Purchase.PurchaseState.PURCHASED, acknowledged = true))

        assertTrue(BillingRules.hasPremium(purchases))
    }

    @Test
    fun `no purchases means no premium`() {
        assertFalse(BillingRules.hasPremium(emptyList()))
    }

    @Test
    fun `a pending purchase does not grant premium`() {
        // Pago diferido en efectivo o transferencia: todavía no hay dinero.
        val purchases = listOf(purchase(Purchase.PurchaseState.PENDING, acknowledged = false))

        assertFalse(BillingRules.hasPremium(purchases))
    }

    @Test
    fun `an unspecified purchase does not grant premium`() {
        val purchases = listOf(purchase(Purchase.PurchaseState.UNSPECIFIED_STATE, acknowledged = false))

        assertFalse(BillingRules.hasPremium(purchases))
    }

    @Test
    fun `an unacknowledged paid purchase needs acknowledgement`() {
        val pending = purchase(Purchase.PurchaseState.PURCHASED, acknowledged = false)

        assertEquals(listOf(pending), BillingRules.needingAcknowledgement(listOf(pending)))
    }

    @Test
    fun `an already acknowledged purchase is not acknowledged again`() {
        val done = purchase(Purchase.PurchaseState.PURCHASED, acknowledged = true)

        assertTrue(BillingRules.needingAcknowledgement(listOf(done)).isEmpty())
    }

    @Test
    fun `a pending purchase is never acknowledged`() {
        // Confirmar una compra que aún no se ha pagado la daría por buena.
        val pending = purchase(Purchase.PurchaseState.PENDING, acknowledged = false)

        assertTrue(BillingRules.needingAcknowledgement(listOf(pending)).isEmpty())
    }

    @Test
    fun `only paid purchases are considered active`() {
        val paid = purchase(Purchase.PurchaseState.PURCHASED, acknowledged = true)
        val pending = purchase(Purchase.PurchaseState.PENDING, acknowledged = false)

        assertEquals(listOf(paid), BillingRules.activePurchases(listOf(paid, pending)))
    }

    @Test
    fun `the recurring price is the last pricing phase not the intro offer`() {
        // La primera fase suele ser prueba gratuita: anunciarla como precio del plan
        // enseñaría "0 €" a alguien que va a pagar 9,99 €.
        val free = phase("0,00 €")
        val real = phase("9,99 €")

        assertEquals(real, BillingRules.recurringPhase(listOf(free, real)))
    }

    @Test
    fun `a single pricing phase is the recurring one`() {
        val only = phase("9,99 €")

        assertEquals(only, BillingRules.recurringPhase(listOf(only)))
    }

    @Test
    fun `no pricing phases means no price`() {
        assertNull(BillingRules.recurringPhase(emptyList()))
    }

    private fun offer(id: String?): ProductDetails.SubscriptionOfferDetails = mockk {
        every { offerId } returns id
    }

    @Test
    fun `the advertised price comes from the base plan even if an offer is listed first`() {
        val trial = offer("free-trial")
        val base = offer(null)

        assertEquals(base, BillingRules.basePlanOffer(listOf(trial, base)))
    }

    @Test
    fun `an eligible offer is used to purchase whatever its position`() {
        val base = offer(null)
        val trial = offer("free-trial")

        assertEquals(trial, BillingRules.purchaseOffer(listOf(base, trial)))
    }

    @Test
    fun `without offers the purchase uses the base plan`() {
        val base = offer(null)

        assertEquals(base, BillingRules.purchaseOffer(listOf(base)))
    }

    @Test
    fun `no offers means nothing to buy`() {
        assertNull(BillingRules.purchaseOffer(emptyList()))
    }
}
