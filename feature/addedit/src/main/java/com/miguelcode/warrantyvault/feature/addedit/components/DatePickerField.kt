package com.miguelcode.warrantyvault.feature.addedit.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material3.SelectableDates
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.miguelcode.warrantyvault.feature.addedit.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.miguelcode.warrantyvault.ui.common.rememberDateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val fmt = rememberDateFormatter()

    val openLabel = stringResource(R.string.addedit_date_select)
    // El campo entero abre el calendario. Antes solo respondía el icono de 48dp y tocar
    // el texto no hacía nada, que es donde toca casi todo el mundo.
    Box(modifier = modifier) {
        OutlinedTextField(
            value = date?.format(fmt) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = {
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = stringResource(R.string.addedit_date_select),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClickLabel = openLabel) { showDialog = true }
        )
    }

    if (showDialog) {
        // El DatePicker de Material3 interpreta y devuelve los millis SIEMPRE en UTC.
        // Convertir con la zona del dispositivo desplaza la fecha un día en cualquier
        // zona distinta de UTC (hacia atrás al reabrir en UTC+, al guardar en UTC-).
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            // Una compra no puede ser futura: elegirla por error daba avisos desfasados.
            selectableDates = PastOrTodayDates
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onDateSelected(selected)
                    }
                    showDialog = false
                }) { Text(stringResource(R.string.addedit_date_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.addedit_date_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private object PastOrTodayDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val day = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return !day.isAfter(LocalDate.now())
    }

    override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
}
