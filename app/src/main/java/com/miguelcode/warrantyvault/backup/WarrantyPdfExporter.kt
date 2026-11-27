package com.miguelcode.warrantyvault.backup

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.miguelcode.warrantyvault.R
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyStatus
import com.miguelcode.warrantyvault.domain.model.WarrantyTracking
import java.io.File
import java.io.OutputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

/**
 * PDF para llevar a la tienda o al servicio técnico: una tabla con todas las garantías y,
 * después, una página por cada ticket con sus datos. Usa PdfDocument del sistema, sin
 * librerías, en tamaño A4 (595 x 842 puntos).
 */
class WarrantyPdfExporter(private val context: Context) {

    private val locale: Locale get() = context.resources.configuration.locales[0]
    private val dateFormat get() = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

    private val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(26, 27, 30)
    }
    private val header = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(70, 70, 79)
    }
    private val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9.5f; color = Color.rgb(26, 27, 30)
    }
    private val muted = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9f; color = Color.rgb(119, 118, 128)
    }
    private val rule = Paint().apply { color = Color.rgb(228, 226, 236); strokeWidth = 0.7f }

    fun write(warranties: List<Warranty>, output: OutputStream) {
        val doc = PdfDocument()
        try {
            writeSummary(doc, warranties)
            warranties.filter { it.receiptPhotoPath?.let(::File)?.isFile == true }
                .forEach { writeReceiptPage(doc, it) }
            doc.writeTo(output)
        } finally {
            doc.close()
        }
    }

    private fun writeSummary(doc: PdfDocument, warranties: List<Warranty>) {
        // Columnas: producto, tienda, compra, fin, estado, precio.
        val columns = floatArrayOf(MARGIN, 190f, 290f, 360f, 430f, 515f)
        val headers = listOf(
            R.string.pdf_col_product, R.string.pdf_col_store, R.string.pdf_col_purchase,
            R.string.pdf_col_end, R.string.pdf_col_status, R.string.pdf_col_price
        ).map(context::getString)

        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun newPage() {
            page?.let(doc::finishPage)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            val canvas = page!!.canvas
            y = MARGIN + 20f
            if (pageNumber == 1) {
                canvas.drawText(context.getString(R.string.pdf_title), MARGIN, y, title)
                y += 18f
                canvas.drawText(
                    context.getString(R.string.pdf_generated_on, LocalDate.now().format(dateFormat), warranties.size),
                    MARGIN, y, muted
                )
                y += 26f
            }
            headers.forEachIndexed { i, h -> canvas.drawText(h, columns[i], y, header) }
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, rule)
            y += 14f
        }

        newPage()
        warranties.sortedBy { it.warrantyEndDate }.forEach { w ->
            if (y > PAGE_H - MARGIN) newPage()
            val canvas = page!!.canvas
            val cells = listOf(
                w.productName, w.store, w.purchaseDate.format(dateFormat),
                w.warrantyEndDate.format(dateFormat), statusLabel(w), w.price?.let(::formatPrice).orEmpty()
            )
            cells.forEachIndexed { i, text ->
                val width = (if (i + 1 < columns.size) columns[i + 1] else PAGE_W - MARGIN) - columns[i] - 6f
                canvas.drawText(ellipsize(text, width, body), columns[i], y, body)
            }
            y += 8f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, rule)
            y += 14f
        }
        page?.let(doc::finishPage)
    }

    private fun writeReceiptPage(doc: PdfDocument, w: Warranty) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, doc.pages.size + 1).create())
        val canvas = page.canvas
        var y = MARGIN + 20f
        canvas.drawText(ellipsize(w.productName, PAGE_W - 2 * MARGIN, title), MARGIN, y, title)
        y += 20f
        val lines = buildList {
            if (w.store.isNotBlank()) add(context.getString(R.string.pdf_line_store, w.store))
            add(context.getString(R.string.pdf_line_purchase, w.purchaseDate.format(dateFormat)))
            add(context.getString(R.string.pdf_line_end, w.warrantyEndDate.format(dateFormat), statusLabel(w)))
            w.price?.let { add(context.getString(R.string.pdf_line_price, formatPrice(it))) }
            if (w.notes.isNotBlank()) add(context.getString(R.string.pdf_line_notes, w.notes.replace('\n', ' ')))
        }
        lines.forEach {
            canvas.drawText(ellipsize(it, PAGE_W - 2 * MARGIN, body), MARGIN, y, body)
            y += 14f
        }
        y += 10f
        drawPhoto(canvas, File(w.receiptPhotoPath!!), RectF(MARGIN, y, PAGE_W - MARGIN, PAGE_H - MARGIN))
        doc.finishPage(page)
    }

    /** Ajusta la foto al hueco sin deformarla, reducida al decodificar para no gastar memoria. */
    private fun drawPhoto(canvas: Canvas, file: File, box: RectF) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= box.width() * 2) sample *= 2
        val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return
        try {
            val scale = minOf(box.width() / bitmap.width, box.height() / bitmap.height)
            val w = bitmap.width * scale
            val h = bitmap.height * scale
            val left = box.left + (box.width() - w) / 2
            canvas.drawBitmap(bitmap, null, RectF(left, box.top, left + w, box.top + h), Paint(Paint.FILTER_BITMAP_FLAG))
        } finally {
            bitmap.recycle()
        }
    }

    private fun statusLabel(w: Warranty): String = when (w.tracking) {
        WarrantyTracking.IN_CLAIM -> context.getString(R.string.pdf_status_in_claim)
        WarrantyTracking.REPAIRED -> context.getString(R.string.pdf_status_repaired)
        WarrantyTracking.ARCHIVED -> context.getString(R.string.pdf_status_archived)
        WarrantyTracking.NONE -> when (w.status) {
            WarrantyStatus.ACTIVE -> context.getString(R.string.pdf_status_active)
            WarrantyStatus.EXPIRING_SOON -> context.getString(R.string.pdf_status_expiring)
            WarrantyStatus.EXPIRED -> context.getString(R.string.pdf_status_expired)
        }
    }

    private fun formatPrice(price: Double): String =
        NumberFormat.getCurrencyInstance(locale).apply { currency = Currency.getInstance("EUR") }.format(price)

    private fun ellipsize(text: String, width: Float, paint: TextPaint): String =
        TextUtils.ellipsize(text, paint, width, TextUtils.TruncateAt.END).toString()

    private companion object {
        const val PAGE_W = 595
        const val PAGE_H = 842
        const val MARGIN = 40f
    }
}
