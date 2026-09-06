package com.shame.tracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "start_time_ms")
    val startTimeMs: Long,
    @ColumnInfo(name = "end_time_ms")
    val endTimeMs: Long? = null,
    @ColumnInfo(name = "total_laps")
    val totalLaps: Int = 0,
    @ColumnInfo(name = "total_steps")
    val totalSteps: Long = 0L,
    @ColumnInfo(name = "total_distance_meters")
    val totalDistanceMeters: Double = 0.0,
    @ColumnInfo(name = "total_duration_ms")
    val totalDurationMs: Long = 0L,
    @ColumnInfo(name = "start_lat")
    val startLat: Double? = null,
    @ColumnInfo(name = "start_lng")
    val startLng: Double? = null,
    @ColumnInfo(name = "synced")
    val synced: Boolean = false
)
