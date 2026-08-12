package com.miguelcode.warrantyvault.feature.addedit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory

private data class CategoryOption(
    val category: WarrantyCategory,
    val label: String,
    val icon: ImageVector
)

private val categories = listOf(
    CategoryOption(WarrantyCategory.ELECTRONICS, "Electrónica", Icons.Outlined.PhoneAndroid),
    CategoryOption(WarrantyCategory.APPLIANCE, "Electrodoméstico", Icons.Outlined.Kitchen),
    CategoryOption(WarrantyCategory.GAMING, "Gaming", Icons.Outlined.SportsEsports),
    CategoryOption(WarrantyCategory.CLOTHING, "Ropa", Icons.Outlined.Checkroom),
    CategoryOption(WarrantyCategory.OTHER, "Otro", Icons.Outlined.Inventory2),
)

@Composable
fun CategoryChips(
    selected: WarrantyCategory,
    onCategorySelected: (WarrantyCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { option ->
            FilterChip(
                selected = option.category == selected,
                onClick = { onCategorySelected(option.category) },
                label = { Text(option.label) },
                leadingIcon = {
                    Icon(option.icon, contentDescription = null, modifier = Modifier)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
