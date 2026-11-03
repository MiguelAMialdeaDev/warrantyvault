package com.miguelcode.warrantyvault.feature.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsEvents
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract
import com.miguelcode.warrantyvault.domain.billing.SubscriptionPrice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PremiumViewModel(
    private val billingManager: BillingManagerContract,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    val stateCode: StateFlow<Int> = billingManager.stateCode
    val errorMessage: StateFlow<String?> = billingManager.errorMessage
    val prices: StateFlow<Map<String, SubscriptionPrice>> = billingManager.prices
    val plansUnavailable: StateFlow<Boolean> = billingManager.plansUnavailable

    /**
     * Porcentaje de ahorro del plan anual, calculado con los precios reales de Play.
     * null cuando aún no hay precios o cuando el anual no ahorra nada: en ese caso la
     * pantalla esconde la insignia en vez de prometer un descuento que no existe.
     */
    val annualSavingsPercent: StateFlow<Int?> = billingManager.prices
        .map(::annualSavings)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // connect() es idempotente: si ya hay conexión solo revalida las compras.
        billingManager.connect()
    }

    fun launchPurchase(activity: Activity, productId: String) {
        analytics.logEvent(
            AnalyticsEvents.PREMIUM_CLICKED,
            mapOf("product_id" to productId)
        )
        billingManager.launchPurchase(activity, productId)
    }

    fun onErrorShown() {
        billingManager.clearError()
    }

    private fun annualSavings(prices: Map<String, SubscriptionPrice>): Int? {
        val monthly = prices[BillingManagerContract.PRODUCT_MONTHLY]?.amountMicros ?: return null
        val yearly = prices[BillingManagerContract.PRODUCT_YEARLY]?.amountMicros ?: return null
        if (monthly <= 0L || yearly <= 0L) return null
        val yearAtMonthlyRate = monthly * MONTHS_PER_YEAR
        if (yearly >= yearAtMonthlyRate) return null
        return (100 * (yearAtMonthlyRate - yearly) / yearAtMonthlyRate).toInt()
    }

    private companion object {
        const val MONTHS_PER_YEAR = 12
    }
}
