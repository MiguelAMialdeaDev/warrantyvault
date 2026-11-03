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
        val previous = (repository.getWarrantyById(warranty.id) as? Result.Success)?.data
        val previousPhoto = previous?.receiptPhotoPath

        val result = repository.updateWarranty(warranty)
        if (result is Result.Success) {
            // Los avisos pendientes se rehacen siempre (llevan el nombre del producto),
            // pero los ya mostrados solo se olvidan si cambió la fecha de fin.
            val endDateChanged = previous?.warrantyEndDate != warranty.warrantyEndDate
            notificationScheduler.cancelAlerts(warranty.id, forgetDelivered = endDateChanged)
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
