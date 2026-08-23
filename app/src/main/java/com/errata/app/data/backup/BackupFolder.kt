package com.errata.app.data.backup

/**
 * User-chosen folder for a stable [FILE_NAME]. Tree URI is device-local
 * (SharedPreferences), never part of [ErrataBackup] JSON.
 */
object BackupFolder {
    const val FILE_NAME = "errata-backup.json"
    const val PREFS = "errata_backup_folder"
    const val KEY_URI = "tree_uri"

    fun persistableUriString(raw: String?): String? =
        raw?.trim()?.takeIf { it.isNotEmpty() }

    /** In-memory stand-in for prefs — persist / clear without Android. */
    class Memory {
        private var stored: String? = null

        fun get(): String? = persistableUriString(stored)

        fun set(raw: String?) {
            stored = persistableUriString(raw)
        }

        fun clear() {
            stored = null
        }
    }
}

class BackupFolderException(val code: String) : Exception(code)
