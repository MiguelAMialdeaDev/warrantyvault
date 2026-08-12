package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.usecase.SyncExpirationAlertsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class SyncExpirationAlertsUseCaseTest {

    private val warrantyRepository: WarrantyRepository = mockk()
    private val preferencesRepository: PreferencesRepository = mockk()
    private val notificationScheduler: NotificationScheduler = mockk(relaxUnitFun = true)
    private lateinit var useCase: SyncExpirationAlertsUseCase

    @Before
    fun setup() {
        useCase = SyncExpirationAlertsUseCase(
            warrantyRepository,
            preferencesRepository,
            notificationScheduler
        )
    }

    private fun warranty(
        id: Long,
        endDate: LocalDate
    ) = Warranty(
        id = id,
        productName = "Product $id",
        category = WarrantyCategory.APPLIANCE,
        store = "Store",
        purchaseDate = LocalDate.now().minusYears(2),
        warrantyEndDate = endDate
    )

    private fun givenNotificationsEnabled(enabled: Boolean) {
        every { preferencesRepository.notificationsEnabledFlow } returns flowOf(enabled)
    }

    private fun givenWarranties(vararg warranties: Warranty) {
        every { warrantyRepository.getAllWarranties() } returns flowOf(warranties.toList())
    }

    @Test
    fun `cancels all alerts before scheduling anything`() = runTest {
        val active = warranty(1, LocalDate.now().plusMonths(6))
        givenNotificationsEnabled(true)
        givenWarranties(active)

        useCase()

        verifyOrder {
            notificationScheduler.cancelAll()
            notificationScheduler.scheduleExpirationAlerts(active)
        }
    }

    @Test
    fun `cancels all alerts even when notifications are disabled`() = runTest {
        givenNotificationsEnabled(false)
        givenWarranties(warranty(1, LocalDate.now().plusMonths(6)))

        useCase()

        verify(exactly = 1) { notificationScheduler.cancelAll() }
    }

    @Test
    fun `cancels all alerts even when there are no warranties`() = runTest {
        givenNotificationsEnabled(true)
        givenWarranties()

        useCase()

        verify(exactly = 1) { notificationScheduler.cancelAll() }
    }

    @Test
    fun `does not schedule anything when notifications are disabled`() = runTest {
        givenNotificationsEnabled(false)
        givenWarranties(warranty(1, LocalDate.now().plusMonths(6)))

        useCase()

        verify(exactly = 0) { notificationScheduler.scheduleExpirationAlerts(any()) }
    }

    @Test
    fun `does not even read the warranties when notifications are disabled`() = runTest {
        givenNotificationsEnabled(false)
        givenWarranties(warranty(1, LocalDate.now().plusMonths(6)))

        useCase()

        verify(exactly = 0) { warrantyRepository.getAllWarranties() }
    }

    @Test
    fun `schedules every warranty that has not expired yet`() = runTest {
        val first = warranty(1, LocalDate.now().plusDays(1))
        val second = warranty(2, LocalDate.now().plusYears(1))
        givenNotificationsEnabled(true)
        givenWarranties(first, second)

        useCase()

        verify(exactly = 1) { notificationScheduler.scheduleExpirationAlerts(first) }
        verify(exactly = 1) { notificationScheduler.scheduleExpirationAlerts(second) }
    }

    @Test
    fun `does not schedule warranties whose end date is already in the past`() = runTest {
        val expiredYesterday = warranty(1, LocalDate.now().minusDays(1))
        val expiredLongAgo = warranty(2, LocalDate.now().minusYears(3))
        givenNotificationsEnabled(true)
        givenWarranties(expiredYesterday, expiredLongAgo)

        useCase()

        verify(exactly = 0) { notificationScheduler.scheduleExpirationAlerts(any()) }
    }

    @Test
    fun `schedules a warranty that expires today`() = runTest {
        val expiresToday = warranty(1, LocalDate.now())
        givenNotificationsEnabled(true)
        givenWarranties(expiresToday)

        useCase()

        verify(exactly = 1) { notificationScheduler.scheduleExpirationAlerts(expiresToday) }
    }

    @Test
    fun `schedules only the non expired warranties of a mixed list`() = runTest {
        val expired = warranty(1, LocalDate.now().minusDays(10))
        val active = warranty(2, LocalDate.now().plusMonths(3))
        givenNotificationsEnabled(true)
        givenWarranties(expired, active)

        useCase()

        verify(exactly = 1) { notificationScheduler.scheduleExpirationAlerts(active) }
        verify(exactly = 0) { notificationScheduler.scheduleExpirationAlerts(expired) }
    }

    @Test
    fun `two consecutive runs cancel all twice and reschedule the same warranty twice`() = runTest {
        val active = warranty(1, LocalDate.now().plusMonths(6))
        givenNotificationsEnabled(true)
        givenWarranties(active)

        useCase()
        useCase()

        verify(exactly = 2) { notificationScheduler.cancelAll() }
        verify(exactly = 2) { notificationScheduler.scheduleExpirationAlerts(active) }
    }

    @Test
    fun `each run starts by wiping the queue so no duplicates can pile up`() = runTest {
        val active = warranty(1, LocalDate.now().plusMonths(6))
        givenNotificationsEnabled(true)
        givenWarranties(active)

        useCase()
        useCase()

        verifyOrder {
            notificationScheduler.cancelAll()
            notificationScheduler.scheduleExpirationAlerts(active)
            notificationScheduler.cancelAll()
            notificationScheduler.scheduleExpirationAlerts(active)
        }
    }

    @Test
    fun `schedules nothing when the database is empty`() = runTest {
        givenNotificationsEnabled(true)
        givenWarranties()

        useCase()

        verify(exactly = 0) { notificationScheduler.scheduleExpirationAlerts(any()) }
    }

    @Test
    fun `only the first emission of the warranties flow is used`() = runTest {
        val first = warranty(1, LocalDate.now().plusMonths(6))
        val later = warranty(2, LocalDate.now().plusMonths(6))
        givenNotificationsEnabled(true)
        every { warrantyRepository.getAllWarranties() } returns
            flowOf(listOf(first), listOf(later))

        useCase()

        verify(exactly = 1) { notificationScheduler.scheduleExpirationAlerts(first) }
        verify(exactly = 0) { notificationScheduler.scheduleExpirationAlerts(later) }
    }
}
