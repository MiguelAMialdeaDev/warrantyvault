package com.miguelcode.warrantyvault.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme
val Primary = Color(0xFF4361EE)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFDBE1FF)
val OnPrimaryContainer = Color(0xFF000F5C)

val Secondary = Color(0xFF5B5D72)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFE1E0F9)
val OnSecondaryContainer = Color(0xFF181A2C)

val Background = Color(0xFFFAFBFF)
val OnBackground = Color(0xFF1A1B1E)
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF1A1B1E)
val SurfaceVariant = Color(0xFFE4E2EC)
val OnSurfaceVariant = Color(0xFF46464F)

val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF410002)

// Ámbar para Premium: el morado por defecto de Material no pegaba con nada.
val Tertiary = Color(0xFF7A5900)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFFDEA6)
val OnTertiaryContainer = Color(0xFF261900)

val Outline = Color(0xFF777680)
val OutlineVariant = Color(0xFFC7C5D0)
val SurfaceContainerLow = Color(0xFFF4F3FA)
val SurfaceContainer = Color(0xFFEEEDF4)
val SurfaceContainerHigh = Color(0xFFE8E7EF)

// Dark Theme
val PrimaryDark = Color(0xFFB4C4FF)
val OnPrimaryDark = Color(0xFF001687)
val PrimaryContainerDark = Color(0xFF243697)
val OnPrimaryContainerDark = Color(0xFFDBE1FF)

val BackgroundDark = Color(0xFF121316)
val OnBackgroundDark = Color(0xFFE3E2E6)
val SurfaceDark = Color(0xFF1A1B1E)
val OnSurfaceDark = Color(0xFFE3E2E6)
val SurfaceVariantDark = Color(0xFF46464F)
val OnSurfaceVariantDark = Color(0xFFC7C5D0)

val SecondaryDark = Color(0xFFC5C4DD)
val OnSecondaryDark = Color(0xFF2D2F42)
val SecondaryContainerDark = Color(0xFF434559)
val OnSecondaryContainerDark = Color(0xFFE1E0F9)

val TertiaryDark = Color(0xFFF5BF48)
val OnTertiaryDark = Color(0xFF402D00)
val TertiaryContainerDark = Color(0xFF5C4300)
val OnTertiaryContainerDark = Color(0xFFFFDEA6)

// El rojo del tema claro sobre fondo oscuro no llegaba al contraste mínimo.
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val OutlineDark = Color(0xFF918F9A)
val OutlineVariantDark = Color(0xFF46464F)
val SurfaceContainerLowDark = Color(0xFF1A1B1E)
val SurfaceContainerDark = Color(0xFF1F1F23)
val SurfaceContainerHighDark = Color(0xFF292A2D)

// Status Colors
val StatusActiveText = Color(0xFF1B6B3A)
val StatusActiveBg = Color(0xFFE8F5E9)
val StatusActiveTextDark = Color(0xFF4CAF50)
val StatusActiveBgDark = Color(0x4D1B5E20)

val StatusExpiringText = Color(0xFFC84B00)
val StatusExpiringBg = Color(0xFFFFF3E0)
val StatusExpiringTextDark = Color(0xFFFFB74D)
val StatusExpiringBgDark = Color(0x33E65100)

val StatusExpiredText = Color(0xFFBA1A1A)
val StatusExpiredBg = Color(0xFFFDECEA)
val StatusExpiredTextDark = Color(0xFFCF6679)
val StatusExpiredBgDark = Color(0x33BA1A1A)

// Category Colors
val CategoryElectronicsBg = Color(0xFFE3F2FD)
val CategoryElectronicsIcon = Color(0xFF1565C0)
val CategoryApplianceBg = Color(0xFFF3E5F5)
val CategoryApplianceIcon = Color(0xFF6A1B9A)
val CategoryGamingBg = Color(0xFFE8F5E9)
val CategoryGamingIcon = Color(0xFF2E7D32)
val CategoryClothingBg = Color(0xFFFFF8E1)
val CategoryClothingIcon = Color(0xFFF57F17)
val CategoryOtherBg = Color(0xFFEFEBE9)
val CategoryOtherIcon = Color(0xFF4E342E)

// En oscuro, los fondos pastel del tema claro brillaban sobre la superficie oscura:
// fondo tonal del propio color del icono y el icono en un tono claro.
val CategoryElectronicsIconDark = Color(0xFF90CAF9)
val CategoryApplianceIconDark = Color(0xFFCE93D8)
val CategoryGamingIconDark = Color(0xFFA5D6A7)
val CategoryClothingIconDark = Color(0xFFFFE082)
val CategoryOtherIconDark = Color(0xFFBCAAA4)
