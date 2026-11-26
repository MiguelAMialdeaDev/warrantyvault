package com.miguelcode.warrantyvault.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Puente entre el aviso de caducidad y MainActivity.
 *
 * El aviso abre el alias no exportado declarado en el manifiesto. Solo los intents que
 * llegan por ese alias pueden traer el id de garantía: el lanzador, que sí está
 * exportado, sigue sin aceptar extras de fuera.
 */
object NotificationEntry {

    private const val ALIAS_CLASS = "com.miguelcode.warrantyvault.notifications.OpenWarrantyFromNotification"
    private const val EXTRA_WARRANTY_ID = "com.miguelcode.warrantyvault.extra.OPEN_WARRANTY_ID"

    fun openWarrantyIntent(context: Context, warrantyId: Long): Intent =
        Intent().setComponent(ComponentName(context.packageName, ALIAS_CLASS))
            .putExtra(EXTRA_WARRANTY_ID, warrantyId)

    /** Id de la garantía a abrir, o null si el intent no viene del aviso o es inválido. */
    fun warrantyIdFrom(intent: Intent?): Long? =
        warrantyIdFrom(intent?.component?.className, intent?.getLongExtra(EXTRA_WARRANTY_ID, -1L) ?: -1L)

    internal fun warrantyIdFrom(componentClass: String?, rawId: Long): Long? =
        rawId.takeIf { componentClass == ALIAS_CLASS && it > 0L }
}
