package com.miguelcode.warrantyvault.domain.usecase

import com.miguelcode.warrantyvault.domain.analytics.AnalyticsEvents
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker
import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.storage.PhotoFileCleaner
import kotlinx.coroutines.flow.first

class UpdateWarrantyUseCase(
    private val repository: WarrantyRepository,
    private val notificationScheduler: NotificationScheduler,
    private val photoFileCleaner: PhotoFileCleaner,
    private val preferencesRepository: PreferencesRepository,
    private val analytics: AnalyticsTracker
) {
    suspend operator fun invoke(warranty: Warranty): Result<Unit> {
        // Lee la foto anterior ANTES de actualizar para poder limpiarla si cambió.
        val previousPhoto = (repository.getWarrantyById(warranty.id) as? Result.Success)
            ?.data?.receiptPhotoPath

        val result = repository.updateWarranty(warranty)
        if (result is Result.Success) {
            notificationScheduler.cancelAlerts(warranty.id)
            if (preferencesRepository.notificationsEnabledFlow.first()) {
                notificationScheduler.scheduleExpirationAlerts(warranty)
            }
            if (previousPhoto != null && previousPhoto != warranty.receiptPhotoPath) {
                photoFileCleaner.delete(previousPhoto)
            }
            analytics.logEvent(AnalyticsEvents.WARRANTY_EDITED)
        }
        return result
    }
}
