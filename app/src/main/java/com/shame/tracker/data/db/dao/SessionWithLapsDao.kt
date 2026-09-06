package com.shame.tracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.shame.tracker.data.model.SessionWithLaps
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionWithLapsDao {
    @Transaction
    @Query("SELECT * FROM sessions ORDER BY start_time_ms DESC")
    fun observeAllWithLaps(): Flow<List<SessionWithLaps>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getWithLaps(sessionId: Long): SessionWithLaps?

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun observeWithLaps(sessionId: Long): Flow<SessionWithLaps?>
}
