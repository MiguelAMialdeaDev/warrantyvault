package com.miguelcode.warrantyvault.feature.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Euro
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.feature.detail.R
import com.miguelcode.warrantyvault.ui.common.rememberDateFormatter
import androidx.compose.ui.res.pluralStringResource
import com.miguelcode.warrantyvault.domain.util.warrantyMonths
import com.miguelcode.warrantyvault.ui.common.formatPrice

@Composable
fun InfoCard(
    warranty: Warranty,
    modifier: Modifier = Modifier
) {
    val fmt = rememberDateFormatter()
    val totalMonths = warrantyMonths(warranty.purchaseDate, warranty.warrantyEndDate)
    val durationText = when {
        totalMonths == 12L -> stringResource(R.string.detail_duration_year_one)
        totalMonths >= 12 && totalMonths % 12 == 0L -> (totalMonths / 12).toInt().let { pluralStringResource(R.plurals.detail_duration_years, it, it) }
        else -> totalMonths.toInt().let { pluralStringResource(R.plurals.detail_duration_months, it, it) }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            if (warranty.store.isNotBlank()) {
                InfoRow(icon = Icons.Outlined.Store, label = stringResource(R.string.detail_info_store), value = warranty.store)
                InfoDivider()
            }
            warranty.price?.let { price ->
                InfoRow(icon = Icons.Outlined.Euro, label = stringResource(R.string.detail_info_price), value = formatPrice(price))
                InfoDivider()
            }
            InfoRow(icon = Icons.Outlined.CalendarMonth, label = stringResource(R.string.detail_info_purchase_date), value = warranty.purchaseDate.format(fmt))
            InfoDivider()
            InfoRow(icon = Icons.Outlined.CalendarMonth, label = stringResource(R.string.detail_info_end_date), value = warranty.warrantyEndDate.format(fmt))
            InfoDivider()
            InfoRow(icon = Icons.Outlined.Schedule, label = stringResource(R.string.detail_info_duration), value = durationText)
            if (warranty.notes.isNotBlank()) {
                InfoDivider()
                NotesRow(value = warranty.notes)
            }
        }
    }
}

@Composable
private fun NotesRow(value: String) {
    // Fila específica para notas: sin altura fija y con soporte para texto
    // multilínea (scroll provisto por el LazyColumn exterior en DetailScreen).
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Notes,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.detail_info_notes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 32.dp)
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 12.dp).weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}
