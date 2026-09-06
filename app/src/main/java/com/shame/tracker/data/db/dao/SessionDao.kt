package com.shame.tracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shame.tracker.data.db.entity.Session
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Query("SELECT * FROM sessions ORDER BY start_time_ms DESC")
    fun observeAll(): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY start_time_ms DESC")
    suspend fun getAll(): List<Session>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): Session?

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeById(id: Long): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE synced = 0")
    suspend fun getUnsynced(): List<Session>

    @Query("UPDATE sessions SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM sessions WHERE end_time_ms IS NOT NULL ORDER BY start_time_ms DESC LIMIT 1")
    suspend fun getLatestCompleted(): Session?

    @Query("SELECT MIN(l.duration_ms) FROM laps l INNER JOIN sessions s ON l.session_id = s.id WHERE s.end_time_ms IS NOT NULL AND l.end_time_ms IS NOT NULL")
    suspend fun getFastestLapMs(): Long?

    @Query("SELECT * FROM sessions WHERE end_time_ms IS NOT NULL ORDER BY total_laps DESC LIMIT 1")
    suspend fun getMostLapsSession(): Session?

    @Query("SELECT * FROM sessions WHERE end_time_ms IS NOT NULL ORDER BY total_distance_meters DESC LIMIT 1")
    suspend fun getLongestDistanceSession(): Session?

    @Query("SELECT * FROM sessions WHERE end_time_ms IS NOT NULL AND total_distance_meters > 0 ORDER BY (total_duration_ms / total_distance_meters) ASC LIMIT 1")
    suspend fun getBestPaceSession(): Session?

    @Query("SELECT COUNT(*) FROM sessions WHERE end_time_ms IS NOT NULL")
    suspend fun getCompletedCount(): Int

    @Query("SELECT MAX(start_time_ms) FROM sessions WHERE end_time_ms IS NOT NULL")
    suspend fun getLastRunTimeMs(): Long?
}
