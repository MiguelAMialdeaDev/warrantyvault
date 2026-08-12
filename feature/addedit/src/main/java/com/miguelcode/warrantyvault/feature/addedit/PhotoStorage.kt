package com.miguelcode.warrantyvault.feature.addedit

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object PhotoStorage {
    /**
     * Copia una imagen desde un URI del photo picker al almacenamiento interno de la app.
     * Devuelve la ruta absoluta del archivo copiado, o null si falla.
     */
    fun copyToInternal(context: Context, sourceUri: Uri): String? {
        val photosDir = File(context.filesDir, "ticket_photos").apply {
            if (!exists()) mkdirs()
        }
        val destFile = File(photosDir, "${UUID.randomUUID()}.jpg")

        return runCatching {
            // Si openInputStream devuelve null (provider caído, URI ya no accesible), el
            // `?.use` de antes se saltaba entero y la función devolvía igualmente la ruta:
            // se guardaba en Room el path de un fichero que nunca llegó a existir.
            val input = context.contentResolver.openInputStream(sourceUri)
                ?: return null
            input.use { source ->
                destFile.outputStream().use { output ->
                    source.copyTo(output)
                }
            }
            destFile.absolutePath
        }.getOrElse {
            // Una copia interrumpida a medias deja un fichero parcial que ya no
            // referencia nadie: se borra aquí porque nada más lo va a limpiar.
            destFile.delete()
            null
        }
    }
}
