package com.miguelcode.warrantyvault.billing

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract

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

    /** Los únicos productos que desbloquean premium. */
    val PREMIUM_PRODUCTS = setOf(
        BillingManagerContract.PRODUCT_MONTHLY,
        BillingManagerContract.PRODUCT_YEARLY
    )

    /**
     * Compras realmente pagadas, de esta app y de uno de nuestros productos premium.
     * PENDING (efectivo, transferencia) todavía no está pagada, así que no cuenta ni para
     * dar premium ni para confirmar. Sin el filtro de producto y paquete, cualquier
     * suscripción que se creara en el futuro (o una compra falsa con otro paquete
     * inyectada por un cliente de billing manipulado) daba premium.
     */
    fun activePurchases(
        purchases: List<Purchase>,
        packageName: String,
        isAuthentic: (Purchase) -> Boolean
    ): List<Purchase> =
        purchases.filter { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.packageName == packageName &&
                purchase.products.any { it in PREMIUM_PRODUCTS } &&
                // La firma va la última: es la comprobación cara.
                isAuthentic(purchase)
        }

    /** Compras pagadas que quedaron sin confirmar y hay que reintentar. */
    fun needingAcknowledgement(
        purchases: List<Purchase>,
        packageName: String,
        isAuthentic: (Purchase) -> Boolean
    ): List<Purchase> =
        activePurchases(purchases, packageName, isAuthentic).filterNot { it.isAcknowledged }

    /** Premium activo si Play devuelve al menos una compra pagada válida. */
    fun hasPremium(
        purchases: List<Purchase>,
        packageName: String,
        isAuthentic: (Purchase) -> Boolean
    ): Boolean =
        activePurchases(purchases, packageName, isAuthentic).isNotEmpty()

    /**
     * Fase de precio que se enseña al usuario: la ÚLTIMA del ciclo. Las anteriores pueden
     * ser prueba gratuita u oferta de entrada, así que coger la primera anunciaría "0 €"
     * como precio del plan.
     */
    fun recurringPhase(phases: List<ProductDetails.PricingPhase>): ProductDetails.PricingPhase? =
        phases.lastOrNull()

    /**
     * Plan base: la entrada sin offerId. Es la que da el precio recurrente que se anuncia.
     * Play no garantiza el orden de la lista, así que `firstOrNull()` podía devolver una
     * oferta de entrada en vez del plan base, o al revés.
     */
    fun basePlanOffer(
        offers: List<ProductDetails.SubscriptionOfferDetails>
    ): ProductDetails.SubscriptionOfferDetails? =
        offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull()

    /**
     * Oferta con la que se compra. Play solo devuelve las ofertas a las que el usuario
     * tiene derecho, así que si aparece una oferta propia (prueba gratis, precio de
     * entrada) se usa; si no, el plan base.
     */
    fun purchaseOffer(
        offers: List<ProductDetails.SubscriptionOfferDetails>
    ): ProductDetails.SubscriptionOfferDetails? =
        offers.firstOrNull { it.offerId != null } ?: basePlanOffer(offers)
}
