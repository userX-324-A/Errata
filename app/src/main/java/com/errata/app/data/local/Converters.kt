package com.errata.app.data.local

import androidx.room.TypeConverter
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.ScheduleKind
import com.errata.app.domain.reminders.DefaultReminderKind
import com.errata.app.domain.settings.AppearanceMode

class Converters {
    @TypeConverter
    fun cadenceModeToString(mode: CadenceMode): String = mode.name

    @TypeConverter
    fun stringToCadenceMode(value: String): CadenceMode = CadenceMode.valueOf(value)

    @TypeConverter
    fun scheduleKindToString(kind: ScheduleKind): String = kind.name

    @TypeConverter
    fun stringToScheduleKind(value: String): ScheduleKind =
        try {
            ScheduleKind.valueOf(value)
        } catch (_: IllegalArgumentException) {
            ScheduleKind.INTERVAL
        }

    @TypeConverter
    fun appearanceModeToString(mode: AppearanceMode): String = mode.name

    @TypeConverter
    fun stringToAppearanceMode(value: String): AppearanceMode =
        try {
            AppearanceMode.valueOf(value)
        } catch (_: IllegalArgumentException) {
            AppearanceMode.SYSTEM
        }

    @TypeConverter
    fun defaultReminderKindToString(kind: DefaultReminderKind): String = kind.name

    @TypeConverter
    fun stringToDefaultReminderKind(value: String): DefaultReminderKind =
        try {
            DefaultReminderKind.valueOf(value)
        } catch (_: IllegalArgumentException) {
            DefaultReminderKind.WHEN_DUE
        }
}
