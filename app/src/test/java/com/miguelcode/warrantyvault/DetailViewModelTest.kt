package com.miguelcode.warrantyvault

import app.cash.turbine.test
import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.usecase.DeleteWarrantyUseCase
import com.miguelcode.warrantyvault.feature.detail.DetailViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val warrantyRepository: WarrantyRepository = mockk()
    private val deleteWarrantyUseCase: DeleteWarrantyUseCase = mockk()

    private val sampleWarranty = Warranty(
        id = 1,
        productName = "MacBook Pro",
        category = WarrantyCategory.ELECTRONICS,
        store = "Apple Store",
        price = 2499.0,
        purchaseDate = LocalDate.now().minusMonths(6),
        warrantyEndDate = LocalDate.now().plusMonths(18)
    )

    @Test
    fun `loads warranty on init`() = runTest {
        every { warrantyRepository.getWarrantyByIdFlow(1L) } returns flowOf(sampleWarranty)

        val vm = DetailViewModel(1L, warrantyRepository, deleteWarrantyUseCase)

        vm.state.test {
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertNotNull(state.warranty)
            assertEquals("MacBook Pro", state.warranty?.productName)
        }
    }

    @Test
    fun `null warranty leaves state empty`() = runTest {
        every { warrantyRepository.getWarrantyByIdFlow(99L) } returns flowOf(null)

        val vm = DetailViewModel(99L, warrantyRepository, deleteWarrantyUseCase)

        vm.state.test {
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertNull(state.warranty)
        }
    }

    @Test
    fun `delete sets isDeleted to true on success`() = runTest {
        every { warrantyRepository.getWarrantyByIdFlow(1L) } returns flowOf(sampleWarranty)
        coEvery { deleteWarrantyUseCase(sampleWarranty) } returns Result.Success(Unit)

        val vm = DetailViewModel(1L, warrantyRepository, deleteWarrantyUseCase)

        vm.state.test {
            expectMostRecentItem()
            vm.onDelete()
            val state = expectMostRecentItem()
            assertTrue(state.isDeleted)
        }
    }

    @Test
    fun `delete shows error on failure`() = runTest {
        every { warrantyRepository.getWarrantyByIdFlow(1L) } returns flowOf(sampleWarranty)
        coEvery { deleteWarrantyUseCase(sampleWarranty) } returns Result.Error(
            RuntimeException("DB error")
        )

        val vm = DetailViewModel(1L, warrantyRepository, deleteWarrantyUseCase)

        vm.state.test {
            expectMostRecentItem()
            vm.onDelete()
            val state = expectMostRecentItem()
            assertFalse(state.isDeleted)
            assertEquals("DB error", state.error)
        }
    }

    @Test
    fun `delete does nothing when no warranty loaded`() = runTest {
        every { warrantyRepository.getWarrantyByIdFlow(1L) } returns flowOf(null)

        val vm = DetailViewModel(1L, warrantyRepository, deleteWarrantyUseCase)

        vm.state.test {
            val before = expectMostRecentItem()
            assertNull(before.warranty)

            vm.onDelete() // no hay warranty cargada: debe salir por el early return

            // Ni una sola emisión nueva: el early return ocurre antes del
            // `_state.update { isDeleting = true }`.
            expectNoEvents()
        }

        // El estado sigue intacto: ni isDeleting, ni isDeleted, ni error.
        val after = vm.state.value
        assertFalse(after.isDeleting)
        assertFalse(after.isDeleted)
        assertNull(after.error)

        // La aserción que de verdad protege el early return de DetailViewModel.onDelete:
        // el use case (que borra la foto y cancela alertas) no debe invocarse jamás.
        // El mock no tiene stub, así que si se invocara MockK lanzaría; pero esa
        // excepción ocurre dentro de viewModelScope y no tumbaría el test: por eso
        // hace falta el coVerify explícito.
        coVerify(exactly = 0) { deleteWarrantyUseCase(any()) }
    }
}
