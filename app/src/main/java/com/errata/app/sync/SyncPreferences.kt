package com.errata.app.sync

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SyncPrefState(
    val linked: Boolean = false,
    val email: String? = null,
    val fileId: String? = null,
    val lastSyncEpochMs: Long = 0,
    val lastError: String? = null,
)

class SyncPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<SyncPrefState> = _state.asStateFlow()

    fun snapshot(): SyncPrefState = _state.value

    fun isLinked(): Boolean = _state.value.linked

    fun markLinked(email: String) {
        prefs.edit()
            .putBoolean(KEY_LINKED, true)
            .putString(KEY_EMAIL, email)
            .remove(KEY_ERROR)
            .apply()
        publish()
    }

    fun setFileId(fileId: String?) {
        prefs.edit().putString(KEY_FILE_ID, fileId).apply()
        publish()
    }

    fun markSynced(nowEpochMs: Long) {
        prefs.edit()
            .putLong(KEY_LAST_SYNC, nowEpochMs)
            .remove(KEY_ERROR)
            .apply()
        publish()
    }

    fun markError(code: String) {
        prefs.edit().putString(KEY_ERROR, code).apply()
        publish()
    }

    fun clearLink() {
        prefs.edit().clear().apply()
        publish()
    }

    private fun read(): SyncPrefState = SyncPrefState(
        linked = prefs.getBoolean(KEY_LINKED, false),
        email = prefs.getString(KEY_EMAIL, null),
        fileId = prefs.getString(KEY_FILE_ID, null),
        lastSyncEpochMs = prefs.getLong(KEY_LAST_SYNC, 0),
        lastError = prefs.getString(KEY_ERROR, null),
    )

    private fun publish() {
        _state.value = read()
    }

    private companion object {
        const val PREFS = "errata_google_sync"
        const val KEY_LINKED = "linked"
        const val KEY_EMAIL = "email"
        const val KEY_FILE_ID = "file_id"
        const val KEY_LAST_SYNC = "last_sync"
        const val KEY_ERROR = "last_error"
    }
}
