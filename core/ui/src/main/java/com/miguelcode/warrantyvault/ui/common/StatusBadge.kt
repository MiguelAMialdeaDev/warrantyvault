package com.miguelcode.warrantyvault.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.miguelcode.warrantyvault.domain.model.WarrantyStatus
import com.miguelcode.warrantyvault.ui.R
import com.miguelcode.warrantyvault.ui.theme.StatusActiveBg
import com.miguelcode.warrantyvault.ui.theme.StatusActiveBgDark
import com.miguelcode.warrantyvault.ui.theme.StatusActiveText
import com.miguelcode.warrantyvault.ui.theme.StatusActiveTextDark
import com.miguelcode.warrantyvault.ui.theme.StatusExpiredBg
import com.miguelcode.warrantyvault.ui.theme.StatusExpiredBgDark
import com.miguelcode.warrantyvault.ui.theme.StatusExpiredText
import com.miguelcode.warrantyvault.ui.theme.StatusExpiredTextDark
import com.miguelcode.warrantyvault.ui.theme.StatusExpiringBg
import com.miguelcode.warrantyvault.ui.theme.StatusExpiringBgDark
import com.miguelcode.warrantyvault.ui.theme.StatusExpiringText
import com.miguelcode.warrantyvault.ui.theme.StatusExpiringTextDark
import com.miguelcode.warrantyvault.ui.theme.WarrantyVaultTheme

@Composable
fun StatusBadge(
    status: WarrantyStatus,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val backgroundColor: Color
    val textColor: Color
    val label: String

    when (status) {
        WarrantyStatus.ACTIVE -> {
            backgroundColor = if (isDark) StatusActiveBgDark else StatusActiveBg
            textColor = if (isDark) StatusActiveTextDark else StatusActiveText
            label = stringResource(R.string.status_active)
        }
        WarrantyStatus.EXPIRING_SOON -> {
            backgroundColor = if (isDark) StatusExpiringBgDark else StatusExpiringBg
            textColor = if (isDark) StatusExpiringTextDark else StatusExpiringText
            label = stringResource(R.string.status_expiring)
        }
        WarrantyStatus.EXPIRED -> {
            backgroundColor = if (isDark) StatusExpiredBgDark else StatusExpiredBg
            textColor = if (isDark) StatusExpiredTextDark else StatusExpiredText
            label = stringResource(R.string.status_expired)
        }
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgeActivePreview() {
    WarrantyVaultTheme { StatusBadge(status = WarrantyStatus.ACTIVE) }
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgeExpiringPreview() {
    WarrantyVaultTheme { StatusBadge(status = WarrantyStatus.EXPIRING_SOON) }
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgeExpiredPreview() {
    WarrantyVaultTheme { StatusBadge(status = WarrantyStatus.EXPIRED) }
}
