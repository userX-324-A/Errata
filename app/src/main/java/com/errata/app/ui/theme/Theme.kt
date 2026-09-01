package com.errata.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.errata.app.domain.settings.AppearanceMode

@Composable
fun ErrataTheme(
    appearance: AppearanceMode = AppearanceMode.SYSTEM,
    darkTheme: Boolean = when (appearance) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
    },
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ErrataTypography,
        content = content,
    )
}

/** Selected task cards use a moss wash so terracotta Done/Snooze stay readable. Chip selected stays solid moss. */
@Composable
fun errataTaskCardContainer(selected: Boolean): Color {
    val scheme = MaterialTheme.colorScheme
    if (!selected) return scheme.surfaceContainer
    return if (scheme.background.luminance() > 0.5f) MossWash else NightMossWash
}
