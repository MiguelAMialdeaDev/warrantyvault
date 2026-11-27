package com.miguelcode.warrantyvault.domain.usecase

import com.miguelcode.warrantyvault.domain.notification.NotificationScheduler
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * Es idempotente porque el scheduler encola trabajo único por (garantía, aviso): volver a
 * programar no acumula duplicados. Antes la idempotencia venía de borrarlo todo y
 * reprogramar, y eso perdía los avisos del día en curso: el worker encolado para hoy que
 * aún no había corrido (Doze, móvil apagado de noche) se cancelaba, y al reprogramarlo su
 * instante ya había pasado. Abrir la app por la mañana mataba el aviso de ese día.
 */
class SyncExpirationAlertsUseCase(
    private val warrantyRepository: WarrantyRepository,
    private val preferencesRepository: PreferencesRepository,
    private val notificationScheduler: NotificationScheduler
) {
    suspend operator fun invoke() = mutex.withLock {
        if (!preferencesRepository.notificationsEnabledFlow.first()) {
            notificationScheduler.cancelAll()
            return@withLock
        }

        val today = LocalDate.now()
        warrantyRepository.getAllWarranties().first()
            // Una garantía ya vencida no tiene ningún aviso futuro que dar.
            .filter { !it.warrantyEndDate.isBefore(today) && !it.isArchived }
            .forEach { notificationScheduler.scheduleExpirationAlerts(it) }
    }

    private companion object {
        /**
         * Compartido entre instancias a propósito: protege la cola de WorkManager, que es
         * global al proceso, y el use case se inyecta como factory (el sync de arranque y
         * el del interruptor de Ajustes son objetos distintos).
         *
         * Entre leer la preferencia y programar hay dos puntos de suspensión, así que dos
         * ejecuciones simultáneas se intercalaban y el resultado dependía del orden de
         * reanudación: apagar el interruptor podía terminar con todas las alertas
         * programadas.
         */
        val mutex = Mutex()
    }
}
