package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.domain.analytics.AnalyticsEvents
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.feature.onboarding.OnboardingViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferencesRepository: PreferencesRepository = mockk()
    private val analytics: AnalyticsTracker = mockk(relaxed = true)

    private fun createViewModel() = OnboardingViewModel(preferencesRepository, analytics)

    @Test
    fun `completeOnboarding marks the onboarding as done in preferences`() = runTest {
        coEvery { preferencesRepository.setOnboardingDone() } returns Unit

        createViewModel().completeOnboarding {}

        coVerify(exactly = 1) { preferencesRepository.setOnboardingDone() }
    }

    @Test
    fun `completeOnboarding logs the onboarding completed event`() = runTest {
        coEvery { preferencesRepository.setOnboardingDone() } returns Unit

        createViewModel().completeOnboarding {}

        verify(exactly = 1) { analytics.logEvent(AnalyticsEvents.ONBOARDING_COMPLETED, any()) }
    }

    @Test
    fun `completeOnboarding invokes the onDone callback`() = runTest {
        coEvery { preferencesRepository.setOnboardingDone() } returns Unit
        var done = false

        createViewModel().completeOnboarding { done = true }

        assertTrue(done)
    }

    @Test
    fun `completeOnboarding persists the flag before invoking onDone`() = runTest {
        var savedWhenCallbackRan = false
        coEvery { preferencesRepository.setOnboardingDone() } answers {
            savedWhenCallbackRan = true
        }

        createViewModel().completeOnboarding {
            assertTrue(
                "onDone se llamó antes de guardar la preferencia",
                savedWhenCallbackRan
            )
        }

        coVerify(exactly = 1) { preferencesRepository.setOnboardingDone() }
    }

    @Test
    fun `completeOnboarding called twice persists the flag once per call`() = runTest {
        coEvery { preferencesRepository.setOnboardingDone() } returns Unit
        var callbacks = 0

        val vm = createViewModel()
        vm.completeOnboarding { callbacks++ }
        vm.completeOnboarding { callbacks++ }

        assertEquals(2, callbacks)
        coVerify(exactly = 2) { preferencesRepository.setOnboardingDone() }
    }
}
