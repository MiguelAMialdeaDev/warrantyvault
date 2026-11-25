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
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
            checkPremiumLimitUseCase = checkPremiumLimitUseCase,
            // Same scheduler as Main: the photo copy only runs when a test advances it, so
            // no coroutine can outlive the test and resume after resetMain().
            ioDispatcher = StandardTestDispatcher(mainDispatcherRule.testDispatcher.scheduler)
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
    fun `edit mode rebuilds the duration the user typed when the end date was clipped`() = runTest {
        // 31/08 + 6 meses = 28/02: plusMonths recorta el día y MONTHS.between no revierte
        // el recorte, así que devolvía 5. El campo mostraba una duración menor y, al
        // recalcular, persistía un vencimiento un mes más corto que el real.
        val warranty = warrantyEndingOn(
            purchase = LocalDate.of(2025, 8, 31),
            end = LocalDate.of(2026, 2, 28)
        )
        coEvery { getWarrantyByIdUseCase(5L) } returns Result.Success(warranty)

        val vm = createViewModel(warrantyId = 5L)

        vm.state.test {
            val state = awaitItem()
            assertEquals("6", state.warrantyMonths)
            assertFalse(state.isYears)
        }
    }

    @Test
    fun `edit mode rebuilds a one month duration that was clipped to zero`() = runTest {
        // 31/01 + 1 mes = 28/02. Aquí between devolvía 0, el campo mostraba 0 y el
        // formulario se bloqueaba con el error de "debe ser positivo".
        val warranty = warrantyEndingOn(
            purchase = LocalDate.of(2026, 1, 31),
            end = LocalDate.of(2026, 2, 28)
        )
        coEvery { getWarrantyByIdUseCase(5L) } returns Result.Success(warranty)

        val vm = createViewModel(warrantyId = 5L)

        vm.state.test {
            val state = awaitItem()
            assertEquals("1", state.warrantyMonths)
            assertNull(state.warrantyMonthsError)
        }
    }

    @Test
    fun `edit mode keeps showing whole years as years`() = runTest {
        val warranty = warrantyEndingOn(
            purchase = LocalDate.of(2024, 1, 15),
            end = LocalDate.of(2026, 1, 15)
        )
        coEvery { getWarrantyByIdUseCase(5L) } returns Result.Success(warranty)

        val vm = createViewModel(warrantyId = 5L)

        vm.state.test {
            val state = awaitItem()
            assertEquals("2", state.warrantyMonths)
            assertTrue(state.isYears)
        }
    }

    @Test
    fun `edit mode shows a three decimal price that survives saving again`() = runTest {
        coEvery { getWarrantyByIdUseCase(5L) } returns Result.Success(
            warrantyEndingOn(LocalDate.of(2024, 1, 15), LocalDate.of(2026, 1, 15))
                .copy(price = 1299.999)
        )

        val vm = createViewModel(warrantyId = 5L)

        assertEquals("1300", vm.state.value.price)
    }

    @Test
    fun `save is ignored while the receipt photo is still being copied`() = runTest {
        coEvery { checkPremiumLimitUseCase() } returns true
        coEvery { addWarrantyUseCase(any()) } returns Result.Success(1L)
        val vm = createViewModel()
        vm.fillValidForm()

        // The copy is queued on the test IO dispatcher and stays pending until we advance it.
        vm.onPhotoPicked(mockk(relaxed = true)) { "/receipts/ticket.jpg" }
        assertTrue(vm.state.value.isPhotoLoading)

        vm.onSave()

        coVerify(exactly = 0) { addWarrantyUseCase(any()) }
        assertFalse(vm.state.value.isSaving)

        // Let the copy finish inside the test, so nothing resumes after it ends.
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.state.value.isPhotoLoading)
        assertEquals("/receipts/ticket.jpg", vm.state.value.receiptPhotoPath)
    }

    private fun warrantyEndingOn(purchase: LocalDate, end: LocalDate) = Warranty(
        id = 5,
        productName = "PS5",
        category = WarrantyCategory.GAMING,
        store = "MediaMarkt",
        price = 549.0,
        purchaseDate = purchase,
        warrantyEndDate = end
    )

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

    @Test
    fun `editing a warranty that no longer exists closes the screen`() = runTest {
        coEvery { getWarrantyByIdUseCase(42L) } returns Result.Error(NoSuchElementException("x"))

        val vm = createViewModel(warrantyId = 42L)

        assertTrue(vm.state.value.notFound)
        assertFalse(vm.state.value.isEditMode)
    }

    @Test
    fun `a new warranty is always inserted with a generated id`() = runTest {
        // Aunque llegue un id (pantalla de editar de una garantía que no se cargó), el
        // alta no puede forzarlo: con REPLACE habría pisado otra fila.
        coEvery { getWarrantyByIdUseCase(7L) } returns Result.Error(NoSuchElementException("x"))
        coEvery { checkPremiumLimitUseCase() } returns true
        val saved = slot<Warranty>()
        coEvery { addWarrantyUseCase(capture(saved)) } returns Result.Success(1L)

        val vm = createViewModel(warrantyId = 7L)
        vm.fillValidForm()
        vm.onSave()

        assertEquals(0L, saved.captured.id)
    }

    @Test
    fun `store notes and price are capped and the price keeps only numbers`() = runTest {
        coEvery { checkPremiumLimitUseCase() } returns true
        val vm = createViewModel()

        vm.onStoreChanged("t".repeat(500))
        vm.onNotesChanged("n".repeat(10_000))
        vm.onPriceChanged("1e308")

        assertEquals(AddEditViewModel.MAX_STORE_LENGTH, vm.state.value.store.length)
        assertEquals(AddEditViewModel.MAX_NOTES_LENGTH, vm.state.value.notes.length)
        assertEquals("1308", vm.state.value.price)
    }
}
