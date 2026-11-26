package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.notifications.NotificationEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationEntryTest {

    private val alias = "com.miguelcode.warrantyvault.notifications.OpenWarrantyFromNotification"

    @Test
    fun `accepts the id when the intent comes through the notification alias`() {
        assertEquals(42L, NotificationEntry.warrantyIdFrom(alias, 42L))
    }

    @Test
    fun `ignores the id when the intent comes through the exported launcher`() {
        assertNull(NotificationEntry.warrantyIdFrom("com.miguelcode.warrantyvault.MainActivity", 42L))
    }

    @Test
    fun `ignores a missing or invalid id`() {
        assertNull(NotificationEntry.warrantyIdFrom(alias, -1L))
        assertNull(NotificationEntry.warrantyIdFrom(alias, 0L))
        assertNull(NotificationEntry.warrantyIdFrom(null, 42L))
    }
}
