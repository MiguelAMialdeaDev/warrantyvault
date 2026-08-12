package com.miguelcode.warrantyvault.feature.detail.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyStatus
import com.miguelcode.warrantyvault.feature.detail.R
import com.miguelcode.warrantyvault.ui.common.StatusBadge
import com.miguelcode.warrantyvault.ui.common.WarrantyProgressBar
import com.miguelcode.warrantyvault.ui.theme.*
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun StatusSection(
    warranty: Warranty,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val daysColor = when (warranty.status) {
        WarrantyStatus.ACTIVE -> if (isDark) StatusActiveTextDark else StatusActiveText
        WarrantyStatus.EXPIRING_SOON -> if (isDark) StatusExpiringTextDark else StatusExpiringText
        WarrantyStatus.EXPIRED -> if (isDark) StatusExpiredTextDark else StatusExpiredText
    }
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusBadge(status = warranty.status)
                Text(
                    text = if (warranty.daysRemaining >= 0) "${warranty.daysRemaining}" else "${abs(warranty.daysRemaining)}",
                    style = MaterialTheme.typography.displayLarge,
                    color = daysColor
                )
                Text(
                    text = stringResource(
                        if (warranty.daysRemaining >= 0) R.string.detail_days_remaining_label
                        else R.string.detail_days_expired_label
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            WarrantyProgressBar(
                progress = warranty.progressFraction,
                status = warranty.status,
                height = 10.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.detail_purchased_on, warranty.purchaseDate.format(fmt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                Text(
                    text = stringResource(R.string.detail_expires_on, warranty.warrantyEndDate.format(fmt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}
