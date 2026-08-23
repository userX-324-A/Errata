package com.errata.app.data.local

import androidx.room.TypeConverter
import com.errata.app.domain.cadence.CadenceMode

class Converters {
    @TypeConverter
    fun cadenceModeToString(mode: CadenceMode): String = mode.name

    @TypeConverter
    fun stringToCadenceMode(value: String): CadenceMode = CadenceMode.valueOf(value)
}
