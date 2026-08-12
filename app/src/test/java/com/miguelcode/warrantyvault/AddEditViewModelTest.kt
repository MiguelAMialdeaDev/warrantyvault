package com.miguelcode.warrantyvault

import app.cash.turbine.test
import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.usecase.AddWarrantyUseCase
import com.miguelcode.warrantyvault.domain.usecase.CheckPremiumLimitUseCase
import com.miguelcode.warrantyvault.domain.usecase.GetWarrantyByIdUseCase
import com.miguelcode.warrantyvault.domain.usecase.UpdateWarrantyUseCase
import com.miguelcode.warrantyvault.feature.addedit.AddEditError
import com.miguelcode.warrantyvault.feature.addedit.AddEditViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class AddEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getWarrantyByIdUseCase: GetWarrantyByIdUseCase = mockk()
    private val addWarrantyUseCase: AddWarrantyUseCase = mockk()
    private val updateWarrantyUseCase: UpdateWarrantyUseCase = mockk()
    private val checkPremiumLimitUseCase: CheckPremiumLimitUseCase = mockk()

    private fun createViewModel(warrantyId: Long? = null): AddEditViewModel {
        return AddEditViewModel(
            warrantyId = warrantyId,
            getWarrantyByIdUseCase = getWarrantyByIdUseCase,
            addWarrantyUseCase = addWarrantyUseCase,
            updateWarrantyUseCase = updateWarrantyUseCase,
            checkPremiumLimitUseCase = checkPremiumLimitUseCase
        )
    }

    /** Rellena el formulario con datos válidos para poder llegar al guard de premium. */
    private fun AddEditViewModel.fillValidForm() {
        onProductNameChanged("MacBook")
        onPurchaseDateChanged(LocalDate.of(2025, 1, 1))
        onWarrantyMonthsChanged("24")
    }

    @Test
    fun `new mode checks premium limit`() = runTest {
        // Caso límite alcanzado: el guard de AddEditViewModel.onSave debe bloquear
        // el guardado. Si se borrase `if (!current.isEditMode && !current.canAdd)`,
        // este test fallaría en el coVerify(exactly = 0).
        coEvery { checkPremiumLimitUseCase() } returns false

        val blocked = createViewModel()
        coVerify(exactly = 1) { checkPremiumLimitUseCase() }
        assertFalse(blocked.state.value.canAdd)

        blocked.fillValidForm()
        assertTrue(blocked.state.value.isFormValid) // el formulario es válido: sólo lo frena el paywall
        blocked.onSave()

        assertEquals(AddEditError.PremiumLimit, blocked.state.value.formError)
        assertFalse(blocked.state.value.isSaved)
        coVerify(exactly = 0) { addWarrantyUseCase(any()) }

        // Caso contrario: con hueco libre, el mismo formulario sí se guarda.
        coEvery { checkPremiumLimitUseCase() } returns true
        coEvery { addWarrantyUseCase(any()) } returns Result.Success(1L)

        val allowed = createViewModel()
        assertTrue(allowed.state.value.canAdd)

        allowed.fillValidForm()
        allowed.onSave()

        assertNull(allowed.state.value.formError)
        assertTrue(allowed.state.value.isSaved)
        coVerify(exactly = 1) { addWarrantyUseCase(any()) }
    }

    @Test
    fun `new mode blocks when limit reached`() = runTest {
        coEvery { checkPremiumLimitUseCase() } returns false

        val vm = createViewModel()

        vm.state.test {
            assertFalse(awaitItem().canAdd)
        }
        // Sin este verify el test pasaría aunque `checkCanAdd()` no se llamase nunca,
        // porque AddEditState.canAdd tiene otro valor por defecto.
        coVerify(exactly = 1) { checkPremiumLimitUseCase() }
    }

    @Test
    fun `edit mode loads existing warranty`() = runTest {
        val warranty = Warranty(
            id = 5,
            productName = "PS5",
            category = WarrantyCategory.GAMING,
            store = "MediaMarkt",
            price = 549.0,
            purchaseDate = LocalDate.of(2024, 1, 15),
            warrantyEndDate = LocalDate.of(2026, 1, 15)
        )
        coEvery { getWarrantyByIdUseCase(5L) } returns Result.Success(warranty)

        val vm = createViewModel(warrantyId = 5L)

        vm.state.test {
            val state = awaitItem()
            assertTrue(state.isEditMode)
            assertEquals("PS5", state.productName)
            assertEquals(WarrantyCategory.GAMING, state.category)
            assertEquals("MediaMarkt", state.store)
        }
    }

    @Test
    fun `validation fails on empty product name`() = runTest {
        coEvery { checkPremiumLimitUseCase() } returns true

        val vm = createViewModel()

        vm.state.test {
            awaitItem()
            vm.onSave()
            assertNotNull(awaitItem().productNameError)
        }
    }

    @Test
    fun `validation fails on missing purchase date`() = runTest {
        coEvery { checkPremiumLimitUseCase() } returns true

        val vm = createViewModel()

        vm.state.test {
            awaitItem()
            vm.onProductNameChanged("MacBook")
            awaitItem()
            vm.onSave()
            assertNotNull(awaitItem().purchaseDateError)
        }
    }

    @Test
    fun `validation fails on missing warranty duration`() = runTest {
        coEvery { checkPremiumLimitUseCase() } returns true

        val vm = createViewModel()

        vm.state.test {
            awaitItem()
            vm.onProductNameChanged("MacBook")
            awaitItem()
            vm.onPurchaseDateChanged(LocalDate.now())
            awaitItem()
            vm.onSave()
            assertNotNull(awaitItem().warrantyMonthsError)
        }
    }

    @Test
    fun `save succeeds with valid data`() = runTest {
        coEvery { checkPremiumLimitUseCase() } returns true
        coEvery { addWarrantyUseCase(any()) } returns Result.Success(1L)

        val vm = createViewModel()

        vm.state.test {
            awaitItem()
            vm.onProductNameChanged("MacBook")
            awaitItem()
            vm.onPurchaseDateChanged(LocalDate.now())
            awaitItem()
            vm.onWarrantyMonthsChanged("24")
            awaitItem()
            vm.onSave()
            // Skip intermediate saving state
            val finalState = expectMostRecentItem()
            assertTrue(finalState.isSaved)
        }

        coVerify { addWarrantyUseCase(any()) }
    }

    @Test
    fun `end date recalculates on duration change`() = runTest {
        coEvery { checkPremiumLimitUseCase() } returns true

        val vm = createViewModel()
        val purchaseDate = LocalDate.of(2025, 1, 1)

        vm.state.test {
            awaitItem()
            vm.onPurchaseDateChanged(purchaseDate)
            vm.onWarrantyMonthsChanged("12")
            // onWarrantyMonthsChanged dispara dos updates (el valor + recalcular fecha fin).
            // expectMostRecentItem toma el último estado coherente.
            val state = expectMostRecentItem()
            assertEquals(LocalDate.of(2026, 1, 1), state.warrantyEndDate)
        }
    }

    @Test
    fun `end date recalculates with years mode`() = runTest {
        coEvery { checkPremiumLimitUseCase() } returns true

        val vm = createViewModel()
        val purchaseDate = LocalDate.of(2025, 1, 1)

        vm.state.test {
            awaitItem()
            vm.onPurchaseDateChanged(purchaseDate)
            vm.onIsYearsChanged(true)
            vm.onWarrantyMonthsChanged("2")
            val state = expectMostRecentItem()
            assertEquals(LocalDate.of(2027, 1, 1), state.warrantyEndDate)
        }
    }

    @Test
    fun `duration exceeding 50 years is rejected`() = runTest {
        coEvery { checkPremiumLimitUseCase() } returns true

        val vm = createViewModel()

        vm.state.test {
            awaitItem()
            vm.onPurchaseDateChanged(LocalDate.of(2025, 1, 1))
            vm.onWarrantyMonthsChanged("601") // > MAX_WARRANTY_MONTHS
            val state = expectMostRecentItem()
            assertNotNull(state.warrantyMonthsError)
        }
    }

    @Test
    fun `double tap save does not duplicate insert`() = runTest {
        coEvery { checkPremiumLimitUseCase() } returns true

        // El guard real contra el doble toque es `isSaving` (AddEditViewModel:162).
        // Con un stub que devuelve al instante, el launch termina dentro del primer
        // onSave() y el segundo saldría por `isSaved`, dejando `isSaving` sin cubrir.
        // Este gate mantiene el primer guardado EN VUELO, como Room en un dispositivo
        // real, para que el segundo tap tenga que chocar contra `isSaving`.
        val inFlight = CompletableDeferred<Unit>()
        coEvery { addWarrantyUseCase(any()) } coAnswers {
            inFlight.await()
            Result.Success(1L)
        }

        val vm = createViewModel()
        vm.fillValidForm()

        vm.onSave()

        // Primer guardado en vuelo: isSaving true y, crucialmente, isSaved todavía
        // false. El segundo tap sólo puede ser frenado por isSaving.
        assertTrue(vm.state.value.isSaving)
        assertFalse(vm.state.value.isSaved)

        vm.onSave() // segundo tap mientras el primero sigue sin resolverse

        // Aún con el primer insert sin terminar, no debe haber un segundo.
        coVerify(exactly = 1) { addWarrantyUseCase(any()) }

        inFlight.complete(Unit)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value.isSaved)
        assertFalse(vm.state.value.isSaving)
        // Solo un insert aunque se haya llamado onSave dos veces.
        coVerify(exactly = 1) { addWarrantyUseCase(any()) }
    }

    @Test
    fun `category updates correctly`() = runTest {
        coEvery { checkPremiumLimitUseCase() } returns true

        val vm = createViewModel()

        vm.state.test {
            awaitItem()
            vm.onCategoryChanged(WarrantyCategory.GAMING)
            assertEquals(WarrantyCategory.GAMING, awaitItem().category)
        }
    }
}
