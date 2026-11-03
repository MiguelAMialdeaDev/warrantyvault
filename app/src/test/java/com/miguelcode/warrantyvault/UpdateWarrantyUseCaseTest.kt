package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.domain.analytics.AnalyticsEvents
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker
import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.storage.PhotoFileCleaner
import com.miguelcode.warrantyvault.domain.usecase.UpdateWarrantyUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class UpdateWarrantyUseCaseTest {

    private val repository: WarrantyRepository = mockk()
    private val notificationScheduler: NotificationScheduler = mockk(relaxUnitFun = true)
    private val photoFileCleaner: PhotoFileCleaner = mockk(relaxUnitFun = true)
    private val preferencesRepository: PreferencesRepository = mockk()
    private val analytics: AnalyticsTracker = mockk(relaxUnitFun = true)
    private lateinit var useCase: UpdateWarrantyUseCase

    @Before
    fun setup() {
        useCase = UpdateWarrantyUseCase(
            repository,
            notificationScheduler,
            photoFileCleaner,
            preferencesRepository,
            analytics
        )
    }

    private fun warranty(
        id: Long = 1,
        photoPath: String? = null
    ) = Warranty(
        id = id,
        productName = "MacBook Pro",
        category = WarrantyCategory.ELECTRONICS,
        store = "Apple Store",
        price = 2499.0,
        purchaseDate = LocalDate.now().minusMonths(6),
        warrantyEndDate = LocalDate.now().plusMonths(18),
        receiptPhotoPath = photoPath
    )

    @Test
    fun `success cancels the previous alerts of the warranty`() = runTest {
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty())
        coEvery { repository.updateWarranty(any()) } returns Result.Success(Unit)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)

        useCase(warranty())

        verify(exactly = 1) { notificationScheduler.cancelAlerts(1L, any()) }
    }

    @Test
    fun `success with notifications enabled cancels before rescheduling`() = runTest {
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty())
        coEvery { repository.updateWarranty(any()) } returns Result.Success(Unit)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)
        val updated = warranty()

        useCase(updated)

        verifyOrder {
            notificationScheduler.cancelAlerts(1L, any())
            notificationScheduler.scheduleExpirationAlerts(updated)
        }
    }

    @Test
    fun `success with notifications disabled cancels alerts but does not reschedule`() = runTest {
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty())
        coEvery { repository.updateWarranty(any()) } returns Result.Success(Unit)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(false)

        useCase(warranty())

        verify(exactly = 1) { notificationScheduler.cancelAlerts(1L, any()) }
        verify(exactly = 0) { notificationScheduler.scheduleExpirationAlerts(any()) }
    }

    @Test
    fun `success deletes the previous photo when the path changed`() = runTest {
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty(photoPath = "/data/old.jpg"))
        coEvery { repository.updateWarranty(any()) } returns Result.Success(Unit)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)

        useCase(warranty(photoPath = "/data/new.jpg"))

        verify(exactly = 1) { photoFileCleaner.delete("/data/old.jpg") }
    }

    @Test
    fun `success deletes the previous photo when the new warranty has no photo`() = runTest {
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty(photoPath = "/data/old.jpg"))
        coEvery { repository.updateWarranty(any()) } returns Result.Success(Unit)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)

        useCase(warranty(photoPath = null))

        verify(exactly = 1) { photoFileCleaner.delete("/data/old.jpg") }
    }

    @Test
    fun `success does not delete the photo when the path is unchanged`() = runTest {
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty(photoPath = "/data/same.jpg"))
        coEvery { repository.updateWarranty(any()) } returns Result.Success(Unit)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)

        useCase(warranty(photoPath = "/data/same.jpg"))

        verify(exactly = 0) { photoFileCleaner.delete(any()) }
    }

    @Test
    fun `success does not delete anything when there was no previous photo`() = runTest {
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty(photoPath = null))
        coEvery { repository.updateWarranty(any()) } returns Result.Success(Unit)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)

        useCase(warranty(photoPath = "/data/new.jpg"))

        verify(exactly = 0) { photoFileCleaner.delete(any()) }
    }

    @Test
    fun `success does not delete anything when the previous warranty could not be read`() = runTest {
        coEvery { repository.getWarrantyById(1L) } returns Result.Error(NoSuchElementException())
        coEvery { repository.updateWarranty(any()) } returns Result.Success(Unit)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)

        useCase(warranty(photoPath = "/data/new.jpg"))

        verify(exactly = 0) { photoFileCleaner.delete(any()) }
    }

    @Test
    fun `success logs the warranty edited event`() = runTest {
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty())
        coEvery { repository.updateWarranty(any()) } returns Result.Success(Unit)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)

        useCase(warranty())

        verify(exactly = 1) { analytics.logEvent(AnalyticsEvents.WARRANTY_EDITED, any()) }
    }

    @Test
    fun `failed update touches neither alerts nor photos nor analytics`() = runTest {
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty(photoPath = "/data/old.jpg"))
        coEvery { repository.updateWarranty(any()) } returns Result.Error(IllegalStateException("db down"))

        useCase(warranty(photoPath = "/data/new.jpg"))

        verify(exactly = 0) { notificationScheduler.cancelAlerts(any(), any()) }
        verify(exactly = 0) { notificationScheduler.scheduleExpirationAlerts(any()) }
        verify(exactly = 0) { photoFileCleaner.delete(any()) }
        verify(exactly = 0) { analytics.logEvent(any(), any()) }
    }

    @Test
    fun `failed update returns the repository error unchanged`() = runTest {
        val expected = Result.Error(IllegalStateException("db down"))
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty())
        coEvery { repository.updateWarranty(any()) } returns expected

        val result = useCase(warranty())

        assertSame(expected, result)
    }

    @Test
    fun `success returns a success result`() = runTest {
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty())
        coEvery { repository.updateWarranty(any()) } returns Result.Success(Unit)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)

        val result = useCase(warranty())

        assertTrue(result.isSuccess)
    }

    @Test
    fun `the warranty given by the caller is the one handed to the repository`() = runTest {
        val toUpdate = warranty(photoPath = "/data/new.jpg")
        val captured = slot<Warranty>()
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty())
        coEvery { repository.updateWarranty(capture(captured)) } returns Result.Success(Unit)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)

        useCase(toUpdate)

        assertEquals(toUpdate, captured.captured)
    }

    @Test
    fun `editing without changing the end date keeps the delivered alerts`() = runTest {
        // Si se olvidaban, corregir las notas el día del aviso lo volvía a lanzar.
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty())
        coEvery { repository.updateWarranty(any()) } returns Result.Success(Unit)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)

        useCase(warranty().copy(notes = "Factura en el cajón"))

        verify(exactly = 1) { notificationScheduler.cancelAlerts(1L, false) }
    }

    @Test
    fun `changing the end date forgets the delivered alerts`() = runTest {
        coEvery { repository.getWarrantyById(1L) } returns Result.Success(warranty())
        coEvery { repository.updateWarranty(any()) } returns Result.Success(Unit)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)
        val extended = warranty().let { it.copy(warrantyEndDate = it.warrantyEndDate.plusYears(1)) }

        useCase(extended)

        verify(exactly = 1) { notificationScheduler.cancelAlerts(1L, true) }
    }
}
