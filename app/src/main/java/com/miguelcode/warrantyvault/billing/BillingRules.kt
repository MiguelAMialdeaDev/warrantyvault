package com.miguelcode.warrantyvault.billing

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase

/**
 * Decisiones de la ruta del dinero, separadas del cliente de Play para poder fijarlas con
 * tests JVM.
 *
 * Invertir cualquiera de estas reglas le quita premium a alguien que ha pagado, regala
 * premium sin cobrar o deja compras sin confirmar (Play las reembolsa a las 72 h). Estaban
 * embebidas en `BillingManager`, que no tenía ni un test: una regresión aquí pasaba la
 * suite entera en verde.
 */
internal object BillingRules {

    /**
     * Compras realmente pagadas. PENDING (efectivo, transferencia) todavía no lo está, así
     * que no cuenta ni para dar premium ni para confirmar.
     */
    fun activePurchases(purchases: List<Purchase>): List<Purchase> =
        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }

    /** Compras pagadas que quedaron sin confirmar y hay que reintentar. */
    fun needingAcknowledgement(purchases: List<Purchase>): List<Purchase> =
        activePurchases(purchases).filterNot { it.isAcknowledged }

    /** Premium activo si Play devuelve al menos una compra pagada. */
    fun hasPremium(purchases: List<Purchase>): Boolean =
        activePurchases(purchases).isNotEmpty()

    /**
     * Fase de precio que se enseña al usuario: la ÚLTIMA del ciclo. Las anteriores pueden
     * ser prueba gratuita u oferta de entrada, así que coger la primera anunciaría "0 €"
     * como precio del plan.
     */
    fun recurringPhase(phases: List<ProductDetails.PricingPhase>): ProductDetails.PricingPhase? =
        phases.lastOrNull()
}
