package com.errata.app.ui.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.errata.app.data.TaskCommands
import com.errata.app.data.backup.BackupFormatException
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(
    val message: String? = null,
    val isError: Boolean = false,
    val busy: Boolean = false,
    val pendingImportJson: String? = null,
)

class BackupViewModel(
    private val commands: TaskCommands,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun suggestedExportFileName(): String {
        val day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return "errata-backup-$day.json"
    }

    fun writeExport(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, message = null, isError = false) }
            try {
                val json = commands.exportJson()
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(StandardCharsets.UTF_8))
                    } ?: error("Could not open file for writing")
                }
                _uiState.update {
                    it.copy(busy = false, message = "exported", isError = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(busy = false, message = e.message ?: "export_failed", isError = true)
                }
            }
        }
    }

    fun prepareImport(json: String) {
        _uiState.update {
            it.copy(pendingImportJson = json, message = null, isError = false)
        }
    }

    fun cancelImport() {
        _uiState.update { it.copy(pendingImportJson = null) }
    }

    fun confirmImport() {
        val json = _uiState.value.pendingImportJson ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(busy = true, pendingImportJson = null, message = null, isError = false)
            }
            try {
                commands.importJsonReplace(json)
                _uiState.update {
                    it.copy(busy = false, message = "imported", isError = false)
                }
            } catch (e: BackupFormatException) {
                _uiState.update {
                    it.copy(busy = false, message = e.message, isError = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(busy = false, message = e.message ?: "import_failed", isError = true)
                }
            }
        }
    }

    companion object {
        fun factory(commands: TaskCommands): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    BackupViewModel(commands) as T
            }
    }
}
