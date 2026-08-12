package com.miguelcode.warrantyvault

import android.content.Context

/**
 * Copia del modo de tema que se puede leer de forma síncrona en `onCreate`.
 *
 * La preferencia real vive en DataStore, que es asíncrono: cuando llega su primer valor la
 * ventana ya está creada con el tema de la plataforma. Con `values-night` se arregla el
 * caso de seguir al sistema, pero no el de un usuario que fuerza "Oscuro" con el sistema
 * en claro: seguía viendo un fotograma claro en cada arranque en frío.
 *
 * Aquí solo se guarda el modo elegido, nunca se decide nada con él más allá de qué tema de
 * ventana aplicar antes de la primera composición.
 */
object ThemeModeCache {

    fun read(context: Context): String =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MODE, MODE_SYSTEM) ?: MODE_SYSTEM

    fun write(context: Context, mode: String) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode)
            .apply()
    }

    const val MODE_SYSTEM = "system"
    const val MODE_DARK = "dark"
    const val MODE_LIGHT = "light"

    private const val FILE_NAME = "theme_mode_cache"
    private const val KEY_MODE = "theme_mode"
}
