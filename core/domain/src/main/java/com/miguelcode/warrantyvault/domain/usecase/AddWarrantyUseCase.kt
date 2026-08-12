package com.miguelcode.warrantyvault.domain.usecase

import com.miguelcode.warrantyvault.domain.analytics.AnalyticsEvents
import com.miguelcode.warrantyvault.domain.analytics.AnalyticsTracker
import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import kotlinx.coroutines.flow.first

class AddWarrantyUseCase(
    private val repository: WarrantyRepository,
    private val notificationScheduler: NotificationScheduler,
    private val preferencesRepository: PreferencesRepository,
    private val analytics: AnalyticsTracker
) {
    suspend operator fun invoke(warranty: Warranty): Result<Long> {
        val result = repository.addWarranty(warranty)
        if (result is Result.Success) {
            // Respeta el interruptor de Ajustes: antes se programaban alertas aunque el
            // usuario las tuviera desactivadas, y reaparecían sin que él hubiera hecho nada.
            if (preferencesRepository.notificationsEnabledFlow.first()) {
                notificationScheduler.scheduleExpirationAlerts(warranty.copy(id = result.data))
            }
            analytics.logEvent(
                AnalyticsEvents.WARRANTY_ADDED,
                mapOf(
                    "category" to warranty.category.name,
                    "has_photo" to (warranty.receiptPhotoPath != null),
                    "has_price" to (warranty.price != null)
                )
            )
        }
        return result
    }
}
