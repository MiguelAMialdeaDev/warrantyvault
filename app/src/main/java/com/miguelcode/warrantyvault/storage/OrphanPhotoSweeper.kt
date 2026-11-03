package com.miguelcode.warrantyvault.storage

import java.io.File

/**
 * Borra las fotos de tickets que ya no usa ninguna garantía.
 *
 * Elegir una foto la copia al momento a `ticket_photos`. Si el usuario sale sin guardar,
 * o cambia de foto antes de guardar, la copia anterior se quedaba en disco para siempre.
 *
 * Solo se borran ficheros con más de [minAgeMillis] de antigüedad: un borrador que se
 * recupera tras la muerte del proceso tiene su foto copiada pero aún sin guardar, y no
 * debe desaparecer al arrancar.
 */
class OrphanPhotoSweeper(
    private val photosDir: File,
    private val minAgeMillis: Long = DEFAULT_MIN_AGE_MILLIS,
    private val now: () -> Long = System::currentTimeMillis
) {
    /** Devuelve cuántos ficheros ha borrado. */
    fun sweep(referencedPaths: Set<String>): Int {
        val files = photosDir.listFiles() ?: return 0
        // Se compara por nombre (un UUID por foto), no por ruta: la misma carpeta puede
        // aparecer como /data/user/0/... o /data/data/..., y comparar rutas borraría
        // fotos que sí están en uso.
        val referenced = referencedPaths.map { File(it).name }.toSet()
        val cutoff = now() - minAgeMillis
        return files.count { file ->
            file.isFile &&
                file.name !in referenced &&
                file.lastModified() < cutoff &&
                runCatching { file.delete() }.getOrDefault(false)
        }
    }

    companion object {
        const val PHOTOS_DIR_NAME = "ticket_photos"
        const val DEFAULT_MIN_AGE_MILLIS = 2L * 24 * 60 * 60 * 1000
    }
}
