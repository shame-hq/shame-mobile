package com.shame.tracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shame.tracker.data.db.entity.GpsPoint

@Dao
interface GpsPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: GpsPoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<GpsPoint>)

    @Query("SELECT * FROM gps_points WHERE session_id = :sessionId ORDER BY timestamp_ms ASC")
    suspend fun getBySession(sessionId: Long): List<GpsPoint>

    @Query("SELECT * FROM gps_points WHERE lap_id = :lapId ORDER BY timestamp_ms ASC")
    suspend fun getByLap(lapId: Long): List<GpsPoint>

    @Query("SELECT gp.* FROM gps_points gp INNER JOIN sessions s ON gp.session_id = s.id WHERE s.synced = 0")
    suspend fun getUnsyncedPoints(): List<GpsPoint>

    @Query("DELETE FROM gps_points WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: Long)
}
