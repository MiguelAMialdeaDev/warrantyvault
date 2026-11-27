package com.miguelcode.warrantyvault.notifications

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Reglas de los avisos de caducidad, sin Android de por medio. El scheduler y el worker
 * solo traducen esto a WorkManager y a notificaciones, y los tests prueban aquí lo que
 * de verdad decide si un aviso sale o no.
 */
internal object AlertRules {

    /** Un aviso que hay que encolar: cuántos días antes del fin avisa y cuánto esperar. */
    data class PlannedAlert(val daysBefore: Long, val delayMillis: Long)

    /** Lo que el worker tiene que hacer cuando por fin se ejecuta. */
    sealed interface Decision {
        data class Show(val daysLeft: Long) : Decision
        data object Skip : Decision
    }

    /**
     * Avisos pendientes de una garantía. Se saltan los que caen antes de hoy y los ya
     * mostrados. El aviso de hoy cuya hora ya pasó sale con espera 0 en vez de perderse.
     */
    fun plan(
        endDate: LocalDate,
        now: Instant,
        zone: ZoneId,
        isDelivered: (daysBefore: Long) -> Boolean,
        daysBefore: List<Long> = NotificationConstants.ALERT_DAYS_BEFORE,
        alertHour: Int = NotificationConstants.ALERT_HOUR
    ): List<PlannedAlert> {
        val today = now.atZone(zone).toLocalDate()
        return daysBefore.mapNotNull { days ->
            val alertDate = endDate.minusDays(days)
            if (alertDate.isBefore(today) || isDelivered(days)) return@mapNotNull null
            val alertInstant = alertDate.atTime(alertHour, 0).atZone(zone).toInstant()
            PlannedAlert(days, (alertInstant.toEpochMilli() - now.toEpochMilli()).coerceAtLeast(0L))
        }
    }

    /**
     * @param scheduledEndEpochDay fecha de fin con la que se programó el aviso, o null si
     *   lo encoló una versión que aún no la guardaba.
     * @param currentEndDate fecha de fin actual de la garantía, o null si ya no existe.
     * @param legacyDaysLeft días que guardaban los avisos antiguos sin fecha de fin.
     */
    fun decide(
        scheduledEndEpochDay: Long?,
        currentEndDate: LocalDate?,
        today: LocalDate,
        notificationsEnabled: Boolean,
        legacyDaysLeft: Long
    ): Decision {
        // Garantía borrada después de programar el aviso.
        if (currentEndDate == null) return Decision.Skip
        // Un trabajo encolado sobrevive a que el usuario apague el interruptor.
        if (!notificationsEnabled) return Decision.Skip
        // La fecha de fin cambió: este aviso es de la fecha vieja.
        if (scheduledEndEpochDay != null && scheduledEndEpochDay != currentEndDate.toEpochDay()) {
            return Decision.Skip
        }
        // Doze puede retrasar la ejecución días: se recalcula contra el reloj real.
        val daysLeft = scheduledEndEpochDay
            ?.let { ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(it)) }
            ?: legacyDaysLeft
        // Ya vencida cuando por fin se ejecuta: el aviso previo no aplica.
        return if (daysLeft < 0) Decision.Skip else Decision.Show(daysLeft)
    }
}
