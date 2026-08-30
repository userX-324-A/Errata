package com.errata.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.ScheduleKind
import com.errata.app.domain.settings.AppearanceMode

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["uuid"], unique = true)],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Sync identity. Local [id] stays the alarm/nav key. */
    val uuid: String = "",
    val title: String,
    val notes: String? = null,
    val estimateMinutes: Int,
    val intervalDays: Int,
    val scheduleKind: ScheduleKind = ScheduleKind.INTERVAL,
    /** Bit 0 = Monday … bit 6 = Sunday. WEEKLY (one or more) or NTH_WEEKDAY (exactly one). */
    val weekdaysMask: Int = 0,
    /** 1–31 when monthly or yearly month-chips; otherwise 0. */
    val monthDay: Int = 0,
    /** 1–4 or 5 (last) when nth-weekday; otherwise 0. */
    val weekdayOrdinal: Int = 0,
    /** Bit 0 = January … bit 11 = December. Unused unless [scheduleKind] is YEARLY. */
    val yearMonthsMask: Int = 0,
    /** Bit 0 = Spring … bit 3 = Winter. Unused unless [scheduleKind] is YEARLY. */
    val seasonMask: Int = 0,
    val cadenceMode: CadenceMode,
    val anchorEpochDay: Long,
    val nextDueAtEpochMs: Long,
    val lastCompletedAtEpochMs: Long? = null,
    /** null = fire at the due clock time */
    val reminderMinutesOfDay: Int? = null,
    val snoozedUntilEpochMs: Long? = null,
    val area: String? = null,
    val isPaused: Boolean = false,
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "completions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId"), Index(value = ["uuid"], unique = true)],
)
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = "",
    val taskId: Long,
    val completedAtEpochMs: Long,
    val scheduledDueAtEpochMs: Long,
    val estimateMinutesAtCompletion: Int,
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val defaultCadenceMode: CadenceMode = CadenceMode.FROM_COMPLETION_CATCH_UP,
    val defaultReminderMinutesOfDay: Int = 9 * 60,
    val defaultWorkStartMinutesOfDay: Int? = null,
    val soonHorizonDays: Int = 7,
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val digestEnabled: Boolean = false,
    /** 0 = keep all; otherwise drop completions older than this many days except the newest 8 per task. */
    val historyRetentionDays: Int = 730,
    /** Last change to shared (synced) settings. Appearance does not bump this. */
    val updatedAtEpochMs: Long = 0,
    val historyGeneration: Int = 0,
    val historyPurgedAtEpochMs: Long = 0,
    val tasksGeneration: Int = 0,
    val tasksResetAtEpochMs: Long = 0,
)
