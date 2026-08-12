package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.usecase.GetWarrantyByIdUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GetWarrantyByIdUseCaseTest {

    private val repository: WarrantyRepository = mockk()
    private lateinit var useCase: GetWarrantyByIdUseCase

    @Before
    fun setup() {
        useCase = GetWarrantyByIdUseCase(repository)
    }

    private fun warranty(id: Long) = Warranty(
        id = id,
        productName = "MacBook Pro",
        category = WarrantyCategory.ELECTRONICS,
        store = "Apple Store",
        price = 2499.0,
        purchaseDate = LocalDate.now().minusMonths(2),
        warrantyEndDate = LocalDate.now().plusMonths(22)
    )

    @Test
    fun `returns the warranty provided by the repository`() = runTest {
        val expected = warranty(3)
        coEvery { repository.getWarrantyById(3L) } returns Result.Success(expected)

        val result = useCase(3L)

        assertTrue(result is Result.Success)
        assertEquals(expected, (result as Result.Success).data)
    }

    @Test
    fun `propagates the repository error unchanged`() = runTest {
        val expected = Result.Error(NoSuchElementException("not found"))
        coEvery { repository.getWarrantyById(99L) } returns expected

        val result = useCase(99L)

        assertSame(expected, result)
    }

    @Test
    fun `exposes the exception raised by the repository`() = runTest {
        val boom = IllegalStateException("db down")
        coEvery { repository.getWarrantyById(1L) } returns Result.Error(boom)

        val result = useCase(1L)

        assertTrue(result.isError)
        assertSame(boom, result.exceptionOrNull())
    }

    @Test
    fun `forwards the requested id to the repository`() = runTest {
        coEvery { repository.getWarrantyById(any()) } returns Result.Success(warranty(7))

        useCase(7L)

        coVerify(exactly = 1) { repository.getWarrantyById(7L) }
    }

    @Test
    fun `does not call the repository with a different id`() = runTest {
        coEvery { repository.getWarrantyById(any()) } returns Result.Success(warranty(7))

        useCase(7L)

        coVerify(exactly = 0) { repository.getWarrantyById(neq(7L)) }
    }
}
