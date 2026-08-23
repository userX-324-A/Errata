package com.errata.app.domain.sync

import kotlinx.serialization.Serializable

const val SYNC_SCHEMA_VERSION = 1

@Serializable
data class SyncSnapshot(
    val schemaVersion: Int = SYNC_SCHEMA_VERSION,
    val writtenAtEpochMs: Long = 0,
    val tasksGeneration: Int = 0,
    val tasksResetAtEpochMs: Long = 0,
    val historyGeneration: Int = 0,
    val historyPurgedAtEpochMs: Long = 0,
    val settings: SyncSettings = SyncSettings(),
    val tasks: List<SyncTask> = emptyList(),
    val completions: List<SyncCompletion> = emptyList(),
)

@Serializable
data class SyncSettings(
    val updatedAtEpochMs: Long = 0,
    val defaultCadenceMode: String = "FROM_COMPLETION_CATCH_UP",
    val defaultReminderMinutesOfDay: Int = 9 * 60,
    val defaultWorkStartMinutesOfDay: Int? = null,
    val soonHorizonDays: Int = 7,
    val digestEnabled: Boolean = false,
    val historyRetentionDays: Int = 730,
)

@Serializable
data class SyncTask(
    val uuid: String,
    val title: String,
    val notes: String? = null,
    val estimateMinutes: Int,
    val intervalDays: Int,
    val scheduleKind: String,
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
data class SyncCompletion(
    val uuid: String,
    val taskUuid: String,
    val completedAtEpochMs: Long,
    val scheduledDueAtEpochMs: Long,
    val estimateMinutesAtCompletion: Int,
)
