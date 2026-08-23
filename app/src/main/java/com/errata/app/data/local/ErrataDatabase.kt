package com.errata.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
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
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ErrataDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun completionDao(): CompletionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val NAME = "errata.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN appearanceMode TEXT NOT NULL DEFAULT 'SYSTEM'",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN digestEnabled INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN scheduleKind TEXT NOT NULL DEFAULT 'INTERVAL'",
                )
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN weekdaysMask INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN monthDay INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        fun create(context: Context): ErrataDatabase =
            Room.databaseBuilder(context.applicationContext, ErrataDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
