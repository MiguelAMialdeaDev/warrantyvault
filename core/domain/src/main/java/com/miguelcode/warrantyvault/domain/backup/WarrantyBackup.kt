package com.miguelcode.warrantyvault.domain.backup

import com.miguelcode.warrantyvault.domain.model.Result

/**
 * Copia de las garantías fuera de la app. La copia automática de Google solo guarda la
 * base de datos, no las fotos de los tickets, que son justo lo que se necesita para
 * reclamar. Los destinos y orígenes son uris del selector de documentos del sistema.
 */
interface WarrantyBackup {
    /** ZIP con todas las garantías y sus fotos. Devuelve cuántas garantías se copiaron. */
    suspend fun exportZip(destinationUri: String): Result<Int>

    /** Importa un ZIP hecho con [exportZip]. Nunca sobrescribe ni borra lo que ya hay. */
    suspend fun importZip(sourceUri: String): Result<ImportSummary>

    /** PDF con el resumen de las garantías y una página por ticket (Premium). */
    suspend fun exportPdf(destinationUri: String): Result<Int>
}

data class ImportSummary(
    val imported: Int,
    /** Ya existían (mismo producto y fechas): no se duplican. */
    val skippedDuplicates: Int,
    /** Entraron archivadas porque el plan gratuito ya estaba lleno. */
    val archivedForLimit: Int
)

/** El fichero no es una copia de WarrantyVault o está dañado. */
class InvalidBackupException(message: String) : Exception(message)
