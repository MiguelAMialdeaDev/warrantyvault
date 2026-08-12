package com.miguelcode.warrantyvault

import app.cash.turbine.test
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.usecase.SyncExpirationAlertsUseCase
import com.miguelcode.warrantyvault.feature.settings.SettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesRepository: PreferencesRepository = mockk()
    private val syncExpirationAlerts: SyncExpirationAlertsUseCase = mockk()

    private fun stubFlows(
        isPremium: Boolean = false,
        themeMode: String = "system",
        notificationsEnabled: Boolean = false
    ) {
        every { preferencesRepository.isPremiumFlow } returns flowOf(isPremium)
        every { preferencesRepository.themeModeFlow } returns flowOf(themeMode)
        every { preferencesRepository.notificationsEnabledFlow } returns
            flowOf(notificationsEnabled)
    }

    private fun createViewModel() =
        SettingsViewModel(preferencesRepository, syncExpirationAlerts)

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
}
