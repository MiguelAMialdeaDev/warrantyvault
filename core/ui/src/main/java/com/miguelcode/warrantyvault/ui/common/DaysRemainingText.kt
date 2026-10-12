package com.miguelcode.warrantyvault.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.miguelcode.warrantyvault.domain.model.WarrantyStatus
import com.miguelcode.warrantyvault.ui.R
import com.miguelcode.warrantyvault.ui.theme.StatusActiveText
import com.miguelcode.warrantyvault.ui.theme.StatusActiveTextDark
import com.miguelcode.warrantyvault.ui.theme.StatusExpiredText
import com.miguelcode.warrantyvault.ui.theme.StatusExpiredTextDark
import com.miguelcode.warrantyvault.ui.theme.StatusExpiringText
import com.miguelcode.warrantyvault.ui.theme.StatusExpiringTextDark
import com.miguelcode.warrantyvault.ui.theme.WarrantyVaultTheme
import kotlin.math.abs
import com.miguelcode.warrantyvault.ui.theme.LocalIsDarkTheme

@Composable
fun DaysRemainingText(
    daysRemaining: Long,
    status: WarrantyStatus,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val color = when (status) {
        WarrantyStatus.ACTIVE -> if (isDark) StatusActiveTextDark else StatusActiveText
        WarrantyStatus.EXPIRING_SOON -> if (isDark) StatusExpiringTextDark else StatusExpiringText
        WarrantyStatus.EXPIRED -> if (isDark) StatusExpiredTextDark else StatusExpiredText
    }
    val absDays = abs(daysRemaining)
    val text = when {
        daysRemaining > 1 -> stringResource(R.string.days_remaining_other, daysRemaining)
        daysRemaining == 1L -> stringResource(R.string.days_remaining_one)
        daysRemaining == 0L -> stringResource(R.string.expires_today)
        daysRemaining == -1L -> stringResource(R.string.expired_yesterday)
        absDays < 30 -> stringResource(R.string.expired_days, absDays)
        absDays < 365 -> {
            val months = absDays / 30
            if (months == 1L) stringResource(R.string.expired_month_one)
            else stringResource(R.string.expired_months, months)
        }
        else -> {
            val years = absDays / 365
            if (years == 1L) stringResource(R.string.expired_year_one)
            else stringResource(R.string.expired_years, years)
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun DaysRemainingPreview() {
    WarrantyVaultTheme {
        DaysRemainingText(daysRemaining = 127, status = WarrantyStatus.ACTIVE)
    }
}
