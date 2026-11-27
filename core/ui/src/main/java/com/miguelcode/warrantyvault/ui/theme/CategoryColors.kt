package com.miguelcode.warrantyvault.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.miguelcode.warrantyvault.domain.model.WarrantyCategory

/** Fondo e icono de una categoría. */
data class CategoryColors(val background: Color, val icon: Color)

/**
 * Colores de cada categoría según el tema aplicado. Home y Detalle repetían la misma
 * tabla, y las dos usaban los fondos pastel del tema claro también en oscuro.
 */
@Composable
@ReadOnlyComposable
fun categoryColors(category: WarrantyCategory): CategoryColors {
    val dark = LocalIsDarkTheme.current
    return if (dark) {
        val icon = when (category) {
            WarrantyCategory.ELECTRONICS -> CategoryElectronicsIconDark
            WarrantyCategory.APPLIANCE -> CategoryApplianceIconDark
            WarrantyCategory.GAMING -> CategoryGamingIconDark
            WarrantyCategory.CLOTHING -> CategoryClothingIconDark
            WarrantyCategory.OTHER -> CategoryOtherIconDark
        }
        CategoryColors(background = icon.copy(alpha = DARK_BG_ALPHA), icon = icon)
    } else {
        when (category) {
            WarrantyCategory.ELECTRONICS -> CategoryColors(CategoryElectronicsBg, CategoryElectronicsIcon)
            WarrantyCategory.APPLIANCE -> CategoryColors(CategoryApplianceBg, CategoryApplianceIcon)
            WarrantyCategory.GAMING -> CategoryColors(CategoryGamingBg, CategoryGamingIcon)
            WarrantyCategory.CLOTHING -> CategoryColors(CategoryClothingBg, CategoryClothingIcon)
            WarrantyCategory.OTHER -> CategoryColors(CategoryOtherBg, CategoryOtherIcon)
        }
    }
}

private const val DARK_BG_ALPHA = 0.18f
