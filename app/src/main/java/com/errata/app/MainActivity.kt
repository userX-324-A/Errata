package com.errata.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errata.app.domain.settings.AppearanceMode
import com.errata.app.ui.ErrataNavHost
import com.errata.app.ui.theme.ErrataTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearance by remember {
                ErrataApp.instance.taskCommands.observeSettings
                    .map { it?.appearanceMode ?: AppearanceMode.SYSTEM }
                    .distinctUntilChanged()
            }.collectAsStateWithLifecycle(initialValue = AppearanceMode.SYSTEM)
            val darkTheme = when (appearance) {
                AppearanceMode.LIGHT -> false
                AppearanceMode.DARK -> true
                AppearanceMode.SYSTEM -> isSystemInDarkTheme()
            }
            val barScrim = Color.Transparent.toArgb()
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(barScrim)
                    } else {
                        SystemBarStyle.light(barScrim, barScrim)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(barScrim)
                    } else {
                        SystemBarStyle.light(barScrim, barScrim)
                    },
                )
            }
            ErrataTheme(appearance = appearance, darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ErrataNavHost()
                }
            }
        }
    }
}
