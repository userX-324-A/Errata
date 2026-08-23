package com.errata.app.data.backup

import com.errata.app.domain.cadence.CadenceMode
import kotlinx.serialization.Serializable

const val BACKUP_SCHEMA_VERSION = 1

@Serializable
data class ErrataBackup(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val exportedAtEpochMs: Long,
    val settings: SettingsBackup,
    val tasks: List<TaskBackup>,
    val completions: List<CompletionBackup>,
)

@Serializable
data class SettingsBackup(
    val defaultCadenceMode: String,
    val defaultReminderMinutesOfDay: Int,
    val defaultWorkStartMinutesOfDay: Int? = null,
    val soonHorizonDays: Int,
)

@Serializable
data class TaskBackup(
    val id: Long,
    val title: String,
    val notes: String? = null,
    val estimateMinutes: Int,
    val intervalDays: Int,
    val cadenceMode: String,
    val anchorEpochDay: Long,
    val nextDueAtEpochMs: Long,
    val lastCompletedAtEpochMs: Long? = null,
    val reminderMinutesOfDay: Int? = null,
    val snoozedUntilEpochMs: Long? = null,
    val area: String? = null,
    val isPaused: Boolean = false,
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Serializable
data class CompletionBackup(
    val id: Long = 0,
    val taskId: Long,
    val completedAtEpochMs: Long,
    val scheduledDueAtEpochMs: Long,
    val estimateMinutesAtCompletion: Int,
)

class BackupFormatException(message: String) : Exception(message)

fun parseCadenceMode(value: String): CadenceMode =
    try {
        CadenceMode.valueOf(value)
    } catch (_: IllegalArgumentException) {
        throw BackupFormatException("Unknown cadence mode: $value")
    }
