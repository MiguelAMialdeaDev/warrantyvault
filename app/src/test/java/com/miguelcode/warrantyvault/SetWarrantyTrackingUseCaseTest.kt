package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.model.WarrantyTracking
import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.usecase.CheckPremiumLimitUseCase
import com.miguelcode.warrantyvault.domain.usecase.PremiumLimitReachedException
import com.miguelcode.warrantyvault.domain.usecase.SetWarrantyTrackingUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SetWarrantyTrackingUseCaseTest {

    private val today = LocalDate.of(2026, 11, 27)
    private val repository: WarrantyRepository = mockk()
    private val scheduler: NotificationScheduler = mockk(relaxUnitFun = true)
    private val preferences: PreferencesRepository = mockk {
        every { notificationsEnabledFlow } returns flowOf(true)
    }
    private val checkLimit: CheckPremiumLimitUseCase = mockk()
    private val saved = slot<Warranty>()

    private val useCase = SetWarrantyTrackingUseCase(repository, scheduler, preferences, checkLimit) { today }

    private fun warranty(tracking: WarrantyTracking = WarrantyTracking.NONE) = Warranty(
        id = 7,
        productName = "TV",
        category = WarrantyCategory.ELECTRONICS,
        purchaseDate = today.minusYears(1),
        warrantyEndDate = today.plusYears(2),
        tracking = tracking,
        trackingDate = if (tracking == WarrantyTracking.NONE) null else today.minusDays(10)
    )

    init {
        coEvery { repository.updateWarranty(capture(saved)) } returns Result.Success(Unit)
    }

    @Test
    fun `archiving stores the date and cancels the alerts`() = runTest {
        useCase(warranty(), WarrantyTracking.ARCHIVED)

        assertEquals(WarrantyTracking.ARCHIVED, saved.captured.tracking)
        assertEquals(today, saved.captured.trackingDate)
        verify { scheduler.cancelAlerts(7, forgetDelivered = false) }
    }

    @Test
    fun `unarchiving with a free slot brings the alerts back`() = runTest {
        coEvery { checkLimit() } returns true

        useCase(warranty(WarrantyTracking.ARCHIVED), WarrantyTracking.NONE)

        assertNull(saved.captured.trackingDate)
        verify { scheduler.scheduleExpirationAlerts(saved.captured) }
    }

    @Test
    fun `unarchiving with the free limit full asks for premium`() = runTest {
        coEvery { checkLimit() } returns false

        val result = useCase(warranty(WarrantyTracking.ARCHIVED), WarrantyTracking.NONE)

        assertTrue((result as Result.Error).exception is PremiumLimitReachedException)
        coVerify(exactly = 0) { repository.updateWarranty(any()) }
    }

    @Test
    fun `marking a claim does not touch the alerts or the limit`() = runTest {
        useCase(warranty(), WarrantyTracking.IN_CLAIM)

        assertEquals(WarrantyTracking.IN_CLAIM, saved.captured.tracking)
        coVerify(exactly = 0) { checkLimit() }
        verify(exactly = 0) { scheduler.cancelAlerts(any(), any()) }
    }
}
