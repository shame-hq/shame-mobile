package com.shame.tracker.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "laps",
    foreignKeys = [ForeignKey(
        entity = Session::class,
        parentColumns = ["id"],
        childColumns = ["session_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("session_id")]
)
data class Lap(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "lap_number")
    val lapNumber: Int,
    @ColumnInfo(name = "start_time_ms")
    val startTimeMs: Long,
    @ColumnInfo(name = "end_time_ms")
    val endTimeMs: Long? = null,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0L,
    @ColumnInfo(name = "steps")
    val steps: Long = 0L,
    @ColumnInfo(name = "distance_meters")
    val distanceMeters: Double = 0.0,
    @ColumnInfo(name = "avg_pace_sec_per_km")
    val avgPaceSecPerKm: Double = 0.0,
    @ColumnInfo(name = "cadence_spm")
    val cadenceSpm: Double = 0.0,
    @ColumnInfo(name = "split_time_ms")
    val splitTimeMs: Long = 0L,
    @ColumnInfo(name = "calories_est")
    val caloriesEst: Int = 0,
    @ColumnInfo(name = "notes")
    val notes: String? = null
)
