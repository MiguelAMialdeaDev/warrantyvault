package com.miguelcode.warrantyvault

import app.cash.turbine.test
import com.miguelcode.warrantyvault.domain.billing.BillingManagerContract
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.usecase.SyncExpirationAlertsUseCase
import com.miguelcode.warrantyvault.feature.settings.SettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.miguelcode.warrantyvault.domain.backup.ImportSummary
import com.miguelcode.warrantyvault.domain.backup.InvalidBackupException
import com.miguelcode.warrantyvault.domain.backup.WarrantyBackup
import com.miguelcode.warrantyvault.domain.model.Result

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesRepository: PreferencesRepository = mockk()
    private val syncExpirationAlerts: SyncExpirationAlertsUseCase = mockk()
    private val billingManager: BillingManagerContract = mockk()
    private val backup: WarrantyBackup = mockk()

    private fun stubFlows(
        isPremium: Boolean = false,
        themeMode: String = "system",
        notificationsEnabled: Boolean = false
    ) {
        every { preferencesRepository.isPremiumFlow } returns flowOf(isPremium)
        every { preferencesRepository.themeModeFlow } returns flowOf(themeMode)
        every { preferencesRepository.notificationsEnabledFlow } returns
            flowOf(notificationsEnabled)
        every { preferencesRepository.crashReportsEnabledFlow } returns flowOf(true)
    }

    private fun createViewModel() =
        SettingsViewModel(preferencesRepository, syncExpirationAlerts, billingManager, backup)

    @Test
    fun `isPremium exposes the value from the repository`() = runTest {
        stubFlows(isPremium = true)

        val vm = createViewModel()

        vm.isPremium.test {
            assertTrue(expectMostRecentItem())
        }
    }

    @Test
    fun `isPremium is false when repository reports a free user`() = runTest {
        stubFlows(isPremium = false)

        val vm = createViewModel()

        vm.isPremium.test {
            assertFalse(expectMostRecentItem())
        }
    }

    @Test
    fun `themeMode exposes the value from the repository`() = runTest {
        stubFlows(themeMode = "dark")

        val vm = createViewModel()

        vm.themeMode.test {
            assertEquals("dark", expectMostRecentItem())
        }
    }

    @Test
    fun `notificationsEnabled exposes the value from the repository`() = runTest {
        stubFlows(notificationsEnabled = true)

        val vm = createViewModel()

        vm.notificationsEnabled.test {
            assertTrue(expectMostRecentItem())
        }
    }

    @Test
    fun `setThemeMode saves the mode in the repository`() = runTest {
        stubFlows()
        coEvery { preferencesRepository.setThemeMode(any()) } returns Unit

        createViewModel().setThemeMode("light")

        coVerify(exactly = 1) { preferencesRepository.setThemeMode("light") }
    }

    @Test
    fun `setNotificationsEnabled true saves the preference and syncs alerts`() = runTest {
        stubFlows()
        coEvery { preferencesRepository.setNotificationsEnabled(any()) } returns Unit
        coEvery { syncExpirationAlerts() } returns Unit

        createViewModel().setNotificationsEnabled(true)

        coVerify(exactly = 1) { preferencesRepository.setNotificationsEnabled(true) }
        coVerify(exactly = 1) { syncExpirationAlerts() }
    }

    @Test
    fun `setNotificationsEnabled false saves the preference and syncs alerts`() = runTest {
        stubFlows()
        coEvery { preferencesRepository.setNotificationsEnabled(any()) } returns Unit
        coEvery { syncExpirationAlerts() } returns Unit

        createViewModel().setNotificationsEnabled(false)

        coVerify(exactly = 1) { preferencesRepository.setNotificationsEnabled(false) }
        coVerify(exactly = 1) { syncExpirationAlerts() }
    }

    @Test
    fun `setNotificationsEnabled saves the preference before syncing alerts`() = runTest {
        stubFlows()
        coEvery { preferencesRepository.setNotificationsEnabled(any()) } returns Unit
        coEvery { syncExpirationAlerts() } returns Unit

        createViewModel().setNotificationsEnabled(false)

        coVerifyOrder {
            preferencesRepository.setNotificationsEnabled(false)
            syncExpirationAlerts()
        }
    }

    @Test
    fun `setThemeMode does not touch the expiration alerts`() = runTest {
        stubFlows()
        coEvery { preferencesRepository.setThemeMode(any()) } returns Unit

        createViewModel().setThemeMode("system")

        coVerify(exactly = 0) { syncExpirationAlerts() }
    }

    @Test
    fun `restorePurchases reports Restored once Play brings premium back`() = runTest {
        val premium = MutableStateFlow(false)
        stubFlows()
        every { preferencesRepository.isPremiumFlow } returns premium
        justRun { billingManager.refreshPurchases() }
        val vm = createViewModel()

        vm.restoreResults.test {
            vm.restorePurchases()
            assertTrue(vm.restoring.value)
            premium.value = true

            assertEquals(SettingsViewModel.RestoreResult.Restored, awaitItem())
            assertFalse(vm.restoring.value)
        }
        verify(exactly = 1) { billingManager.refreshPurchases() }
    }

    @Test
    fun `restorePurchases reports NothingFound when no subscription shows up in time`() = runTest {
        stubFlows(isPremium = false)
        justRun { billingManager.refreshPurchases() }
        val vm = createViewModel()

        vm.restoreResults.test {
            vm.restorePurchases()
            advanceTimeBy(SETTINGS_RESTORE_TIMEOUT_MS + 1)
            advanceUntilIdle()

            assertEquals(SettingsViewModel.RestoreResult.NothingFound, awaitItem())
            assertFalse(vm.restoring.value)
        }
    }

    @Test
    fun `restorePurchases ignores a second tap while the first is still running`() = runTest {
        stubFlows()
        every { preferencesRepository.isPremiumFlow } returns MutableStateFlow(false)
        justRun { billingManager.refreshPurchases() }
        val vm = createViewModel()

        vm.restorePurchases()
        vm.restorePurchases()

        verify(exactly = 1) { billingManager.refreshPurchases() }
    }

    private companion object {
        // Mismo valor que SettingsViewModel.RESTORE_TIMEOUT_MS, que es internal a su módulo.
        const val SETTINGS_RESTORE_TIMEOUT_MS = 10_000L
    }

    @Test
    fun `import reports what was imported and that the file was not a backup`() = runTest {
        stubFlows()
        val summary = ImportSummary(imported = 4, skippedDuplicates = 1, archivedForLimit = 2)
        coEvery { backup.importZip("ok") } returns Result.Success(summary)
        coEvery { backup.importZip("bad") } returns Result.Error(InvalidBackupException("x"))
        val vm = createViewModel()

        vm.backupResults.test {
            vm.importBackup("ok")
            assertEquals(SettingsViewModel.BackupResult.Imported(summary), awaitItem())
            vm.importBackup("bad")
            assertEquals(SettingsViewModel.BackupResult.NotABackup, awaitItem())
        }
        assertEquals(false, vm.backupBusy.value)
    }
}
