package com.miguelcode.warrantyvault.feature.addedit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
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
        val name = UUID.randomUUID().toString()
        val destFile = File(photosDir, "$name.jpg")
        // Copia cruda en la caché: el original se lee una sola vez (puede venir de la nube)
        // y desde un fichero se pueden leer las dimensiones y el EXIF antes de decodificar.
        val rawFile = File(context.cacheDir, "$name.raw")
        val tmpFile = File(photosDir, "$name.tmp")

        return runCatching {
            // Si openInputStream devuelve null (provider caído, URI ya no accesible), el
            // `?.use` de antes se saltaba entero y la función devolvía igualmente la ruta:
            // se guardaba en Room el path de un fichero que nunca llegó a existir.
            val input = context.contentResolver.openInputStream(sourceUri)
                ?: return null
            input.use { source ->
                rawFile.outputStream().use { output ->
                    copyWithLimit(source, output, MAX_PHOTO_BYTES)
                }
            }
            // Antes se guardaba el original tal cual: hasta 20 MB por ticket, con la
            // ubicación GPS y el modelo del móvil en el EXIF, y con extensión .jpg aunque
            // fuera PNG o HEIC. Ahora se reduce, se gira según el EXIF y se vuelve a
            // codificar como JPEG sin metadatos (unos cientos de KB).
            val bitmap = decodeUpright(rawFile) ?: return@runCatching null
            try {
                tmpFile.outputStream().use { out ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out))
                }
            } finally {
                bitmap.recycle()
            }
            // Renombrado al final: nunca queda a la vista un JPEG a medio escribir.
            check(tmpFile.renameTo(destFile))
            destFile.absolutePath
        }.getOrElse {
            // Una copia interrumpida a medias deja un fichero parcial que ya no
            // referencia nadie: se borra aquí porque nada más lo va a limpiar.
            destFile.delete()
            null
        }.also {
            rawFile.delete()
            tmpFile.delete()
        }
    }

    /** Decodifica reducida a [MAX_SIDE_PX] como mucho y girada según su EXIF. */
    private fun decodeUpright(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_SIDE_PX)
        }
        val sampled = BitmapFactory.decodeFile(file.path, options) ?: return null
        val scaled = scaleDown(sampled, MAX_SIDE_PX)

        val orientation = runCatching {
            ExifInterface(file.path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = orientationMatrix(orientation) ?: return scaled
        return Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
            .also { if (it !== scaled) scaled.recycle() }
    }

    /** inSampleSize solo reduce en potencias de 2: el ajuste fino lo hace [scaleDown]. */
    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val ratio = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        ).also { if (it !== bitmap) bitmap.recycle() }
    }

    private fun orientationMatrix(orientation: Int): Matrix? = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { postRotate(90f) }
        ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { postRotate(180f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { postRotate(270f) }
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { postScale(1f, -1f) }
        ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply { postRotate(90f); postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply { postRotate(270f); postScale(-1f, 1f) }
        else -> null
    }

    /**
     * Mayor potencia de 2 que deja el lado largo en [maxSide] o por encima. Así se
     * decodifica sin cargar en memoria una foto de 50 MP entera.
     */
    fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        val longest = maxOf(width, height)
        while (longest / (sample * 2) >= maxSide) sample *= 2
        return sample
    }

    /** Lado largo máximo guardado: de sobra para leer un ticket con zoom. */
    const val MAX_SIDE_PX = 2048
    private const val JPEG_QUALITY = 85

    /**
     * Fichero temporal donde la cámara del sistema deja la foto. Vive en la caché y se
     * borra tras copiarlo a ticket_photos; si la app muere antes, Android limpia la
     * caché solo cuando le hace falta espacio, y cada captura nueva pisa la anterior.
     */
    fun newCaptureFile(context: Context): File {
        val dir = File(context.cacheDir, CAPTURE_DIR).apply { mkdirs() }
        return File(dir, CAPTURE_FILE).apply { delete() }
    }

    /** Uri del FileProvider para el fichero de captura (ver res/xml/file_paths.xml de :app). */
    fun captureUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private const val CAPTURE_DIR = "camera"
    private const val CAPTURE_FILE = "capture.jpg"

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
