package com.miguelcode.warrantyvault.domain.notification

import com.miguelcode.warrantyvault.domain.model.Warranty

interface NotificationScheduler {
    fun scheduleExpirationAlerts(warranty: Warranty)
    fun cancelAlerts(warrantyId: Long)
    fun cancelAll()
}
