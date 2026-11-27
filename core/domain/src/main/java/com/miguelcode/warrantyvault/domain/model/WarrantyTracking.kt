package com.miguelcode.warrantyvault.domain.model

/**
 * Seguimiento manual de la garantía, aparte de su estado por fechas. Antes una garantía
 * caducada o ya reclamada solo podía borrarse, y con ella el historial; además ocupaba
 * uno de los 3 huecos del plan gratuito.
 */
enum class WarrantyTracking {
    /** Sin seguimiento: la garantía normal. */
    NONE,

    /** El producto está en reclamación o en el servicio técnico. */
    IN_CLAIM,

    /** La reclamación terminó: reparado o cambiado. */
    REPAIRED,

    /** Guardada para el historial: no avisa ni cuenta para el límite gratuito. */
    ARCHIVED
}
