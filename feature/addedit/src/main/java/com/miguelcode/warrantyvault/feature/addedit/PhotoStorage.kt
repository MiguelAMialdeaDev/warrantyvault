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
        return runCatching {
            val photosDir = File(context.filesDir, "ticket_photos").apply {
                if (!exists()) mkdirs()
            }
            val destFile = File(photosDir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        }.getOrNull()
    }
}
