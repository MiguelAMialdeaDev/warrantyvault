package com.miguelcode.warrantyvault.feature.addedit.receipt

import java.time.LocalDate

/** Lo que se ha podido sacar de un ticket. Cualquier campo puede faltar. */
data class ReceiptGuess(
    val store: String? = null,
    val purchaseDate: LocalDate? = null,
    val total: Double? = null
) {
    val isEmpty: Boolean get() = store == null && purchaseDate == null && total == null
}

/**
 * Heurísticas sobre el texto reconocido de un ticket español: tienda, fecha y total. Son
 * sugerencias que el usuario confirma, así que ante la duda se devuelve null en vez de
 * rellenar algo dudoso.
 */
object ReceiptParser {

    // El OCR mete a veces espacios junto a las barras ("14/03/ 2026").
    private val dateRegex = Regex("""\b(\d{1,2})\s?[/.\-]\s?(\d{1,2})\s?[/.\-]\s?(\d{2}|\d{4})\b""")
    private val amountRegex = Regex("""(?<![\d.,])(\d{1,3}(?:[.\s]\d{3})*|\d+)[.,](\d{2})(?!\d)""")
    private val totalKeywords = listOf("TOTAL A PAGAR", "IMPORTE TOTAL", "TOTAL EUR", "TOTAL €", "TOTAL", "A PAGAR", "IMPORTE")
    private val notTotal = listOf("SUBTOTAL", "SUB TOTAL", "BASE", "IVA", "CAMBIO", "ENTREGADO", "DESCUENTO", "AHORRO")
    private val notStore = listOf(
        "TICKET", "FACTURA", "FECHA", "HORA", "CIF", "NIF", "C.I.F", "N.I.F", "TEL", "TFNO",
        "WWW", "HTTP", "@", "CALLE", "C/", "AVDA", "AV.", "PLAZA", "S.L", "S.A", "SIMPLIFICADA"
    )

    fun parse(lines: List<String>, today: LocalDate = LocalDate.now()): ReceiptGuess {
        val clean = lines.map { it.trim() }.filter { it.isNotEmpty() }
        return ReceiptGuess(
            store = findStore(clean),
            purchaseDate = findDate(clean, today),
            total = findTotal(clean)
        )
    }

    /** Primera fecha válida que no sea futura ni de hace más de 10 años. */
    internal fun findDate(lines: List<String>, today: LocalDate): LocalDate? =
        lines.asSequence()
            .flatMap { dateRegex.findAll(it) }
            .mapNotNull { m ->
                val (d, mo, y) = m.destructured
                val year = y.toInt().let { if (y.length == 2) 2000 + it else it }
                runCatching { LocalDate.of(year, mo.toInt(), d.toInt()) }.getOrNull()
            }
            .firstOrNull { !it.isAfter(today) && it.isAfter(today.minusYears(10)) }

    /**
     * El importe de la línea de total. Si una línea dice TOTAL y el número está en la
     * siguiente (pasa en tickets a dos columnas), se mira también la siguiente.
     */
    internal fun findTotal(lines: List<String>): Double? {
        val candidates = mutableListOf<Double>()
        lines.forEachIndexed { i, raw ->
            val line = raw.uppercase()
            if (notTotal.any { line.contains(it) && !line.contains("TOTAL A PAGAR") }) return@forEachIndexed
            if (totalKeywords.none { line.contains(it) }) return@forEachIndexed
            val amounts = amounts(raw).ifEmpty { lines.getOrNull(i + 1)?.let(::amounts).orEmpty() }
            candidates += amounts
        }
        return candidates.maxOrNull()?.takeIf { it > 0 && it < MAX_TOTAL }
    }

    /** La primera línea con pinta de nombre: letras de sobra y nada de datos fiscales. */
    internal fun findStore(lines: List<String>): String? =
        lines.take(STORE_LOOKAHEAD).firstOrNull { line ->
            val upper = line.uppercase()
            val letters = line.count { it.isLetter() }
            letters >= 3 &&
                letters >= line.count { it.isDigit() } * 2 &&
                notStore.none { upper.contains(it) } &&
                dateRegex.find(line) == null
        }?.let(::titleCase)?.take(MAX_STORE)

    private fun amounts(line: String): List<Double> = amountRegex.findAll(line).mapNotNull { m ->
        val (whole, cents) = m.destructured
        "${whole.replace(".", "").replace(" ", "")}.$cents".toDoubleOrNull()
    }.toList()

    /** "MEDIA MARKT VALENCIA" -> "Media Markt Valencia"; lo que ya trae minúsculas se deja. */
    private fun titleCase(s: String): String =
        if (s.any { it.isLowerCase() }) s
        else s.lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.titlecase() } }

    private const val STORE_LOOKAHEAD = 5
    private const val MAX_STORE = 80
    private const val MAX_TOTAL = 100_000.0
}
