package com.miguelcode.warrantyvault.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

/**
 * Fecha en el formato del idioma del móvil ("1 oct 2026", "Oct 1, 2026"). El patrón
 * fijo dd/MM/yyyy se leía al revés en inglés de EE. UU.
 */
@Composable
fun rememberDateFormatter(): DateTimeFormatter {
    val locale = currentLocale()
    return remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
}

/** Precio en euros con separadores y posición del símbolo según el idioma ("1.299,99 €", "€1,299.99"). */
@Composable
fun formatPrice(price: Double): String {
    val locale = currentLocale()
    val format = remember(locale) {
        NumberFormat.getCurrencyInstance(locale).apply { currency = Currency.getInstance("EUR") }
    }
    return format.format(price)
}

@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
