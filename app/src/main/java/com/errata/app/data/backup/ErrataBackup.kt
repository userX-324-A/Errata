package com.errata.app.data.backup

import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.ScheduleKind
import com.errata.app.domain.settings.AppearanceMode
import kotlinx.serialization.Serializable

const val BACKUP_SCHEMA_VERSION = 2

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
    val appearanceMode: String = AppearanceMode.SYSTEM.name,
    val digestEnabled: Boolean = false,
    val historyRetentionDays: Int = 730,
    val updatedAtEpochMs: Long = 0,
    val historyGeneration: Int = 0,
    val historyPurgedAtEpochMs: Long = 0,
    val tasksGeneration: Int = 0,
    val tasksResetAtEpochMs: Long = 0,
)

@Serializable
data class TaskBackup(
    val id: Long,
    val uuid: String = "",
    val title: String,
    val notes: String? = null,
    val estimateMinutes: Int,
    val intervalDays: Int,
    val scheduleKind: String = ScheduleKind.INTERVAL.name,
    val weekdaysMask: Int = 0,
    val monthDay: Int = 0,
    val weekdayOrdinal: Int = 0,
    val yearMonthsMask: Int = 0,
    val seasonMask: Int = 0,
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
    val uuid: String = "",
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

fun parseAppearanceMode(value: String): AppearanceMode =
    try {
        AppearanceMode.valueOf(value)
    } catch (_: IllegalArgumentException) {
        AppearanceMode.SYSTEM
    }

fun parseScheduleKind(value: String): ScheduleKind =
    try {
        ScheduleKind.valueOf(value)
    } catch (_: IllegalArgumentException) {
        throw BackupFormatException("Unknown schedule kind: $value")
    }
