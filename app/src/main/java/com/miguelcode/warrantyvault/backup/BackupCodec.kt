package com.miguelcode.warrantyvault.backup

import com.miguelcode.warrantyvault.domain.backup.InvalidBackupException
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.model.WarrantyTracking
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Formato de warranties.json dentro del ZIP. Lleva versión para poder leer copias
 * antiguas cuando cambie, y cada campo se valida al leer: el fichero viene de fuera y
 * puede estar editado a mano o dañado.
 */
internal object BackupCodec {

    const val FORMAT = "warrantyvault-backup"
    const val VERSION = 1
    const val JSON_ENTRY = "warranties.json"
    const val PHOTOS_DIR = "photos/"

    /** Garantía leída de una copia, con el nombre de su foto dentro del ZIP. */
    data class Entry(val warranty: Warranty, val photoEntry: String?)

    fun encode(entries: List<Entry>, exportedAt: LocalDateTime): String {
        val list = JSONArray()
        entries.forEach { (w, photo) ->
            list.put(
                JSONObject()
                    .put("productName", w.productName)
                    .put("category", w.category.name)
                    .put("store", w.store)
                    .put("price", w.price ?: JSONObject.NULL)
                    .put("purchaseDate", w.purchaseDate.toString())
                    .put("warrantyEndDate", w.warrantyEndDate.toString())
                    .put("notes", w.notes)
                    .put("createdAt", w.createdAt.toString())
                    .put("tracking", w.tracking.name)
                    .put("trackingDate", w.trackingDate?.toString() ?: JSONObject.NULL)
                    .put("photo", photo ?: JSONObject.NULL)
            )
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", exportedAt.toString())
            .put("warranties", list)
            .toString(2)
    }

    /**
     * Lee las garantías válidas e ignora las que no lo son. Lanza si el fichero no es
     * una copia de la app o es de una versión más nueva que esta.
     */
    fun decode(json: String): List<Entry> {
        val root = runCatching { JSONObject(json) }
            .getOrElse { throw InvalidBackupException("Not JSON") }
        if (root.optString("format") != FORMAT) throw InvalidBackupException("Unknown format")
        val version = root.optInt("version", -1)
        if (version !in 1..VERSION) throw InvalidBackupException("Unsupported version $version")
        val list = root.optJSONArray("warranties") ?: throw InvalidBackupException("No warranties")

        return (0 until list.length()).mapNotNull { i ->
            list.optJSONObject(i)?.let { runCatching { decodeEntry(it) }.getOrNull() }
        }
    }

    private fun decodeEntry(o: JSONObject): Entry? {
        val name = o.optString("productName").trim().take(MAX_NAME)
        if (name.isBlank()) return null
        val purchase = LocalDate.parse(o.getString("purchaseDate"))
        val end = LocalDate.parse(o.getString("warrantyEndDate"))
        if (end.isBefore(purchase)) return null
        val photo = o.optNullableString("photo")
            // Solo nombres planos dentro de photos/: nada de rutas con "..".
            ?.takeIf { it.startsWith(PHOTOS_DIR) && isSafeName(it.removePrefix(PHOTOS_DIR)) }
        val warranty = Warranty(
            productName = name,
            category = WarrantyCategory.entries.firstOrNull { it.name == o.optString("category") }
                ?: WarrantyCategory.OTHER,
            store = o.optString("store").take(MAX_STORE),
            price = if (o.isNull("price")) null
            else o.optDouble("price").takeIf { it.isFinite() && it >= 0 },
            purchaseDate = purchase,
            warrantyEndDate = end,
            notes = o.optString("notes").take(MAX_NOTES),
            createdAt = o.optNullableString("createdAt")
                ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
                ?: LocalDateTime.now(),
            tracking = WarrantyTracking.entries.firstOrNull { it.name == o.optString("tracking") }
                ?: WarrantyTracking.NONE,
            trackingDate = o.optNullableString("trackingDate")
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        )
        return Entry(warranty, photo)
    }

    fun isSafeName(name: String): Boolean =
        name.isNotEmpty() && name.length <= 100 && name.all { it.isLetterOrDigit() || it in "-_." } &&
            !name.startsWith(".")

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

    // Los mismos topes que el formulario.
    private const val MAX_NAME = 60
    private const val MAX_STORE = 80
    private const val MAX_NOTES = 2000
}
