package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.backup.BackupCodec
import com.miguelcode.warrantyvault.domain.backup.InvalidBackupException
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.model.WarrantyTracking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BackupCodecTest {

    private val warranty = Warranty(
        id = 9,
        productName = "Lavadora",
        category = WarrantyCategory.APPLIANCE,
        store = "MediaMarkt",
        price = 499.99,
        purchaseDate = LocalDate.of(2025, 3, 1),
        warrantyEndDate = LocalDate.of(2028, 3, 1),
        notes = "Serie 123",
        createdAt = LocalDateTime.of(2025, 3, 1, 10, 0),
        tracking = WarrantyTracking.IN_CLAIM,
        trackingDate = LocalDate.of(2026, 1, 5)
    )

    @Test
    fun `a warranty survives a round trip`() {
        val json = BackupCodec.encode(listOf(BackupCodec.Entry(warranty, "photos/9.jpg")), LocalDateTime.now())

        val (decoded, photo) = BackupCodec.decode(json).single()

        // El id no viaja: al importar se genera uno nuevo.
        assertEquals(warranty.copy(id = 0), decoded)
        assertEquals("photos/9.jpg", photo)
    }

    @Test(expected = InvalidBackupException::class)
    fun `a json from somewhere else is rejected`() {
        BackupCodec.decode("""{"warranties": []}""")
    }

    @Test(expected = InvalidBackupException::class)
    fun `a backup from a newer version is rejected`() {
        BackupCodec.decode("""{"format": "warrantyvault-backup", "version": 99, "warranties": []}""")
    }

    @Test
    fun `broken entries are skipped and the rest imported`() {
        val json = """
            {"format": "warrantyvault-backup", "version": 1, "warranties": [
              {"productName": "", "purchaseDate": "2025-01-01", "warrantyEndDate": "2026-01-01"},
              {"productName": "Bad date", "purchaseDate": "ayer", "warrantyEndDate": "2026-01-01"},
              {"productName": "Ends before", "purchaseDate": "2025-01-01", "warrantyEndDate": "2024-01-01"},
              {"productName": "OK", "purchaseDate": "2025-01-01", "warrantyEndDate": "2026-01-01",
               "category": "SPACESHIP", "price": -3, "tracking": "LOST"}
            ]}
        """.trimIndent()

        val decoded = BackupCodec.decode(json).single().warranty

        assertEquals("OK", decoded.productName)
        assertEquals(WarrantyCategory.OTHER, decoded.category)
        assertNull(decoded.price)
        assertEquals(WarrantyTracking.NONE, decoded.tracking)
    }

    @Test
    fun `photo paths that try to leave the photos folder are dropped`() {
        val json = BackupCodec.encode(listOf(BackupCodec.Entry(warranty, "photos/../../databases/x")), LocalDateTime.now())

        assertNull(BackupCodec.decode(json).single().photoEntry)
        assertTrue(BackupCodec.isSafeName("9.jpg"))
    }
}
