package com.errata.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks
        WHERE isArchived = 0
        ORDER BY nextDueAtEpochMs ASC, id ASC
        """,
    )
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?

    @Query(
        """
        SELECT * FROM tasks
        WHERE isArchived = 0 AND isPaused = 0
        """,
    )
    suspend fun listSchedulable(): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY id ASC")
    suspend fun listAll(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}

@Dao
interface CompletionDao {
    @Insert
    suspend fun insert(completion: CompletionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(completions: List<CompletionEntity>)

    @Query("SELECT * FROM completions WHERE taskId = :taskId ORDER BY completedAtEpochMs DESC")
    suspend fun forTask(taskId: Long): List<CompletionEntity>

    @Query("SELECT * FROM completions ORDER BY id ASC")
    suspend fun listAll(): List<CompletionEntity>

    @Query("DELETE FROM completions")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun get(): SettingsEntity?

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)

    @Query("DELETE FROM settings")
    suspend fun deleteAll()
}
