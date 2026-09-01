package com.errata.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Margin-note identity: paper, ink, terracotta correction mark, quiet moss.

internal val Paper = Color(0xFFF4EFE6)
internal val Ink = Color(0xFF1C1914)
internal val Terracotta = Color(0xFFB85C38)
internal val TerracottaSoft = Color(0xFFE8C4B3)
internal val Moss = Color(0xFF3F5A4A)
internal val MossWash = Color(0xFFD5E0D8)
internal val Card = Color(0xFFEBE4D6)
internal val OutlineLight = Color(0xFFC9BFB0)

internal val NightPaper = Color(0xFF141210)
internal val NightInk = Color(0xFFEDE6DA)
internal val NightTerracotta = Color(0xFFE08A6A)
internal val NightTerracottaSoft = Color(0xFF5A3328)
internal val NightMoss = Color(0xFF9BB5A6)
internal val NightMossWash = Color(0xFF24332C)
internal val NightCard = Color(0xFF1F1C18)
internal val OutlineDark = Color(0xFF5A5348)

val LightColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color(0xFFFFF8F4),
    primaryContainer = TerracottaSoft,
    onPrimaryContainer = Color(0xFF4A1F12),
    inversePrimary = NightTerracotta,
    secondary = Moss,
    onSecondary = Color(0xFFF4F8F5),
    secondaryContainer = Moss,
    onSecondaryContainer = Color(0xFFF4F8F5),
    tertiary = Color(0xFF6B5344),
    onTertiary = Color(0xFFFFF8F4),
    tertiaryContainer = Color(0xFFE8D8CC),
    onTertiaryContainer = Color(0xFF2C1E16),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Card,
    onSurfaceVariant = Color(0xFF5C564C),
    surfaceTint = Terracotta,
    inverseSurface = NightCard,
    inverseOnSurface = NightInk,
    error = Color(0xFFA11B2B),
    onError = Color(0xFFFFF8F4),
    errorContainer = Color(0xFFF8D4D6),
    onErrorContainer = Color(0xFF3E0A12),
    outline = OutlineLight,
    outlineVariant = Color(0xFFDDD4C6),
    scrim = Color(0xFF1C1914),
    surfaceBright = Color(0xFFFBF7F0),
    surfaceDim = Card,
    surfaceContainerLowest = Color(0xFFFFFCF7),
    surfaceContainerLow = Color(0xFFF7F1E8),
    surfaceContainer = Card,
    surfaceContainerHigh = Color(0xFFE3DBCC),
    surfaceContainerHighest = Color(0xFFD8CFC0),
)

val DarkColors = darkColorScheme(
    primary = NightTerracotta,
    onPrimary = Color(0xFF3A160C),
    primaryContainer = NightTerracottaSoft,
    onPrimaryContainer = Color(0xFFF3D0C2),
    inversePrimary = Terracotta,
    secondary = NightMoss,
    onSecondary = Color(0xFF122018),
    secondaryContainer = NightMossWash,
    onSecondaryContainer = Color(0xFFC5D7CC),
    tertiary = Color(0xFFD4B8A6),
    onTertiary = Color(0xFF2C1E16),
    tertiaryContainer = Color(0xFF433328),
    onTertiaryContainer = Color(0xFFE8D8CC),
    background = NightPaper,
    onBackground = NightInk,
    surface = NightPaper,
    onSurface = NightInk,
    surfaceVariant = NightCard,
    onSurfaceVariant = Color(0xFFC4BBAE),
    surfaceTint = NightTerracotta,
    inverseSurface = Card,
    inverseOnSurface = Ink,
    error = Color(0xFFF0A8B0),
    onError = Color(0xFF3E0A12),
    errorContainer = Color(0xFF6F2A32),
    onErrorContainer = Color(0xFFF8D4D6),
    outline = OutlineDark,
    outlineVariant = Color(0xFF3E3932),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2C2823),
    surfaceDim = NightPaper,
    surfaceContainerLowest = Color(0xFF0E0C0A),
    surfaceContainerLow = Color(0xFF191714),
    surfaceContainer = NightCard,
    surfaceContainerHigh = Color(0xFF2A2621),
    surfaceContainerHighest = Color(0xFF35312B),
)
