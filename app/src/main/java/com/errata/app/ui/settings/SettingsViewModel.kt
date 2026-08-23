package com.errata.app.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.errata.app.data.TaskCommands
import com.errata.app.data.local.SettingsEntity
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.history.HistoryRetention
import com.errata.app.domain.settings.AppearanceMode
import com.errata.app.sync.GoogleAuth
import com.errata.app.sync.GoogleLinkResult
import com.errata.app.sync.SyncCoordinator
import com.errata.app.sync.SyncPreferences
import com.errata.app.sync.SyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loaded: Boolean = false,
    val defaultCadenceMode: CadenceMode = CadenceMode.FROM_COMPLETION_CATCH_UP,
    val defaultReminderMinutesOfDay: Int = 9 * 60,
    val defaultWorkStartMinutesOfDay: Int? = null,
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val digestEnabled: Boolean = false,
    val historyRetentionDays: Int = HistoryRetention.DEFAULT_DAYS,
    val googleConfigured: Boolean = false,
    val playServices: Boolean = false,
    val googleLinked: Boolean = false,
    val googleEmail: String? = null,
    val lastSyncEpochMs: Long = 0,
    val lastSyncError: String? = null,
)

class SettingsViewModel(
    private val commands: TaskCommands,
    private val syncPrefs: SyncPreferences,
    private val syncScheduler: SyncScheduler,
    private val coordinator: SyncCoordinator,
    private val playServicesAvailable: Boolean,
) : ViewModel() {

    private var pendingLinkEmail: String? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        commands.observeSettings,
        syncPrefs.state,
    ) { entity, sync ->
        val s = entity ?: SettingsEntity()
        SettingsUiState(
            loaded = true,
            defaultCadenceMode = s.defaultCadenceMode,
            defaultReminderMinutesOfDay = s.defaultReminderMinutesOfDay,
            defaultWorkStartMinutesOfDay = s.defaultWorkStartMinutesOfDay,
            appearanceMode = s.appearanceMode,
            digestEnabled = s.digestEnabled,
            historyRetentionDays = s.historyRetentionDays,
            googleConfigured = GoogleAuth.isConfigured(),
            playServices = playServicesAvailable,
            googleLinked = sync.linked,
            googleEmail = sync.email,
            lastSyncEpochMs = sync.lastSyncEpochMs,
            lastSyncError = sync.lastError,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(
            googleConfigured = GoogleAuth.isConfigured(),
            playServices = playServicesAvailable,
        ),
    )

    fun setDefaultReminderMinutes(minutes: Int) {
        persist { it.copy(defaultReminderMinutesOfDay = minutes) }
    }

    fun setDefaultCadenceMode(mode: CadenceMode) {
        persist { it.copy(defaultCadenceMode = mode) }
    }

    fun setWorkStartMinutes(minutes: Int?) {
        persist { it.copy(defaultWorkStartMinutesOfDay = minutes) }
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        persist { it.copy(appearanceMode = mode) }
    }

    fun setDigestEnabled(enabled: Boolean) {
        persist { it.copy(digestEnabled = enabled) }
    }

    fun setHistoryRetentionDays(days: Int) {
        persist { it.copy(historyRetentionDays = days) }
    }

    fun purgeHistory() {
        viewModelScope.launch { commands.purgeHistory() }
    }

    fun resetTasks(alsoClearCloud: Boolean) {
        viewModelScope.launch {
            commands.resetTasks(alsoClearCloud)
            if (syncPrefs.isLinked()) {
                syncScheduler.requestNow()
            }
        }
    }

    fun rescheduleReminders() {
        viewModelScope.launch { commands.rescheduleReminders() }
    }

    fun syncNow() {
        viewModelScope.launch { syncScheduler.requestNow() }
    }

    fun linkGoogle(activity: Activity, onNeedsConsent: (IntentSender) -> Unit) {
        viewModelScope.launch {
            when (val result = GoogleAuth.beginLink(activity)) {
                is GoogleLinkResult.Linked -> onLinked(result.email)
                is GoogleLinkResult.NeedsConsent -> {
                    pendingLinkEmail = result.email
                    onNeedsConsent(result.sender)
                }
                GoogleLinkResult.Cancelled -> Unit
                is GoogleLinkResult.Failed -> syncPrefs.markError(result.reason)
            }
        }
    }

    fun completeGoogleConsent(activity: Activity, data: Intent?) {
        val email = pendingLinkEmail ?: return
        viewModelScope.launch {
            when (val result = GoogleAuth.completeLinkFromIntent(activity, email, data)) {
                is GoogleLinkResult.Linked -> onLinked(result.email)
                is GoogleLinkResult.NeedsConsent -> syncPrefs.markError("auth")
                GoogleLinkResult.Cancelled -> Unit
                is GoogleLinkResult.Failed -> syncPrefs.markError(result.reason)
            }
            pendingLinkEmail = null
        }
    }

    fun unlink(context: Context, wipeCloud: Boolean) {
        viewModelScope.launch {
            if (wipeCloud) {
                coordinator.wipeCloud()
            }
            syncScheduler.cancelAll()
            GoogleAuth.clearCredential(context)
            syncPrefs.clearLink()
        }
    }

    private fun onLinked(email: String) {
        syncPrefs.markLinked(email)
        syncScheduler.ensurePeriodic()
        syncScheduler.requestNow()
    }

    private fun persist(transform: (SettingsEntity) -> SettingsEntity) {
        viewModelScope.launch {
            val current = commands.getSettings()
            commands.updateSettings(transform(current))
        }
    }

    companion object {
        fun factory(
            commands: TaskCommands,
            syncPrefs: SyncPreferences,
            syncScheduler: SyncScheduler,
            coordinator: SyncCoordinator,
            playServicesAvailable: Boolean,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        commands,
                        syncPrefs,
                        syncScheduler,
                        coordinator,
                        playServicesAvailable,
                    ) as T
                }
            }
    }
}
