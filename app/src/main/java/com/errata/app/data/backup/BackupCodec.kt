package com.errata.app.data.backup

import com.errata.app.domain.sync.StableIds
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(backup: ErrataBackup): String = json.encodeToString(backup.normalized())

    fun decode(text: String): ErrataBackup {
        val backup = try {
            json.decodeFromString<ErrataBackup>(text)
        } catch (e: Exception) {
            throw BackupFormatException("Could not read backup file: ${e.message}")
        }
        if (backup.schemaVersion !in 1..BACKUP_SCHEMA_VERSION) {
            throw BackupFormatException(
                "Unsupported backup version ${backup.schemaVersion} (expected $BACKUP_SCHEMA_VERSION)",
            )
        }
        return backup.normalized()
    }
}

internal fun ErrataBackup.normalized(): ErrataBackup = copy(
    schemaVersion = BACKUP_SCHEMA_VERSION,
    tasks = tasks.map { task -> task.copy(uuid = StableIds.orNew(task.uuid)) },
    completions = completions.map { row -> row.copy(uuid = StableIds.orNew(row.uuid)) },
)
