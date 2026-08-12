package com.miguelcode.warrantyvault

import app.cash.turbine.test
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.usecase.GetWarrantiesUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GetWarrantiesUseCaseTest {

    private val repository: WarrantyRepository = mockk()
    private lateinit var useCase: GetWarrantiesUseCase

    @Before
    fun setup() {
        useCase = GetWarrantiesUseCase(repository)
    }

    private fun warranty(id: Long, name: String = "Product $id") = Warranty(
        id = id,
        productName = name,
        category = WarrantyCategory.ELECTRONICS,
        store = "Store",
        purchaseDate = LocalDate.now().minusMonths(2),
        warrantyEndDate = LocalDate.now().plusMonths(10)
    )

    @Test
    fun `emits the list provided by the repository`() = runTest {
        val warranties = listOf(warranty(1, "MacBook"), warranty(2, "iPhone"))
        every { repository.getAllWarranties() } returns flowOf(warranties)

        useCase().test {
            assertEquals(warranties, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits an empty list when the repository has no warranties`() = runTest {
        every { repository.getAllWarranties() } returns flowOf(emptyList())

        useCase().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `re-emits every update of the repository flow in order`() = runTest {
        val first = listOf(warranty(1))
        val second = listOf(warranty(1), warranty(2))
        every { repository.getAllWarranties() } returns flowOf(first, second)

        useCase().test {
            assertEquals(first, awaitItem())
            assertEquals(second, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `propagates errors thrown by the repository flow`() = runTest {
        val boom = IllegalStateException("db down")
        every { repository.getAllWarranties() } returns flow { throw boom }

        useCase().test {
            assertEquals(boom, awaitError())
        }
    }

    @Test
    fun `propagates an error raised after a successful emission`() = runTest {
        val warranties = listOf(warranty(1))
        val boom = IllegalStateException("db down")
        every { repository.getAllWarranties() } returns flow {
            emit(warranties)
            throw boom
        }

        useCase().test {
            assertEquals(warranties, awaitItem())
            assertEquals(boom, awaitError())
        }
    }

    @Test
    fun `asks the repository for the warranties exactly once per invocation`() = runTest {
        every { repository.getAllWarranties() } returns flowOf(emptyList())

        useCase()

        verify(exactly = 1) { repository.getAllWarranties() }
    }
}
