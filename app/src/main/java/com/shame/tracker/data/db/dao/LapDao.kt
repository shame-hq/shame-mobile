package com.shame.tracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shame.tracker.data.db.entity.Lap
import kotlinx.coroutines.flow.Flow

@Dao
interface LapDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lap: Lap): Long

    @Update
    suspend fun update(lap: Lap)

    @Query("SELECT * FROM laps WHERE session_id = :sessionId ORDER BY lap_number ASC")
    fun observeBySession(sessionId: Long): Flow<List<Lap>>

    @Query("SELECT * FROM laps WHERE session_id = :sessionId ORDER BY lap_number ASC")
    suspend fun getBySession(sessionId: Long): List<Lap>

    @Query("SELECT * FROM laps WHERE id = :id")
    suspend fun getById(id: Long): Lap?

    @Query("DELETE FROM laps WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Query("SELECT * FROM laps WHERE session_id = :sessionId AND end_time_ms IS NULL LIMIT 1")
    suspend fun getActiveLap(sessionId: Long): Lap?

    @Query("SELECT l.* FROM laps l INNER JOIN sessions s ON l.session_id = s.id WHERE s.synced = 0")
    suspend fun getUnsyncedLaps(): List<Lap>
}
