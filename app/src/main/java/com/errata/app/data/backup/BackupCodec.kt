package com.errata.app.data.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(backup: ErrataBackup): String = json.encodeToString(backup)

    fun decode(text: String): ErrataBackup {
        val backup = try {
            json.decodeFromString<ErrataBackup>(text)
        } catch (e: Exception) {
            throw BackupFormatException("Could not read backup file: ${e.message}")
        }
        if (backup.schemaVersion != BACKUP_SCHEMA_VERSION) {
            throw BackupFormatException(
                "Unsupported backup version ${backup.schemaVersion} (expected $BACKUP_SCHEMA_VERSION)",
            )
        }
        return backup
    }
}
