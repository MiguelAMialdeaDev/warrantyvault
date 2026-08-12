package com.miguelcode.warrantyvault.domain.usecase

import com.miguelcode.warrantyvault.domain.analytics.AnalyticsEvents
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker
import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.storage.PhotoFileCleaner

class DeleteWarrantyUseCase(
    private val repository: WarrantyRepository,
    private val notificationScheduler: NotificationScheduler,
    private val photoFileCleaner: PhotoFileCleaner,
    private val analytics: AnalyticsTracker
) {
    suspend operator fun invoke(warranty: Warranty): Result<Unit> {
        val result = repository.deleteWarranty(warranty)
        if (result is Result.Success) {
            notificationScheduler.cancelAlerts(warranty.id)
            photoFileCleaner.delete(warranty.receiptPhotoPath)
            analytics.logEvent(AnalyticsEvents.WARRANTY_DELETED)
        }
        return result
    }
}
