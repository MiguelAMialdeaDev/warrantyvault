package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.notifications.WarrantyExpirationWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NotificationIdTest {

    @Test
    fun `warranty 1 at 14 days and warranty 11 at 4 days get different ids`() {
        assertNotEquals(
            WarrantyExpirationWorker.notificationIdFor(1, 14),
            WarrantyExpirationWorker.notificationIdFor(11, 4)
        )
    }

    @Test
    fun `ids never repeat across warranties and alert days`() {
        val ids = (1L..500L).flatMap { id ->
            (0L..30L).map { days -> WarrantyExpirationWorker.notificationIdFor(id, days) }
        }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `the same alert keeps the same id so it updates instead of duplicating`() {
        assertEquals(
            WarrantyExpirationWorker.notificationIdFor(42, 7),
            WarrantyExpirationWorker.notificationIdFor(42, 7)
        )
    }
}
