package com.miguelcode.warrantyvault.domain.usecase

import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Deja las alertas encoladas en WorkManager acordes con lo que hay en la base de datos
 * y con la preferencia de notificaciones.
 *
 * Hace falta porque el trabajo de WorkManager vive en el dispositivo, no en la base de
 * datos: no viaja en la copia de seguridad. Al cambiar de móvil o al reinstalar, el
 * usuario recupera sus garantías pero se queda sin una sola alerta programada, y nada
 * las regeneraba. En una app cuyo único trabajo es avisar antes de que venza algo, eso
 * es quedarse sin producto en silencio.
 *
 * Es idempotente: primero borra todas las alertas y luego reprograma desde cero, así que
 * puede llamarse en cada arranque sin acumular duplicados.
 */
class SyncExpirationAlertsUseCase(
    private val warrantyRepository: WarrantyRepository,
    private val preferencesRepository: PreferencesRepository,
    private val notificationScheduler: NotificationScheduler
) {
    suspend operator fun invoke() {
        notificationScheduler.cancelAll()

        if (!preferencesRepository.notificationsEnabledFlow.first()) return

        val today = LocalDate.now()
        warrantyRepository.getAllWarranties().first()
            // Una garantía ya vencida no tiene ningún aviso futuro que dar.
            .filter { !it.warrantyEndDate.isBefore(today) }
            .forEach { notificationScheduler.scheduleExpirationAlerts(it) }
    }
}
