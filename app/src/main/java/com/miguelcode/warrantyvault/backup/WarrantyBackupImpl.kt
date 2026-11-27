package com.miguelcode.warrantyvault.backup

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.miguelcode.warrantyvault.domain.backup.ImportSummary
import com.miguelcode.warrantyvault.domain.backup.InvalidBackupException
import com.miguelcode.warrantyvault.domain.backup.WarrantyBackup
import com.miguelcode.warrantyvault.domain.model.Result
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyTracking
import com.miguelcode.warrantyvault.domain.repository.PreferencesRepository
import com.miguelcode.warrantyvault.domain.repository.WarrantyRepository
import com.miguelcode.warrantyvault.domain.usecase.CheckPremiumLimitUseCase
import com.miguelcode.warrantyvault.domain.usecase.SyncExpirationAlertsUseCase
import com.miguelcode.warrantyvault.storage.OrphanPhotoSweeper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class WarrantyBackupImpl(
    private val context: Context,
    private val repository: WarrantyRepository,
    private val preferences: PreferencesRepository,
    private val syncExpirationAlerts: SyncExpirationAlertsUseCase,
    private val pdfExporter: WarrantyPdfExporter
) : WarrantyBackup {

    private val photosDir get() = File(context.filesDir, OrphanPhotoSweeper.PHOTOS_DIR_NAME)

    override suspend fun exportZip(destinationUri: String): Result<Int> = io {
        val warranties = repository.getAllWarranties().first()
        val output = context.contentResolver.openOutputStream(Uri.parse(destinationUri))
            ?: throw IOException("Cannot open destination")
        output.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                val entries = warranties.map { w ->
                    val photo = w.receiptPhotoPath?.let(::File)?.takeIf { it.isFile }
                    val entryName = photo?.let { "${BackupCodec.PHOTOS_DIR}${w.id}.jpg" }
                    if (photo != null && entryName != null) {
                        zip.putNextEntry(ZipEntry(entryName))
                        photo.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                    BackupCodec.Entry(w, entryName)
                }
                zip.putNextEntry(ZipEntry(BackupCodec.JSON_ENTRY))
                zip.write(BackupCodec.encode(entries, LocalDateTime.now()).toByteArray())
                zip.closeEntry()
            }
        }
        warranties.size
    }

    override suspend fun importZip(sourceUri: String): Result<ImportSummary> = io {
        val workDir = File(context.cacheDir, "import_${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val (json, photos) = unzip(sourceUri, workDir)
            val entries = BackupCodec.decode(json ?: throw InvalidBackupException("No warranties.json"))

            val existing = repository.getAllWarranties().first()
            val isPremium = preferences.isPremium()
            var usedSlots = repository.getCount()
            var imported = 0
            var duplicates = 0
            var archivedForLimit = 0

            entries.forEach { (incoming, photoEntry) ->
                if (existing.any { it.sameAs(incoming) }) {
                    duplicates++
                    return@forEach
                }
                // Una copia de un usuario Premium no debe perderse al restaurarla en el
                // plan gratuito: lo que no cabe entra archivado y se puede recuperar.
                var warranty = incoming.copy(id = 0)
                if (!warranty.isArchived) {
                    if (!isPremium && usedSlots >= CheckPremiumLimitUseCase.MAX_FREE_WARRANTIES) {
                        warranty = warranty.copy(
                            tracking = WarrantyTracking.ARCHIVED,
                            trackingDate = LocalDate.now()
                        )
                        archivedForLimit++
                    } else {
                        usedSlots++
                    }
                }
                val photoPath = photoEntry?.let(photos::get)?.let(::adoptPhoto)
                when (repository.addWarranty(warranty.copy(receiptPhotoPath = photoPath))) {
                    is Result.Success -> imported++
                    is Result.Error -> photoPath?.let { File(it).delete() }
                }
            }
            // Los avisos no viajan en la copia: se programan para lo recién importado.
            syncExpirationAlerts()
            ImportSummary(imported, duplicates, archivedForLimit)
        } finally {
            workDir.deleteRecursively()
        }
    }

    override suspend fun exportPdf(destinationUri: String): Result<Int> = io {
        val warranties = repository.getAllWarranties().first()
        val output = context.contentResolver.openOutputStream(Uri.parse(destinationUri))
            ?: throw IOException("Cannot open destination")
        output.use { pdfExporter.write(warranties, it) }
        warranties.size
    }

    /**
     * Lee el ZIP entero a la carpeta temporal. Los nombres del ZIP nunca se usan como
     * ruta (zip slip): cada foto se guarda con un nombre propio y se indexa por su
     * nombre dentro del ZIP. Todo lleva tope de tamaño por si el fichero es una bomba.
     */
    private fun unzip(sourceUri: String, workDir: File): Pair<String?, Map<String, File>> {
        var json: String? = null
        val photos = mutableMapOf<String, File>()
        var total = 0L
        val input = context.contentResolver.openInputStream(Uri.parse(sourceUri))
            ?: throw IOException("Cannot open source")
        input.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val name = entry.name
                    when {
                        name == BackupCodec.JSON_ENTRY -> {
                            json = readLimited(zip, MAX_JSON_BYTES).toString(Charsets.UTF_8)
                        }
                        name.startsWith(BackupCodec.PHOTOS_DIR) &&
                            BackupCodec.isSafeName(name.removePrefix(BackupCodec.PHOTOS_DIR)) &&
                            photos.size < MAX_PHOTOS -> {
                            val file = File(workDir, "p${photos.size}")
                            file.outputStream().use { copyLimited(zip, it, MAX_PHOTO_BYTES) }
                            total += file.length()
                            if (total > MAX_TOTAL_BYTES) throw InvalidBackupException("Backup too large")
                            photos[name] = file
                        }
                    }
                    zip.closeEntry()
                }
            }
        }
        return json to photos
    }

    /** Copia la foto a ticket_photos con un nombre nuevo, solo si de verdad es una imagen. */
    private fun adoptPhoto(file: File): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        photosDir.mkdirs()
        val dest = File(photosDir, "${UUID.randomUUID()}.jpg")
        return runCatching { file.copyTo(dest); dest.absolutePath }.getOrNull()
    }

    private fun Warranty.sameAs(other: Warranty) =
        productName.equals(other.productName, ignoreCase = true) &&
            purchaseDate == other.purchaseDate &&
            warrantyEndDate == other.warrantyEndDate

    private fun readLimited(input: InputStream, max: Long): ByteArray =
        java.io.ByteArrayOutputStream().also { copyLimited(input, it, max) }.toByteArray()

    private fun copyLimited(input: InputStream, output: OutputStream, max: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            copied += read
            if (copied > max) throw InvalidBackupException("Entry too large")
            output.write(buffer, 0, read)
        }
    }

    private suspend fun <T> io(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.Success(block())
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private companion object {
        const val MAX_JSON_BYTES = 5L * 1024 * 1024
        const val MAX_PHOTO_BYTES = 20L * 1024 * 1024
        const val MAX_TOTAL_BYTES = 1024L * 1024 * 1024
        const val MAX_PHOTOS = 5000
    }
}
