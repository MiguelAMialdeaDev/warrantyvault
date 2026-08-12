package com.miguelcode.warrantyvault.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.miguelcode.warrantyvault.domain.model.WarrantyStatus
import com.miguelcode.warrantyvault.ui.theme.StatusActiveText
import com.miguelcode.warrantyvault.ui.theme.StatusActiveTextDark
import com.miguelcode.warrantyvault.ui.theme.StatusExpiredText
import com.miguelcode.warrantyvault.ui.theme.StatusExpiredTextDark
import com.miguelcode.warrantyvault.ui.theme.StatusExpiringText
import com.miguelcode.warrantyvault.ui.theme.StatusExpiringTextDark
import com.miguelcode.warrantyvault.ui.theme.WarrantyVaultTheme

@Composable
fun WarrantyProgressBar(
    progress: Float,
    status: WarrantyStatus,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp
) {
    val isDark = isSystemInDarkTheme()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress"
    )
    val color = when (status) {
        WarrantyStatus.ACTIVE -> if (isDark) StatusActiveTextDark else StatusActiveText
        WarrantyStatus.EXPIRING_SOON -> if (isDark) StatusExpiringTextDark else StatusExpiringText
        WarrantyStatus.EXPIRED -> if (isDark) StatusExpiredTextDark else StatusExpiredText
    }
    val shape = RoundedCornerShape(height / 2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(shape)
                .background(color)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressBarPreview() {
    WarrantyVaultTheme {
        WarrantyProgressBar(progress = 0.6f, status = WarrantyStatus.ACTIVE)
    }
}
