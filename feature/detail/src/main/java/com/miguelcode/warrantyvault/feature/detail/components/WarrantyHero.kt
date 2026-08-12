package com.miguelcode.warrantyvault.feature.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory
import com.miguelcode.warrantyvault.feature.detail.R
import com.miguelcode.warrantyvault.ui.theme.*

@Composable
fun WarrantyHero(
    category: WarrantyCategory,
    photoPath: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = photoPath,
                contentDescription = stringResource(R.string.detail_photo_ticket),
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val (bg, iconColor, icon) = when (category) {
                WarrantyCategory.ELECTRONICS -> Triple(CategoryElectronicsBg, CategoryElectronicsIcon, Icons.Outlined.PhoneAndroid)
                WarrantyCategory.APPLIANCE -> Triple(CategoryApplianceBg, CategoryApplianceIcon, Icons.Outlined.Kitchen)
                WarrantyCategory.GAMING -> Triple(CategoryGamingBg, CategoryGamingIcon, Icons.Outlined.SportsEsports)
                WarrantyCategory.CLOTHING -> Triple(CategoryClothingBg, CategoryClothingIcon, Icons.Outlined.Checkroom)
                WarrantyCategory.OTHER -> Triple(CategoryOtherBg, CategoryOtherIcon, Icons.Outlined.Inventory2)
            }
            Box(
                modifier = Modifier.matchParentSize().background(bg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = iconColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}
