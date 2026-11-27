package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.notifications.AlertRules
import com.miguelcode.warrantyvault.notifications.AlertRules.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class AlertRulesTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 11, 27)

    private fun at(hour: Int, minute: Int = 0) =
        ZonedDateTime.of(today.atTime(hour, minute), zone).toInstant()

    @Test
    fun `plans the four alerts at 9 for a warranty far from its end`() {
        val plan = AlertRules.plan(today.plusDays(60), at(12), zone, isDelivered = { false })

        assertEquals(listOf(30L, 14L, 7L, 1L), plan.map { it.daysBefore })
        // El de 30 días cae dentro de 30 días a las 9:00, contando desde hoy a las 12:00.
        val expected = Duration.between(at(12), ZonedDateTime.of(today.plusDays(30).atTime(9, 0), zone))
        assertEquals(expected.toMillis(), plan.first().delayMillis)
    }

    @Test
    fun `skips alerts whose day has already passed`() {
        val plan = AlertRules.plan(today.plusDays(10), at(12), zone, isDelivered = { false })

        assertEquals(listOf(7L, 1L), plan.map { it.daysBefore })
    }

    @Test
    fun `today's alert after 9 fires right away instead of being lost`() {
        val plan = AlertRules.plan(today.plusDays(30), at(13, 26), zone, isDelivered = { false })

        assertEquals(30L, plan.first().daysBefore)
        assertEquals(0L, plan.first().delayMillis)
    }

    @Test
    fun `today's alert before 9 waits until 9`() {
        val plan = AlertRules.plan(today.plusDays(30), at(0, 25), zone, isDelivered = { false })

        assertEquals(Duration.ofMinutes(8 * 60 + 35).toMillis(), plan.first().delayMillis)
    }

    @Test
    fun `an alert already shown is not planned again`() {
        val plan = AlertRules.plan(today.plusDays(30), at(13), zone, isDelivered = { it == 30L })

        assertEquals(listOf(14L, 7L, 1L), plan.map { it.daysBefore })
    }

    @Test
    fun `nothing is planned for an expired warranty`() {
        assertTrue(AlertRules.plan(today.minusDays(1), at(10), zone, isDelivered = { false }).isEmpty())
    }

    @Test
    fun `shows the real days left even when the worker runs late`() {
        // Programado a 7 días, pero Doze lo ejecuta 2 días tarde.
        val end = today.plusDays(5)
        val decision = AlertRules.decide(end.toEpochDay(), end, today, true, legacyDaysLeft = 7)

        assertEquals(Decision.Show(5), decision)
    }

    @Test
    fun `skips when the warranty was deleted`() {
        assertEquals(Decision.Skip, AlertRules.decide(today.toEpochDay(), null, today, true, 0))
    }

    @Test
    fun `skips when the user turned alerts off`() {
        val end = today.plusDays(7)
        assertEquals(Decision.Skip, AlertRules.decide(end.toEpochDay(), end, today, false, 7))
    }

    @Test
    fun `skips an alert scheduled for an end date that has since changed`() {
        val oldEnd = today.plusDays(7)
        val newEnd = today.plusYears(1)
        assertEquals(Decision.Skip, AlertRules.decide(oldEnd.toEpochDay(), newEnd, today, true, 7))
    }

    @Test
    fun `skips when the warranty already expired by the time the worker runs`() {
        val end = today.minusDays(1)
        assertEquals(Decision.Skip, AlertRules.decide(end.toEpochDay(), end, today, true, 1))
    }

    @Test
    fun `old jobs without an end date use the days they were scheduled with`() {
        val end = today.plusDays(14)
        assertEquals(Decision.Show(14), AlertRules.decide(null, end, today, true, legacyDaysLeft = 14))
    }
}
