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

    @Test
    fun `a big photo is decoded at the largest power of two that keeps 2048 px`() {
        // 8000 px: 1/2 = 4000, 1/4 = 2000 (< 2048) -> se queda en 2 y luego se escala.
        assertEquals(2, PhotoStorage.sampleSizeFor(8000, 6000, 2048))
        assertEquals(4, PhotoStorage.sampleSizeFor(9000, 4000, 2048))
    }

    @Test
    fun `a small photo is not subsampled`() {
        assertEquals(1, PhotoStorage.sampleSizeFor(1200, 1600, 2048))
    }
}
