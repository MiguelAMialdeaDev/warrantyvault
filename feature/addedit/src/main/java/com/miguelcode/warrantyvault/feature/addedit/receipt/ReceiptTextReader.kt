package com.miguelcode.warrantyvault.feature.addedit.receipt

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.File

/** Lee el texto de la foto de un ticket, línea a línea en orden de lectura. */
fun interface ReceiptTextReader {
    /** null si no se pudo leer (sin Play Services, modelo sin descargar, imagen rota). */
    suspend fun readLines(photoPath: String): List<String>?
}

/**
 * OCR de ML Kit en el propio móvil: el ticket no sale del dispositivo, que es lo que
 * promete la política de privacidad.
 */
class MlKitReceiptTextReader(private val context: Context) : ReceiptTextReader {

    override suspend fun readLines(photoPath: String): List<String>? {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(File(photoPath)))
            rows(recognizer.process(image).await())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } finally {
            recognizer.close()
        }
    }

    /**
     * ML Kit devuelve bloques por columnas: en un ticket "TOTAL" y "599,00" llegan como
     * líneas separadas. Se juntan las que están a la misma altura para que cada fila del
     * ticket quede en una sola línea.
     */
    private fun rows(text: Text): List<String> {
        val lines = text.textBlocks.flatMap { it.lines }.filter { it.boundingBox != null }
            .sortedBy { it.boundingBox!!.centerY() }
        val rows = mutableListOf<MutableList<Text.Line>>()
        lines.forEach { line ->
            val box = line.boundingBox!!
            val row = rows.lastOrNull()
            val rowBox = row?.first()?.boundingBox
            if (row != null && rowBox != null && kotlin.math.abs(box.centerY() - rowBox.centerY()) < rowBox.height() / 2) {
                row += line
            } else {
                rows += mutableListOf(line)
            }
        }
        return rows.map { row -> row.sortedBy { it.boundingBox!!.left }.joinToString(" ") { it.text } }
    }
}
