package com.errata.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.errata.app.data.TaskCommands
import com.errata.app.data.local.SettingsEntity
import com.errata.app.domain.cadence.CadenceMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loaded: Boolean = false,
    val defaultCadenceMode: CadenceMode = CadenceMode.FROM_COMPLETION_CATCH_UP,
    val defaultReminderMinutesOfDay: Int = 9 * 60,
    val defaultWorkStartMinutesOfDay: Int? = null,
)

class SettingsViewModel(
    private val commands: TaskCommands,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = commands.observeSettings
        .map { entity ->
            val s = entity ?: SettingsEntity()
            SettingsUiState(
                loaded = true,
                defaultCadenceMode = s.defaultCadenceMode,
                defaultReminderMinutesOfDay = s.defaultReminderMinutesOfDay,
                defaultWorkStartMinutesOfDay = s.defaultWorkStartMinutesOfDay,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsUiState(),
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

    private fun persist(transform: (SettingsEntity) -> SettingsEntity) {
        viewModelScope.launch {
            val current = commands.getSettings()
            commands.updateSettings(transform(current))
        }
    }

    companion object {
        fun factory(commands: TaskCommands): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(commands) as T
                }
            }
    }
}
