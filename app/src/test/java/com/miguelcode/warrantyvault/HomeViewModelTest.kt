package com.miguelcode.warrantyvault

import app.cash.turbine.test
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.usecase.CheckPremiumLimitUseCase
import com.miguelcode.warrantyvault.domain.usecase.GetWarrantiesUseCase
import com.miguelcode.warrantyvault.feature.home.HomeViewModel
import com.miguelcode.warrantyvault.feature.home.MAX_FREE_WARRANTIES
import com.miguelcode.warrantyvault.feature.home.WarrantyFilter
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getWarrantiesUseCase: GetWarrantiesUseCase = mockk()
    private val preferencesRepository: PreferencesRepository = mockk()

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(getWarrantiesUseCase, preferencesRepository)
    }

    private fun sampleWarranty(
        id: Long = 1,
        name: String = "MacBook",
        endDate: LocalDate = LocalDate.now().plusMonths(6)
    ) = Warranty(
        id = id,
        productName = name,
        category = WarrantyCategory.ELECTRONICS,
        store = "Apple Store",
        purchaseDate = LocalDate.now().minusMonths(6),
        warrantyEndDate = endDate
    )

    @Test
    fun `initial state is loading`() = runTest {
        // Un flow que nunca emite deja el ViewModel en su estado inicial: así se
        // comprueba de verdad que HomeState.isLoading arranca en true. El test
        // anterior usaba flowOf(...), que con UnconfinedTestDispatcher ya ha emitido
        // antes de la primera aserción, y por eso asertaba justo lo contrario de su
        // nombre.
        every { getWarrantiesUseCase() } returns MutableSharedFlow<List<Warranty>>()
        every { preferencesRepository.isPremiumFlow } returns MutableSharedFlow<Boolean>()

        val vm = createViewModel()

        vm.state.test {
            val state = awaitItem()
            assertTrue(state.isLoading)
            assertTrue(state.warranties.isEmpty())
        }
    }

    @Test
    fun `loading ends once the first emission arrives`() = runTest {
        every { getWarrantiesUseCase() } returns flowOf(emptyList())
        every { preferencesRepository.isPremiumFlow } returns flowOf(false)

        val vm = createViewModel()

        vm.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.warranties.isEmpty())
        }
    }

    @Test
    fun `loads warranties from use case`() = runTest {
        val warranties = listOf(sampleWarranty(1, "MacBook"), sampleWarranty(2, "iPhone"))
        every { getWarrantiesUseCase() } returns flowOf(warranties)
        every { preferencesRepository.isPremiumFlow } returns flowOf(false)

        val vm = createViewModel()

        vm.state.test {
            val state = awaitItem()
            assertEquals(2, state.warranties.size)
            assertEquals(2, state.usedSlots)
        }
    }

    @Test
    fun `filter updates correctly`() = runTest {
        every { getWarrantiesUseCase() } returns flowOf(emptyList())
        every { preferencesRepository.isPremiumFlow } returns flowOf(false)

        val vm = createViewModel()

        vm.state.test {
            awaitItem() // initial
            vm.onFilterSelected(WarrantyFilter.EXPIRED)
            assertEquals(WarrantyFilter.EXPIRED, awaitItem().selectedFilter)
        }
    }

    @Test
    fun `search query updates correctly`() = runTest {
        every { getWarrantiesUseCase() } returns flowOf(emptyList())
        every { preferencesRepository.isPremiumFlow } returns flowOf(false)

        val vm = createViewModel()

        vm.state.test {
            awaitItem()
            vm.onSearchQueryChanged("Mac")
            assertEquals("Mac", awaitItem().searchQuery)
        }
    }

    @Test
    fun `premium status is reflected`() = runTest {
        every { getWarrantiesUseCase() } returns flowOf(emptyList())
        every { preferencesRepository.isPremiumFlow } returns flowOf(true)

        val vm = createViewModel()

        vm.state.test {
            assertTrue(awaitItem().isPremium)
        }
    }

    @Test
    fun `filteredWarranties filters by status`() = runTest {
        val active = sampleWarranty(1, "Active", LocalDate.now().plusMonths(6))
        val expired = sampleWarranty(2, "Expired", LocalDate.now().minusDays(5))
        every { getWarrantiesUseCase() } returns flowOf(listOf(active, expired))
        every { preferencesRepository.isPremiumFlow } returns flowOf(false)

        val vm = createViewModel()

        vm.state.test {
            awaitItem()
            vm.onFilterSelected(WarrantyFilter.EXPIRED)
            val state = awaitItem()
            assertEquals(1, state.filteredWarranties.size)
            assertEquals("Expired", state.filteredWarranties[0].productName)
        }
    }

    @Test
    fun `filteredWarranties filters by search query`() = runTest {
        val w1 = sampleWarranty(1, "MacBook Pro")
        val w2 = sampleWarranty(2, "PlayStation 5")
        every { getWarrantiesUseCase() } returns flowOf(listOf(w1, w2))
        every { preferencesRepository.isPremiumFlow } returns flowOf(false)

        val vm = createViewModel()

        vm.state.test {
            awaitItem()
            vm.onSearchQueryChanged("Mac")
            val state = awaitItem()
            assertEquals(1, state.filteredWarranties.size)
            assertEquals("MacBook Pro", state.filteredWarranties[0].productName)
        }
    }

    @Test
    fun `showPremiumBanner true when free and 2+ slots`() = runTest {
        val warranties = listOf(sampleWarranty(1), sampleWarranty(2))
        every { getWarrantiesUseCase() } returns flowOf(warranties)
        every { preferencesRepository.isPremiumFlow } returns flowOf(false)

        val vm = createViewModel()

        vm.state.test {
            assertTrue(awaitItem().showPremiumBanner)
        }
    }

    /**
     * MAX_FREE_WARRANTIES está duplicado: `feature/home/HomeState.kt:37` (usada por
     * `limitReached` → FAB de la Home) y `CheckPremiumLimitUseCase:16` (usada por el
     * alta real). Nada ata los dos valores: subir el plan gratis a 5 en el dominio
     * dejaría la Home bloqueando el FAB en la 3ª garantía. Este test hace ruidosa
     * esa divergencia. El arreglo definitivo es borrar la constante de :feature:home
     * y consumir la de :core:domain (P2-24 de la auditoría).
     */
    @Test
    fun `home free limit matches domain free limit`() {
        assertEquals(CheckPremiumLimitUseCase.MAX_FREE_WARRANTIES, MAX_FREE_WARRANTIES)
    }

    @Test
    fun `limitReached flips exactly at the free limit`() = runTest {
        val limit = CheckPremiumLimitUseCase.MAX_FREE_WARRANTIES
        val warranties = List(limit) { sampleWarranty(id = it.toLong() + 1) }

        every { getWarrantiesUseCase() } returns flowOf(warranties.dropLast(1))
        every { preferencesRepository.isPremiumFlow } returns flowOf(false)
        assertFalse(createViewModel().state.value.limitReached) // limit - 1 → aún cabe

        every { getWarrantiesUseCase() } returns flowOf(warranties)
        assertTrue(createViewModel().state.value.limitReached) // limit → bloqueado

        every { preferencesRepository.isPremiumFlow } returns flowOf(true)
        assertFalse(createViewModel().state.value.limitReached) // premium → nunca bloquea
    }

    @Test
    fun `showPremiumBanner false when premium`() = runTest {
        val warranties = listOf(sampleWarranty(1), sampleWarranty(2), sampleWarranty(3))
        every { getWarrantiesUseCase() } returns flowOf(warranties)
        every { preferencesRepository.isPremiumFlow } returns flowOf(true)

        val vm = createViewModel()

        vm.state.test {
            assertFalse(awaitItem().showPremiumBanner)
        }
    }
}
