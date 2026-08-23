package com.errata.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskEntity::class,
        CompletionEntity::class,
        SettingsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ErrataDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun completionDao(): CompletionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val NAME = "errata.db"

        fun create(context: Context): ErrataDatabase =
            Room.databaseBuilder(context.applicationContext, ErrataDatabase::class.java, NAME)
                .addCallback(
                    object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed defaults via DAO after open — see ensureSettings.
                        }
                    },
                )
                .build()
                .also { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        database.ensureSettings()
                    }
                }
    }

    suspend fun ensureSettings() {
        if (settingsDao().get() == null) {
            settingsDao().upsert(SettingsEntity())
        }
    }
}
