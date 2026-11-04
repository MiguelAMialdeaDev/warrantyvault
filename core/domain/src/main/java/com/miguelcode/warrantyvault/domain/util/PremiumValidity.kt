package com.miguelcode.warrantyvault.domain.util

import java.util.concurrent.TimeUnit

/**
 * El premium guardado en el móvil es solo una caché de lo que dice Google Play. Si Play
 * no lo confirma en [MAX_UNVERIFIED_MILLIS] (móvil sin Play Services, Play bloqueado a
 * propósito), la caché deja de valer. Antes el flag no caducaba nunca: un premium
 * restaurado o manipulado se quedaba para siempre si la app no llegaba a hablar con Play.
 *
 * Catorce días dejan margen de sobra a quien pasa una temporada sin conexión.
 */
object PremiumValidity {

    val MAX_UNVERIFIED_MILLIS: Long = TimeUnit.DAYS.toMillis(14)

    fun isActive(flag: Boolean, lastVerifiedAtMillis: Long, nowMillis: Long): Boolean {
        if (!flag) return false
        val age = nowMillis - lastVerifiedAtMillis
        // Un reloj atrasado (age negativo) tampoco vale: es la forma fácil de alargarlo.
        return age in 0..MAX_UNVERIFIED_MILLIS
    }
}
