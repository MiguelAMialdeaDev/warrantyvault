package com.miguelcode.warrantyvault.domain.usecase

import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyTracking
import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Cambia el seguimiento de una garantía (en reclamación, reparada, archivada o ninguno).
 *
 * Archivar quita sus avisos y libera su hueco del plan gratuito. Desarchivar la vuelve a
 * contar, así que con el límite ya lleno pide Premium igual que un alta.
 */
class SetWarrantyTrackingUseCase(
    private val repository: WarrantyRepository,
    private val notificationScheduler: NotificationScheduler,
    private val preferencesRepository: PreferencesRepository,
    private val checkPremiumLimitUseCase: CheckPremiumLimitUseCase,
    private val today: () -> LocalDate = LocalDate::now
) {
    suspend operator fun invoke(warranty: Warranty, tracking: WarrantyTracking): Result<Unit> {
        if (warranty.tracking == tracking) return Result.Success(Unit)
        val unarchiving = warranty.isArchived && tracking != WarrantyTracking.ARCHIVED
        if (unarchiving && !checkPremiumLimitUseCase()) {
            return Result.Error(PremiumLimitReachedException())
        }

        val updated = warranty.copy(
            tracking = tracking,
            trackingDate = if (tracking == WarrantyTracking.NONE) null else today()
        )
        val result = repository.updateWarranty(updated)
        if (result is Result.Success) {
            when {
                // Los avisos ya mostrados se conservan: si se desarchiva no deben repetirse.
                updated.isArchived -> notificationScheduler.cancelAlerts(warranty.id, forgetDelivered = false)
                unarchiving && preferencesRepository.notificationsEnabledFlow.first() ->
                    notificationScheduler.scheduleExpirationAlerts(updated)
            }
        }
        return result
    }
}
