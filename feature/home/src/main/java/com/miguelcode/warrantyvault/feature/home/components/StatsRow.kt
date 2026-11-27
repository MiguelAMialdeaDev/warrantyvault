package com.miguelcode.warrantyvault.feature.home.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.miguelcode.warrantyvault.feature.home.R
import com.miguelcode.warrantyvault.ui.theme.StatusActiveText
import com.miguelcode.warrantyvault.ui.theme.StatusActiveTextDark
import com.miguelcode.warrantyvault.ui.theme.StatusExpiredText
import com.miguelcode.warrantyvault.ui.theme.StatusExpiredTextDark
import com.miguelcode.warrantyvault.ui.theme.StatusExpiringText
import com.miguelcode.warrantyvault.ui.theme.StatusExpiringTextDark
import com.miguelcode.warrantyvault.ui.theme.WarrantyVaultTheme
import com.miguelcode.warrantyvault.ui.theme.LocalIsDarkTheme

@Composable
fun StatsRow(
    activeCount: Int,
    expiringCount: Int,
    expiredCount: Int,
    modifier: Modifier = Modifier,
    onActiveClick: () -> Unit = {},
    onExpiringClick: () -> Unit = {},
    onExpiredClick: () -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            count = activeCount,
            label = stringResource(R.string.home_stat_active),
            color = if (isDark) StatusActiveTextDark else StatusActiveText,
            onClick = onActiveClick,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            count = expiringCount,
            label = stringResource(R.string.home_stat_expiring),
            color = if (isDark) StatusExpiringTextDark else StatusExpiringText,
            onClick = onExpiringClick,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            count = expiredCount,
            label = stringResource(R.string.home_stat_expired),
            color = if (isDark) StatusExpiredTextDark else StatusExpiredText,
            onClick = onExpiredClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    count: Int,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedCount by animateIntAsState(
        targetValue = count,
        animationSpec = tween(durationMillis = 400),
        label = "statCount"
    )

    // Cada contador filtra la lista por su estado: era lo que la gente intentaba tocar.
    Card(
        onClick = onClick,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "$count $label"
            role = Role.Button
            onClick { onClick(); true }
        },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$animatedCount",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsRowPreview() {
    WarrantyVaultTheme {
        StatsRow(activeCount = 5, expiringCount = 2, expiredCount = 1)
    }
}
