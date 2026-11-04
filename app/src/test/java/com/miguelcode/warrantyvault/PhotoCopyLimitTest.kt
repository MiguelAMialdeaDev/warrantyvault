package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.feature.addedit.PhotoStorage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class PhotoCopyLimitTest {

    @Test
    fun `a photo within the limit is copied whole`() {
        val bytes = ByteArray(10_000) { it.toByte() }
        val out = ByteArrayOutputStream()

        PhotoStorage.copyWithLimit(ByteArrayInputStream(bytes), out, maxBytes = 10_000)

        assertEquals(bytes.toList(), out.toByteArray().toList())
    }

    @Test(expected = IOException::class)
    fun `a photo over the limit is rejected`() {
        PhotoStorage.copyWithLimit(
            ByteArrayInputStream(ByteArray(10_001)),
            ByteArrayOutputStream(),
            maxBytes = 10_000
        )
    }
}
