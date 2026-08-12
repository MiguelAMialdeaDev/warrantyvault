package com.miguelcode.warrantyvault.domain.util

/**
 * Convierte el precio tecleado a `Double` aceptando coma decimal.
 *
 * `toDoubleOrNull()` solo entiende el punto, así que un usuario español que escribe
 * "1299,99" (que es como se escribe aquí, y lo que ofrece el teclado numérico) obtenía
 * `null`: el precio se descartaba en silencio, sin ningún aviso, y la garantía se guardaba
 * sin él. También se limpian los separadores de millar y los espacios, incluido el espacio
 * duro que algunos teclados insertan.
 *
 * Devuelve `null` si el texto no es un número válido o si es negativo.
 */
fun String.toPriceOrNull(): Double? {
    val cleaned = trim().replace(" ", "").replace(" ", "")
    if (cleaned.isEmpty()) return null

    val lastComma = cleaned.lastIndexOf(',')
    val lastDot = cleaned.lastIndexOf('.')

    val normalised = when {
        // Están los dos: el último es el decimal y el otro separa millares
        // (1.299,99 en formato español, 1,299.99 en formato inglés).
        lastComma >= 0 && lastDot >= 0 ->
            if (lastComma > lastDot) cleaned.replace(".", "").replace(',', '.')
            else cleaned.replace(",", "")
        // Solo coma: es decimal si separa una o dos cifras finales ("1,29"),
        // y de millares en cualquier otro caso ("1,299").
        lastComma >= 0 ->
            if (cleaned.length - lastComma - 1 in 1..2) cleaned.replace(',', '.')
            else cleaned.replace(",", "")
        else -> cleaned
    }

    return normalised.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
}
