package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.domain.analytics.AnalyticsEvents
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker
import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.storage.PhotoFileCleaner
import com.miguelcode.warrantyvault.domain.usecase.DeleteWarrantyUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DeleteWarrantyUseCaseTest {

    private val repository: WarrantyRepository = mockk()
    private val notificationScheduler: NotificationScheduler = mockk(relaxUnitFun = true)
    private val photoFileCleaner: PhotoFileCleaner = mockk(relaxUnitFun = true)
    private val analytics: AnalyticsTracker = mockk(relaxUnitFun = true)
    private lateinit var useCase: DeleteWarrantyUseCase

    @Before
    fun setup() {
        useCase = DeleteWarrantyUseCase(
            repository,
            notificationScheduler,
            photoFileCleaner,
            analytics
        )
    }

    private fun warranty(
        id: Long = 5,
        photoPath: String? = "/data/receipt.jpg"
    ) = Warranty(
        id = id,
        productName = "PlayStation 5",
        category = WarrantyCategory.GAMING,
        store = "MediaMarkt",
        price = 549.0,
        purchaseDate = LocalDate.now().minusMonths(3),
        warrantyEndDate = LocalDate.now().plusMonths(21),
        receiptPhotoPath = photoPath
    )

    @Test
    fun `success cancels the alerts of the deleted warranty`() = runTest {
        coEvery { repository.deleteWarranty(any()) } returns Result.Success(Unit)

        useCase(warranty(id = 5))

        verify(exactly = 1) { notificationScheduler.cancelAlerts(5L) }
    }

    @Test
    fun `success deletes the receipt photo of the deleted warranty`() = runTest {
        coEvery { repository.deleteWarranty(any()) } returns Result.Success(Unit)

        useCase(warranty(photoPath = "/data/receipt.jpg"))

        verify(exactly = 1) { photoFileCleaner.delete("/data/receipt.jpg") }
    }

    @Test
    fun `success logs the warranty deleted event`() = runTest {
        coEvery { repository.deleteWarranty(any()) } returns Result.Success(Unit)

        useCase(warranty())

        verify(exactly = 1) { analytics.logEvent(AnalyticsEvents.WARRANTY_DELETED, any()) }
    }

    @Test
    fun `success returns a success result`() = runTest {
        coEvery { repository.deleteWarranty(any()) } returns Result.Success(Unit)

        val result = useCase(warranty())

        assertTrue(result.isSuccess)
    }

    @Test
    fun `error result does not cancel the alerts`() = runTest {
        coEvery { repository.deleteWarranty(any()) } returns Result.Error(IllegalStateException("db down"))

        useCase(warranty())

        verify(exactly = 0) { notificationScheduler.cancelAlerts(any()) }
    }

    @Test
    fun `error result does not delete the receipt photo`() = runTest {
        coEvery { repository.deleteWarranty(any()) } returns Result.Error(IllegalStateException("db down"))

        useCase(warranty(photoPath = "/data/receipt.jpg"))

        verify(exactly = 0) { photoFileCleaner.delete(any()) }
    }

    @Test
    fun `error result does not log analytics`() = runTest {
        coEvery { repository.deleteWarranty(any()) } returns Result.Error(IllegalStateException("db down"))

        useCase(warranty())

        verify(exactly = 0) { analytics.logEvent(any(), any()) }
    }

    @Test
    fun `error result is returned unchanged`() = runTest {
        val expected = Result.Error(IllegalStateException("db down"))
        coEvery { repository.deleteWarranty(any()) } returns expected

        val result = useCase(warranty())

        assertSame(expected, result)
    }

    @Test
    fun `warranty without photo still asks the cleaner with a null path`() = runTest {
        coEvery { repository.deleteWarranty(any()) } returns Result.Success(Unit)

        useCase(warranty(photoPath = null))

        verify(exactly = 1) { photoFileCleaner.delete(null) }
    }

    @Test
    fun `the warranty given by the caller is the one handed to the repository`() = runTest {
        val toDelete = warranty()
        val captured = slot<Warranty>()
        coEvery { repository.deleteWarranty(capture(captured)) } returns Result.Success(Unit)

        useCase(toDelete)

        assertEquals(toDelete, captured.captured)
    }
}
