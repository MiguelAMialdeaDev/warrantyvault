package com.miguelcode.warrantyvault.feature.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyTracking
import com.miguelcode.warrantyvault.feature.detail.R
import com.miguelcode.warrantyvault.ui.common.label
import com.miguelcode.warrantyvault.ui.common.rememberDateFormatter

/**
 * Seguimiento manual: en reclamación, reparada o archivada. Solo se ofrecen los pasos que
 * tienen sentido desde el estado actual, como mucho dos, para no llenar la pantalla.
 */
@Composable
fun TrackingSection(
    warranty: Warranty,
    enabled: Boolean,
    onChange: (WarrantyTracking) -> Unit,
    modifier: Modifier = Modifier
) {
    val fmt = rememberDateFormatter()
    val current = warranty.tracking.label()
    val since = warranty.trackingDate?.format(fmt)
    val actions: List<Pair<WarrantyTracking, Int>> = when (warranty.tracking) {
        WarrantyTracking.NONE -> listOf(
            WarrantyTracking.IN_CLAIM to R.string.detail_tracking_start_claim,
            WarrantyTracking.ARCHIVED to R.string.detail_tracking_archive
        )
        WarrantyTracking.IN_CLAIM -> listOf(
            WarrantyTracking.REPAIRED to R.string.detail_tracking_mark_repaired,
            WarrantyTracking.NONE to R.string.detail_tracking_clear
        )
        WarrantyTracking.REPAIRED -> listOf(
            WarrantyTracking.NONE to R.string.detail_tracking_clear,
            WarrantyTracking.ARCHIVED to R.string.detail_tracking_archive
        )
        WarrantyTracking.ARCHIVED -> listOf(
            WarrantyTracking.NONE to R.string.detail_tracking_unarchive
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.detail_tracking_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = when {
                    current != null && since != null ->
                        stringResource(R.string.detail_tracking_since, current, since)
                    else -> stringResource(R.string.detail_tracking_none)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { (target, labelRes) ->
                    OutlinedButton(
                        onClick = { onChange(target) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(labelRes)) }
                }
            }
        }
    }
}
