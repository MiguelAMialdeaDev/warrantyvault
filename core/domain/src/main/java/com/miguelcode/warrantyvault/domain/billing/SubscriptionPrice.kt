package com.miguelcode.warrantyvault.domain.billing

/**
 * Precio de una suscripción tal y como lo devuelve Google Play para el país y la
 * moneda del usuario. Nunca hay que mostrar un precio escrito a mano: Play aplica
 * impuestos, redondeos y conversiones por mercado.
 */
data class SubscriptionPrice(
    /** Ya formateado y localizado por Play, listo para pintar ("1,99 €", "$2.49"…). */
    val formatted: String,
    /** Importe en micros (1.000.000 = 1 unidad de moneda). Sirve para comparar planes. */
    val amountMicros: Long
)
