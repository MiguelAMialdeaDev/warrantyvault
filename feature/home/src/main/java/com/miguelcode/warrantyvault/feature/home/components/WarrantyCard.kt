package com.miguelcode.warrantyvault.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import coil.compose.AsyncImage
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
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.miguelcode.warrantyvault.feature.home.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.miguelcode.warrantyvault.domain.model.Warranty
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.domain.model.WarrantyStatus
import com.miguelcode.warrantyvault.ui.common.DaysRemainingText
import com.miguelcode.warrantyvault.ui.common.StatusBadge
import com.miguelcode.warrantyvault.ui.common.WarrantyProgressBar
import com.miguelcode.warrantyvault.ui.theme.WarrantyVaultTheme
import com.miguelcode.warrantyvault.ui.theme.categoryColors
import com.miguelcode.warrantyvault.ui.common.label
import java.time.LocalDate
import androidx.compose.ui.res.pluralStringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantyCard(
    warranty: Warranty,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusLabel = stringResource(
        when (warranty.status) {
            WarrantyStatus.ACTIVE -> R.string.home_a11y_active
            WarrantyStatus.EXPIRING_SOON -> R.string.home_a11y_expiring
            WarrantyStatus.EXPIRED -> R.string.home_a11y_expired
        }
    )
    val daysLabel = when {
        warranty.daysRemaining > 0 -> warranty.daysRemaining.toInt().let { pluralStringResource(R.plurals.home_a11y_days_remaining, it, it) }
        warranty.daysRemaining == 0L -> stringResource(R.string.home_a11y_expires_today)
        else -> (-warranty.daysRemaining).toInt().let { pluralStringResource(R.plurals.home_a11y_expired_days, it, it) }
    }
    val categoryName = warranty.category.displayName()
    val trackingLabel = warranty.tracking.label()
    val cardContentDescription = listOfNotNull(
        warranty.productName, categoryName, trackingLabel, statusLabel, daysLabel
    ).joinToString(", ")

    // clearAndSetSemantics borra el subárbol de a11y de los hijos para que TalkBack
    // no lea cada Text por separado además del contentDescription. Se restituye el
    // role de Button y el onClick semántico para que siga siendo enfocable y tocable.
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = cardContentDescription
                role = Role.Button
                onClick(label = null) { onClick(); true }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!warranty.receiptPhotoPath.isNullOrBlank()) {
                PhotoThumbnail(path = warranty.receiptPhotoPath!!)
            } else {
                CategoryCircle(category = warranty.category)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = warranty.productName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = warranty.status)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        if (warranty.store.isNotBlank()) {
                            append(warranty.store)
                            append(" · ")
                        }
                        append(categoryName)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (trackingLabel != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = trackingLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WarrantyProgressBar(
                        progress = warranty.progressFraction,
                        status = warranty.status,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    DaysRemainingText(
                        daysRemaining = warranty.daysRemaining,
                        status = warranty.status
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(
    path: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = java.io.File(path),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun CategoryCircle(
    category: WarrantyCategory,
    modifier: Modifier = Modifier
) {
    val colors = categoryColors(category)
    val bgColor = colors.background
    val iconColor = colors.icon
    val icon: ImageVector = when (category) {
        WarrantyCategory.ELECTRONICS -> Icons.Outlined.PhoneAndroid
        WarrantyCategory.APPLIANCE -> Icons.Outlined.Kitchen
        WarrantyCategory.GAMING -> Icons.Outlined.SportsEsports
        WarrantyCategory.CLOTHING -> Icons.Outlined.Checkroom
        WarrantyCategory.OTHER -> Icons.Outlined.Inventory2
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .background(bgColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category.displayName(),
            modifier = Modifier.size(24.dp),
            tint = iconColor
        )
    }
}

@Composable
private fun WarrantyCategory.displayName(): String = stringResource(
    when (this) {
        WarrantyCategory.ELECTRONICS -> R.string.category_electronics
        WarrantyCategory.APPLIANCE -> R.string.category_appliance
        WarrantyCategory.GAMING -> R.string.category_gaming
        WarrantyCategory.CLOTHING -> R.string.category_clothing
        WarrantyCategory.OTHER -> R.string.category_other
    }
)

@Preview(showBackground = true)
@Composable
private fun WarrantyCardPreview() {
    WarrantyVaultTheme {
        WarrantyCard(
            warranty = Warranty(
                id = 1,
                productName = "MacBook Pro 14\"",
                category = WarrantyCategory.ELECTRONICS,
                store = "Apple Store",
                price = 2499.0,
                purchaseDate = LocalDate.now().minusMonths(6),
                warrantyEndDate = LocalDate.now().plusMonths(18)
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WarrantyCardExpiringPreview() {
    WarrantyVaultTheme {
        WarrantyCard(
            warranty = Warranty(
                id = 2,
                productName = "PlayStation 5",
                category = WarrantyCategory.GAMING,
                store = "MediaMarkt",
                purchaseDate = LocalDate.now().minusYears(1).minusMonths(11),
                warrantyEndDate = LocalDate.now().plusDays(15)
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
