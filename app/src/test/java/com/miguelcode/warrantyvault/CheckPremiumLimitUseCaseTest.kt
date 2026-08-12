package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.usecase.CheckPremiumLimitUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckPremiumLimitUseCaseTest {

    private val warrantyRepository: WarrantyRepository = mockk()
    private val preferencesRepository: PreferencesRepository = mockk()
    private lateinit var useCase: CheckPremiumLimitUseCase

    @Before
    fun setup() {
        useCase = CheckPremiumLimitUseCase(warrantyRepository, preferencesRepository)
    }

    @Test
    fun `premium user can always add`() = runTest {
        coEvery { preferencesRepository.isPremium() } returns true
        coEvery { warrantyRepository.getCount() } returns 100

        assertTrue(useCase())
    }

    @Test
    fun `free user with 0 warranties can add`() = runTest {
        coEvery { preferencesRepository.isPremium() } returns false
        coEvery { warrantyRepository.getCount() } returns 0

        assertTrue(useCase())
    }

    @Test
    fun `free user with 2 warranties can add`() = runTest {
        coEvery { preferencesRepository.isPremium() } returns false
        coEvery { warrantyRepository.getCount() } returns 2

        assertTrue(useCase())
    }

    @Test
    fun `free user with 3 warranties cannot add`() = runTest {
        coEvery { preferencesRepository.isPremium() } returns false
        coEvery { warrantyRepository.getCount() } returns 3

        assertFalse(useCase())
    }

    @Test
    fun `free user with more than 3 warranties cannot add`() = runTest {
        coEvery { preferencesRepository.isPremium() } returns false
        coEvery { warrantyRepository.getCount() } returns 5

        assertFalse(useCase())
    }
}
