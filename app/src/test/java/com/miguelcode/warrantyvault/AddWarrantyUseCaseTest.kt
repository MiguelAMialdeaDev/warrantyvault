package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.domain.analytics.AnalyticsEvents
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker
import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.usecase.AddWarrantyUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class AddWarrantyUseCaseTest {

    private val repository: WarrantyRepository = mockk()
    private val notificationScheduler: NotificationScheduler = mockk(relaxUnitFun = true)
    private val preferencesRepository: PreferencesRepository = mockk()
    private val analytics: AnalyticsTracker = mockk(relaxUnitFun = true)
    private lateinit var useCase: AddWarrantyUseCase

    @Before
    fun setup() {
        useCase = AddWarrantyUseCase(
            repository,
            notificationScheduler,
            preferencesRepository,
            analytics
        )
    }

    private fun warranty(
        id: Long = 0,
        photoPath: String? = null,
        price: Double? = null,
        category: WarrantyCategory = WarrantyCategory.ELECTRONICS
    ) = Warranty(
        id = id,
        productName = "MacBook Pro",
        category = category,
        store = "Apple Store",
        price = price,
        purchaseDate = LocalDate.now().minusMonths(1),
        warrantyEndDate = LocalDate.now().plusMonths(23),
        receiptPhotoPath = photoPath
    )

    @Test
    fun `success with notifications enabled schedules alerts`() = runTest {
        coEvery { repository.addWarranty(any()) } returns Result.Success(7L)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)

        useCase(warranty())

        verify(exactly = 1) { notificationScheduler.scheduleExpirationAlerts(any()) }
    }

    @Test
    fun `success with notifications enabled schedules alerts for the id returned by the repository`() = runTest {
        coEvery { repository.addWarranty(any()) } returns Result.Success(42L)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)
        val scheduled = slot<Warranty>()

        useCase(warranty(id = 0))

        verify { notificationScheduler.scheduleExpirationAlerts(capture(scheduled)) }
        assertEquals(42L, scheduled.captured.id)
    }

    @Test
    fun `success with notifications disabled does not schedule alerts`() = runTest {
        coEvery { repository.addWarranty(any()) } returns Result.Success(7L)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(false)

        useCase(warranty())

        verify(exactly = 0) { notificationScheduler.scheduleExpirationAlerts(any()) }
    }

    @Test
    fun `success with notifications disabled still logs the analytics event`() = runTest {
        coEvery { repository.addWarranty(any()) } returns Result.Success(7L)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(false)

        useCase(warranty())

        verify(exactly = 1) { analytics.logEvent(AnalyticsEvents.WARRANTY_ADDED, any()) }
    }

    @Test
    fun `error result does not schedule alerts`() = runTest {
        coEvery { repository.addWarranty(any()) } returns Result.Error(IllegalStateException("db down"))

        useCase(warranty())

        verify(exactly = 0) { notificationScheduler.scheduleExpirationAlerts(any()) }
    }

    @Test
    fun `error result does not log analytics`() = runTest {
        coEvery { repository.addWarranty(any()) } returns Result.Error(IllegalStateException("db down"))

        useCase(warranty())

        verify(exactly = 0) { analytics.logEvent(any(), any()) }
    }

    @Test
    fun `error result is returned unchanged`() = runTest {
        val expected = Result.Error(IllegalStateException("db down"))
        coEvery { repository.addWarranty(any()) } returns expected

        val result = useCase(warranty())

        assertSame(expected, result)
    }

    @Test
    fun `success returns the id produced by the repository`() = runTest {
        coEvery { repository.addWarranty(any()) } returns Result.Success(99L)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)

        val result = useCase(warranty())

        assertTrue(result is Result.Success)
        assertEquals(99L, (result as Result.Success).data)
    }

    @Test
    fun `analytics event carries category has_photo and has_price of the saved warranty`() = runTest {
        coEvery { repository.addWarranty(any()) } returns Result.Success(1L)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)
        val params = slot<Map<String, Any?>>()

        useCase(warranty(photoPath = "/data/photo.jpg", price = 2499.0, category = WarrantyCategory.GAMING))

        verify { analytics.logEvent(AnalyticsEvents.WARRANTY_ADDED, capture(params)) }
        assertEquals("GAMING", params.captured["category"])
        assertEquals(true, params.captured["has_photo"])
        assertEquals(true, params.captured["has_price"])
    }

    @Test
    fun `analytics event reports has_photo and has_price as false when both are missing`() = runTest {
        coEvery { repository.addWarranty(any()) } returns Result.Success(1L)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)
        val params = slot<Map<String, Any?>>()

        useCase(warranty(photoPath = null, price = null, category = WarrantyCategory.OTHER))

        verify { analytics.logEvent(AnalyticsEvents.WARRANTY_ADDED, capture(params)) }
        assertEquals("OTHER", params.captured["category"])
        assertEquals(false, params.captured["has_photo"])
        assertEquals(false, params.captured["has_price"])
    }

    @Test
    fun `the warranty given by the caller is the one handed to the repository`() = runTest {
        val toSave = warranty(price = 10.0)
        val saved = slot<Warranty>()
        coEvery { repository.addWarranty(capture(saved)) } returns Result.Success(1L)
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(true)

        useCase(toSave)

        assertEquals(toSave, saved.captured)
    }
}
