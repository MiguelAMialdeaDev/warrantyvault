package com.miguelcode.warrantyvault.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class Warranty(
    val id: Long = 0,
    val productName: String,
    val category: WarrantyCategory,
    val store: String = "",
    val price: Double? = null,
    val purchaseDate: LocalDate,
    val warrantyEndDate: LocalDate,
    val receiptPhotoPath: String? = null,
    val notes: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Días a partir de los cuales la garantía se considera "por vencer".
     *
     * Debe coincidir con el primer aviso de `NotificationConstants.ALERT_DAYS_BEFORE`.
     * Antes no coincidía: el estado usaba `isAfter`, que es estricto, así que cubría de 0
     * a 29 días mientras la primera notificación saltaba a los 30. El usuario recibía
     * "tu garantía está a punto de vencer", abría la app y veía la etiqueta verde de
     * ACTIVA, y el filtro "Por vencer" tampoco la listaba.
     */
    val status: WarrantyStatus
        get() {
            // Una sola lectura del día: leerlo dos veces puede comparar contra dos
            // "hoy" distintos si el getter se evalúa justo al cruzar medianoche.
            val today = LocalDate.now()
            return when {
                today.isAfter(warrantyEndDate) -> WarrantyStatus.EXPIRED
                !today.plusDays(EXPIRING_SOON_DAYS).isBefore(warrantyEndDate) -> WarrantyStatus.EXPIRING_SOON
                else -> WarrantyStatus.ACTIVE
            }
        }

    val daysRemaining: Long
        get() = ChronoUnit.DAYS.between(LocalDate.now(), warrantyEndDate)

    val progressFraction: Float
        get() {
            val total = ChronoUnit.DAYS.between(purchaseDate, warrantyEndDate).toFloat()
            // Los dos casos degenerados no significan lo mismo y no deben pintarse igual:
            //  - Compra y fin el mismo día: garantía de duración cero, agotada -> barra llena.
            //  - Fin ANTES que la compra: el dato está mal introducido, no hay nada que
            //    consumir. Una barra llena se leería como "garantía agotada" y ocultaría
            //    el error, así que se deja vacía.
            if (total == 0f) return 1f
            if (total < 0f) return 0f
            val elapsed = ChronoUnit.DAYS.between(purchaseDate, LocalDate.now()).toFloat()
            return (elapsed / total).coerceIn(0f, 1f)
        }

    companion object {
        const val EXPIRING_SOON_DAYS = 30L
    }
}
