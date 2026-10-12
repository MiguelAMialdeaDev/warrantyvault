package com.miguelcode.warrantyvault.domain.util

import java.math.BigDecimal
import java.math.RoundingMode

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
        // Solo punto: mismo criterio que la coma. En España el punto ES el separador
        // de millares (el propio parser ya lo trata así en "1.299,99"), así que sin
        // esta simetría "1.299" se leía como 1,299: el precio quedaba dividido por mil
        // según el usuario escribiese o no los céntimos.
        lastDot >= 0 ->
            if (cleaned.length - lastDot - 1 in 1..2) cleaned
            else cleaned.replace(".", "")
        else -> cleaned
    }

    return normalised.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
}

/**
 * Texto con el que se rellena el campo al editar, garantizado para volver a parsearse
 * con [toPriceOrNull] al mismo valor.
 *
 * `Double.toString()` no lo cumplía: 1299.999 se escribía "1299.999", que el parser lee
 * como millares (1299999), y 1.0E7 salía en notación científica. Cada edición, aunque
 * solo se tocasen las notas, multiplicaba el precio por mil. Se redondea a céntimos y
 * se quitan los ceros sobrantes, así que siempre quedan 0, 1 o 2 decimales tras el punto.
 */
fun Double.toPriceInput(): String =
    BigDecimal.valueOf(this)
        .setScale(2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
