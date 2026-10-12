package com.miguelcode.warrantyvault.domain.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Duración en meses que el usuario introdujo, no la distancia natural entre fechas.
 *
 * `plusMonths` recorta el día cuando el mes destino es más corto (31 ene + 1 mes =
 * 28 feb) y `MONTHS.between` no revierte ese recorte: devuelve un mes de menos. Lo
 * usan el formulario de edición y la ficha de detalle, que antes mostraba "0 meses"
 * para una garantía de un mes comprada el 31 de enero.
 */
fun warrantyMonths(start: LocalDate, end: LocalDate): Long {
    val base = ChronoUnit.MONTHS.between(start, end)
    return if (start.plusMonths(base + 1) == end) base + 1 else base
}
