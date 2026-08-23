package com.errata.app.ui.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.errata.app.data.TaskCommands
import com.errata.app.data.backup.BackupFolderException
import com.errata.app.data.backup.BackupFolderStore
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
    val hasFolder: Boolean = false,
    val folderLabel: String? = null,
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

    fun refreshFolder(context: Context) {
        viewModelScope.launch { applyFolderState(context) }
    }

    fun setFolder(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    BackupFolderStore(context).setTreeUri(uri)
                }
                applyFolderState(context)
                _uiState.update { it.copy(message = null, isError = false) }
            } catch (e: BackupFolderException) {
                _uiState.update { it.copy(message = e.code, isError = true) }
            } catch (_: Exception) {
                _uiState.update { it.copy(message = "folder_unavailable", isError = true) }
            }
        }
    }

    fun clearFolder(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                BackupFolderStore(context).clear()
            }
            applyFolderState(context)
            _uiState.update { it.copy(message = "folder_cleared", isError = false) }
        }
    }

    fun writeToFolder(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, message = null, isError = false) }
            try {
                val json = commands.exportJson()
                withContext(Dispatchers.IO) {
                    BackupFolderStore(context).writeJson(json)
                }
                _uiState.update {
                    it.copy(busy = false, message = "folder_written", isError = false)
                }
            } catch (e: BackupFolderException) {
                _uiState.update { it.copy(busy = false, message = e.code, isError = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(busy = false, message = e.message ?: "export_failed", isError = true)
                }
            }
        }
    }

    fun readFromFolder(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, message = null, isError = false) }
            try {
                val json = withContext(Dispatchers.IO) {
                    BackupFolderStore(context).readJson()
                }
                _uiState.update {
                    it.copy(busy = false, pendingImportJson = json, isError = false)
                }
            } catch (e: BackupFolderException) {
                _uiState.update { it.copy(busy = false, message = e.code, isError = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(busy = false, message = e.message ?: "import_failed", isError = true)
                }
            }
        }
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

    private suspend fun applyFolderState(context: Context) {
        val (hasFolder, label) = withContext(Dispatchers.IO) {
            val store = BackupFolderStore(context)
            (store.uri() != null) to store.displayName()
        }
        _uiState.update { it.copy(hasFolder = hasFolder, folderLabel = label) }
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
