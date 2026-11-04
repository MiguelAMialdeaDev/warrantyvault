package com.miguelcode.warrantyvault.feature.addedit

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

object PhotoStorage {

    /**
     * Tope por foto. Una foto de móvil sin recomprimir ronda los 2 a 8 MB; por encima de
     * esto no es un ticket, y sin tope una imagen enorme (o un proveedor que no deja de
     * mandar bytes) llenaba el almacenamiento.
     */
    const val MAX_PHOTO_BYTES: Long = 20L * 1024 * 1024

    /**
     * Copia una imagen desde un URI del photo picker al almacenamiento interno de la app.
     * Devuelve la ruta absoluta del archivo copiado, o null si falla, si no es una imagen
     * o si supera [MAX_PHOTO_BYTES].
     */
    fun copyToInternal(context: Context, sourceUri: Uri): String? {
        // En Android 8 a 10 sin el selector de fotos del sistema, PickVisualMedia cae al
        // selector de documentos, que deja elegir cualquier fichero de cualquier app.
        val mime = context.contentResolver.getType(sourceUri)
        if (mime != null && !mime.startsWith("image/")) return null

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
                    copyWithLimit(source, output, MAX_PHOTO_BYTES)
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

    /** Copia como `copyTo`, pero lanza en cuanto se pasa del tope. */
    fun copyWithLimit(source: InputStream, output: OutputStream, maxBytes: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = source.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw IOException("La foto supera el tamaño máximo")
            output.write(buffer, 0, read)
        }
    }
}
