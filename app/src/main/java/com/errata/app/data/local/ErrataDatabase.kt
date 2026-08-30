package com.errata.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskEntity::class,
        CompletionEntity::class,
        SettingsEntity::class,
    ],
    version = 9,
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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN weekdayOrdinal INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN yearMonthsMask INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN seasonMask INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN historyRetentionDays INTEGER NOT NULL DEFAULT 730",
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE completions ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN updatedAtEpochMs INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN historyGeneration INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN historyPurgedAtEpochMs INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN tasksGeneration INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN tasksResetAtEpochMs INTEGER NOT NULL DEFAULT 0",
                )
                fillUuids(db, "tasks")
                fillUuids(db, "completions")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tasks_uuid ON tasks(uuid)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_completions_uuid ON completions(uuid)",
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN defaultReminderKind TEXT NOT NULL DEFAULT 'WHEN_DUE'",
                )
            }
        }

        private fun fillUuids(db: SupportSQLiteDatabase, table: String) {
            db.query("SELECT id FROM $table").use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    db.execSQL(
                        "UPDATE $table SET uuid = ? WHERE id = ?",
                        arrayOf<Any>(UUID.randomUUID().toString(), id),
                    )
                }
            }
        }

        fun create(context: Context): ErrataDatabase =
            Room.databaseBuilder(context.applicationContext, ErrataDatabase::class.java, NAME)
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                )
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
