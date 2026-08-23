package com.errata.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Calm slate / stone — not gamified purple, not streak-orange.
private val Mist = Color(0xFFF4F6F5)
private val Ink = Color(0xFF1C2421)
private val Moss = Color(0xFF3D5A4C)
private val MossMuted = Color(0xFF6B8578)
private val SoftCard = Color(0xFFE8EEEC)
private val Night = Color(0xFF121816)
private val NightSurface = Color(0xFF1A221E)
private val NightAccent = Color(0xFF9BB5A6)

private val LightColors = lightColorScheme(
    primary = Moss,
    onPrimary = Color.White,
    secondary = MossMuted,
    onSecondary = Color.White,
    background = Mist,
    onBackground = Ink,
    surface = Mist,
    onSurface = Ink,
    surfaceVariant = SoftCard,
    onSurfaceVariant = MossMuted,
)

private val DarkColors = darkColorScheme(
    primary = NightAccent,
    onPrimary = Night,
    secondary = MossMuted,
    onSecondary = Color.White,
    background = Night,
    onBackground = Color(0xFFE6EDEA),
    surface = NightSurface,
    onSurface = Color(0xFFE6EDEA),
    surfaceVariant = Color(0xFF24302B),
    onSurfaceVariant = Color(0xFFA8B8B0),
)

@Composable
fun ErrataTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ErrataTypography,
        content = content,
    )
}
