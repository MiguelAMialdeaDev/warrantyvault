package com.miguelcode.warrantyvault

import android.app.Activity
import app.cash.turbine.test
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsEvents
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract
import com.miguelcode.warrantyvault.domain.billing.SubscriptionPrice
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.feature.premium.PremiumViewModel
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PremiumViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val billingManager: BillingManagerContract = mockk(relaxed = true)
    private val analytics: AnalyticsTracker = mockk(relaxed = true)

    private val stateCode = MutableStateFlow(BillingManagerContract.STATE_IDLE)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val prices = MutableStateFlow<Map<String, SubscriptionPrice>>(emptyMap())
    private val isPremium = MutableStateFlow(false)
    private val preferencesRepository: PreferencesRepository = mockk {
        every { isPremiumFlow } returns isPremium
    }

    private fun createViewModel(): PremiumViewModel {
        every { billingManager.stateCode } returns stateCode
        every { billingManager.errorMessage } returns errorMessage
        every { billingManager.prices } returns prices
        every { billingManager.connect() } just runs
        return PremiumViewModel(billingManager, analytics, preferencesRepository)
    }

    private fun price(amountMicros: Long, formatted: String) =
        SubscriptionPrice(formatted = formatted, amountMicros = amountMicros)

    @Test
    fun `a premium user is recognised even when the billing state is idle`() = runTest {
        // En otra sesión stateCode vuelve a IDLE aunque la suscripción siga activa: la
        // pantalla tiene que leer el derecho real para no ofrecer un segundo plan.
        isPremium.value = true
        val vm = createViewModel()

        vm.isPremium.test {
            assertEquals(true, expectMostRecentItem())
        }
        assertEquals(BillingManagerContract.STATE_IDLE, vm.stateCode.value)
    }

    @Test
    fun `connects to billing on init`() {
        createViewModel()

        verify(exactly = 1) { billingManager.connect() }
    }

    @Test
    fun `stateCode exposes the billing state`() = runTest {
        val vm = createViewModel()
        stateCode.value = BillingManagerContract.STATE_PURCHASED

        vm.stateCode.test {
            assertEquals(BillingManagerContract.STATE_PURCHASED, expectMostRecentItem())
        }
    }

    @Test
    fun `stateCode starts idle before any purchase flow`() = runTest {
        val vm = createViewModel()

        vm.stateCode.test {
            assertEquals(BillingManagerContract.STATE_IDLE, expectMostRecentItem())
        }
    }

    @Test
    fun `errorMessage exposes the billing error`() = runTest {
        val vm = createViewModel()
        errorMessage.value = "Play no disponible"

        vm.errorMessage.test {
            assertEquals("Play no disponible", expectMostRecentItem())
        }
    }

    @Test
    fun `prices expose the catalogue returned by billing`() = runTest {
        val vm = createViewModel()
        prices.value = mapOf(
            BillingManagerContract.PRODUCT_MONTHLY to price(1_990_000L, "1,99 €")
        )

        vm.prices.test {
            val item = expectMostRecentItem()
            assertEquals(1, item.size)
            assertEquals("1,99 €", item[BillingManagerContract.PRODUCT_MONTHLY]?.formatted)
        }
    }

    @Test
    fun `annualSavingsPercent is null while there are no prices`() = runTest {
        val vm = createViewModel()

        vm.annualSavingsPercent.test {
            assertNull(expectMostRecentItem())
        }
    }

    @Test
    fun `annualSavingsPercent is null when only the monthly price is known`() = runTest {
        val vm = createViewModel()
        prices.value = mapOf(
            BillingManagerContract.PRODUCT_MONTHLY to price(1_990_000L, "1,99 €")
        )

        vm.annualSavingsPercent.test {
            assertNull(expectMostRecentItem())
        }
    }

    @Test
    fun `annualSavingsPercent is null when only the yearly price is known`() = runTest {
        val vm = createViewModel()
        prices.value = mapOf(
            BillingManagerContract.PRODUCT_YEARLY to price(14_990_000L, "14,99 €")
        )

        vm.annualSavingsPercent.test {
            assertNull(expectMostRecentItem())
        }
    }

    @Test
    fun `annualSavingsPercent computes the discount of the yearly plan`() = runTest {
        val vm = createViewModel()
        // 1,99 x 12 = 23,88 frente a 14,99 anual -> 37% de ahorro.
        prices.value = mapOf(
            BillingManagerContract.PRODUCT_MONTHLY to price(1_990_000L, "1,99 €"),
            BillingManagerContract.PRODUCT_YEARLY to price(14_990_000L, "14,99 €")
        )

        vm.annualSavingsPercent.test {
            assertEquals(37, expectMostRecentItem())
        }
    }

    @Test
    fun `annualSavingsPercent is null when the yearly plan does not save anything`() = runTest {
        val vm = createViewModel()
        prices.value = mapOf(
            BillingManagerContract.PRODUCT_MONTHLY to price(1_990_000L, "1,99 €"),
            BillingManagerContract.PRODUCT_YEARLY to price(23_880_000L, "23,88 €")
        )

        vm.annualSavingsPercent.test {
            assertNull(expectMostRecentItem())
        }
    }

    @Test
    fun `annualSavingsPercent is null when the yearly plan is more expensive`() = runTest {
        val vm = createViewModel()
        prices.value = mapOf(
            BillingManagerContract.PRODUCT_MONTHLY to price(1_990_000L, "1,99 €"),
            BillingManagerContract.PRODUCT_YEARLY to price(29_990_000L, "29,99 €")
        )

        vm.annualSavingsPercent.test {
            assertNull(expectMostRecentItem())
        }
    }

    @Test
    fun `annualSavingsPercent is null when a price comes back as zero micros`() = runTest {
        val vm = createViewModel()
        prices.value = mapOf(
            BillingManagerContract.PRODUCT_MONTHLY to price(0L, "0,00 €"),
            BillingManagerContract.PRODUCT_YEARLY to price(14_990_000L, "14,99 €")
        )

        vm.annualSavingsPercent.test {
            assertNull(expectMostRecentItem())
        }
    }

    @Test
    fun `launchPurchase delegates to the billing manager with the product id`() {
        val vm = createViewModel()
        val activity: Activity = mockk(relaxed = true)

        vm.launchPurchase(activity, BillingManagerContract.PRODUCT_YEARLY)

        verify(exactly = 1) {
            billingManager.launchPurchase(activity, BillingManagerContract.PRODUCT_YEARLY)
        }
    }

    @Test
    fun `launchPurchase logs the premium clicked event with the product id`() {
        val vm = createViewModel()
        val activity: Activity = mockk(relaxed = true)

        vm.launchPurchase(activity, BillingManagerContract.PRODUCT_MONTHLY)

        verify(exactly = 1) {
            analytics.logEvent(
                AnalyticsEvents.PREMIUM_CLICKED,
                mapOf("product_id" to BillingManagerContract.PRODUCT_MONTHLY)
            )
        }
    }

    @Test
    fun `onErrorShown clears the billing error`() {
        val vm = createViewModel()

        vm.onErrorShown()

        verify(exactly = 1) { billingManager.clearError() }
    }
}
