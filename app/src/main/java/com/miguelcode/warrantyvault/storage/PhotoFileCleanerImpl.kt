package com.miguelcode.warrantyvault.storage

import com.miguelcode.warrantyvault.domain.storage.PhotoFileCleaner
import java.io.File

/**
 * Solo borra ficheros que estén DIRECTAMENTE dentro de [photosDir].
 *
 * La ruta llega de Room o del borrador guardado del formulario, y antes se borraba sin
 * mirar: una ruta manipulada (copia de seguridad editada y restaurada, o un cambio futuro
 * que guardase otra cosa) podía borrar la base de datos o las preferencias de la propia
 * app. Se compara con rutas canónicas para que `../` o un enlace no se cuelen.
 */
class PhotoFileCleanerImpl(private val photosDir: File) : PhotoFileCleaner {
    override fun delete(absolutePath: String?) {
        if (absolutePath.isNullOrBlank()) return
        runCatching {
            val base = photosDir.canonicalFile
            val target = File(absolutePath).canonicalFile
            if (target.parentFile == base && target.isFile) target.delete()
        }
    }
}
