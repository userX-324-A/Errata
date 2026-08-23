package com.errata.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
