package com.miguelcode.warrantyvault.domain.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface BillingManagerContract {
    /** 0=Idle, 1=Loading, 2=Purchased, 3=Error */
    val stateCode: StateFlow<Int>

    /** Mensaje ya traducido y apto para enseñar al usuario. null = no hay error pendiente. */
    val errorMessage: StateFlow<String?>

    /**
     * Precios reales de Play indexados por productId. Vacío mientras no haya respuesta
     * de Play: con el mapa vacío no se debe permitir comprar, porque sin ProductDetails
     * la compra fallaría igualmente.
     */
    val prices: StateFlow<Map<String, SubscriptionPrice>>

    /**
     * true cuando Play responde bien pero no devuelve ningún plan (productos sin crear
     * o inactivos en Play Console). Evita dejar el botón en "cargando" para siempre en
     * un caso que no se arregla solo.
     */
    val plansUnavailable: StateFlow<Boolean>

    fun launchPurchase(activity: Activity, productId: String)

    /** Abre la conexión con Play y, al lograrlo, consulta productos y compras. Idempotente. */
    fun connect()

    /**
     * Revalida las suscripciones contra Play y sincroniza el flag local de premium.
     * Hay que llamarla al volver a primer plano: el usuario puede haber cancelado o
     * comprado desde la app de Google Play sin pasar por aquí.
     */
    fun refreshPurchases()

    /** Marca el error como ya mostrado para que no se repita al recomponer. */
    fun clearError()

    companion object {
        const val STATE_IDLE = 0
        const val STATE_LOADING = 1
        const val STATE_PURCHASED = 2
        const val STATE_ERROR = 3
        const val PRODUCT_MONTHLY = "premium_monthly"
        const val PRODUCT_YEARLY = "premium_yearly"
    }
}
