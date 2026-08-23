package com.errata.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.ScheduleKind
import com.errata.app.domain.settings.AppearanceMode

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String? = null,
    val estimateMinutes: Int,
    val intervalDays: Int,
    val scheduleKind: ScheduleKind = ScheduleKind.INTERVAL,
    /** Bit 0 = Monday … bit 6 = Sunday. Unused unless [scheduleKind] is WEEKLY. */
    val weekdaysMask: Int = 0,
    /** 1–31 when monthly; otherwise 0. */
    val monthDay: Int = 0,
    val cadenceMode: CadenceMode,
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
    indices = [Index("taskId")],
)
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
)
