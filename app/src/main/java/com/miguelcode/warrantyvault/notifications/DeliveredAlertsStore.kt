package com.miguelcode.warrantyvault.notifications

import android.content.Context

/**
 * Recuerda qué avisos ya se mostraron para no repetirlos.
 *
 * Hace falta desde que los avisos del día en curso se lanzan aunque su hora ya haya
 * pasado: como el sync se ejecuta en cada arranque, abrir la app varias veces el mismo día
 * volvía a encolar un aviso ya entregado y el usuario lo veía reaparecer después de
 * descartarlo.
 *
 * Cada combinación de garantía y días de antelación se muestra una sola vez en la vida de
 * la garantía, así que la clave no necesita fecha.
 *
 * Va en SharedPreferences y no en DataStore a propósito: lo lee un worker que puede
 * ejecutarse con la app cerrada, y aquí la lectura síncrona es una ventaja.
 */
class DeliveredAlertsStore(context: Context) {

    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun isDelivered(warrantyId: Long, daysBefore: Long): Boolean =
        prefs.getBoolean(keyFor(warrantyId, daysBefore), false)

    fun markDelivered(warrantyId: Long, daysBefore: Long) {
        prefs.edit().putBoolean(keyFor(warrantyId, daysBefore), true).apply()
    }

    /**
     * Se llama al borrar una garantía o al cambiarle la fecha de fin: sus avisos vuelven a
     * estar disponibles y, si no, las claves quedarían ahí para siempre.
     */
    fun clearFor(warrantyId: Long) {
        val prefix = "$warrantyId$SEPARATOR"
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach { editor.remove(it) }
        editor.apply()
    }

    private fun keyFor(warrantyId: Long, daysBefore: Long) = "$warrantyId$SEPARATOR$daysBefore"

    private companion object {
        const val FILE_NAME = "delivered_alerts"
        const val SEPARATOR = "_"
    }
}
