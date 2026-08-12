package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.domain.model.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun `Success isSuccess returns true`() {
        val result = Result.Success("data")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `Error isError returns true`() {
        val result = Result.Error(RuntimeException("fail"))
        assertTrue(result.isError)
    }

    @Test
    fun `getOrNull returns data on Success`() {
        val result: Result<String> = Result.Success("hello")
        assertEquals("hello", result.getOrNull())
    }

    @Test
    fun `getOrNull returns null on Error`() {
        val result: Result<String> = Result.Error(RuntimeException())
        assertNull(result.getOrNull())
    }

    @Test
    fun `exceptionOrNull returns exception on Error`() {
        val ex = RuntimeException("fail")
        val result: Result<String> = Result.Error(ex)
        assertEquals(ex, result.exceptionOrNull())
    }

    @Test
    fun `exceptionOrNull returns null on Success`() {
        val result: Result<String> = Result.Success("ok")
        assertNull(result.exceptionOrNull())
    }

    @Test
    fun `map transforms data on Success`() {
        val result = Result.Success(5).map { it * 2 }
        assertEquals(10, (result as Result.Success).data)
    }

    @Test
    fun `map passes through Error`() {
        val ex = RuntimeException("fail")
        val result: Result<Int> = Result.Error(ex)
        val mapped = result.map { it * 2 }
        assertTrue(mapped.isError)
        assertEquals(ex, mapped.exceptionOrNull())
    }

    @Test
    fun `runCatching returns Success on no exception`() {
        val result = Result.runCatching { 42 }
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `runCatching returns Error on exception`() {
        val result = Result.runCatching { throw IllegalStateException("boom") }
        assertTrue(result.isError)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }
}
