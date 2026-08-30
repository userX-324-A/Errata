package com.errata.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errata.app.domain.settings.AppearanceMode
import com.errata.app.ui.ErrataNavHost
import com.errata.app.ui.theme.ErrataTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                CoroutineScope(Dispatchers.IO).launch {
                    ErrataApp.instance.reminderScheduler.rescheduleAll()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            val settings by ErrataApp.instance.taskCommands.observeSettings
                .collectAsStateWithLifecycle(initialValue = null)
            val appearance = settings?.appearanceMode ?: AppearanceMode.SYSTEM
            val darkTheme = when (appearance) {
                AppearanceMode.LIGHT -> false
                AppearanceMode.DARK -> true
                AppearanceMode.SYSTEM -> isSystemInDarkTheme()
            }
            val barScrim = Color.Transparent.toArgb()
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
            ErrataTheme(appearance = appearance, darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ErrataNavHost()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (ErrataApp.instance.syncPreferences.isLinked()) {
            ErrataApp.instance.syncScheduler.requestNow()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
