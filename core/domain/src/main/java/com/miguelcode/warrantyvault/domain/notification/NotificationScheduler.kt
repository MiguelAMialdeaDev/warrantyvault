package com.miguelcode.warrantyvault.domain.notification

import com.miguelcode.warrantyvault.domain.model.Warranty

interface NotificationScheduler {
    fun scheduleExpirationAlerts(warranty: Warranty)
    /**
     * Cancela los avisos pendientes. Con [forgetDelivered] a false se conserva el registro
     * de avisos ya mostrados: es lo correcto al editar sin cambiar la fecha de fin, para
     * que el aviso de hoy no vuelva a salir.
     */
    fun cancelAlerts(warrantyId: Long, forgetDelivered: Boolean = true)
    fun cancelAll()
}
