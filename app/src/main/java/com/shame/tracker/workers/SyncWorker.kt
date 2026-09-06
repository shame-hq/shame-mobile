package com.shame.tracker.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shame.tracker.data.repository.SessionRepository
import com.shame.tracker.network.SupabaseService
import com.shame.tracker.network.NetworkModule
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: SessionRepository,
    private val supabaseService: SupabaseService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sessions = repository.getUnsyncedSessions()
            val laps = repository.getUnsyncedLaps()
            val points = repository.getUnsyncedGpsPoints()

            if (sessions.isEmpty() && laps.isEmpty() && points.isEmpty()) {
                return@withContext Result.success()
            }

            val authHeader = "Bearer ${NetworkModule.SUPABASE_ANON_KEY}"
            val contentType = "application/json"
            val prefer = "return=minimal"

            // Upload Sessions
            if (sessions.isNotEmpty()) {
                val sessionPayload = sessions.map { s ->
                    mapOf(
                        "id" to s.id,
                        "name" to s.name,
                        "start_time_ms" to s.startTimeMs,
                        "end_time_ms" to s.endTimeMs,
                        "total_laps" to s.totalLaps,
                        "total_steps" to s.totalSteps,
                        "total_distance_meters" to s.totalDistanceMeters,
                        "total_duration_ms" to s.totalDurationMs
                    )
                }
                val r1 = supabaseService.uploadSessions(
                    NetworkModule.SUPABASE_ANON_KEY,
                    authHeader,
                    contentType,
                    prefer,
                    sessionPayload
                )
                if (!r1.isSuccessful) return@withContext Result.retry()
            }

            // Upload Laps
            if (laps.isNotEmpty()) {
                val lapsPayload = laps.map { l ->
                    mapOf(
                        "id" to l.id,
                        "session_id" to l.sessionId,
                        "lap_number" to l.lapNumber,
                        "start_time_ms" to l.startTimeMs,
                        "end_time_ms" to l.endTimeMs,
                        "duration_ms" to l.durationMs,
                        "steps" to l.steps,
                        "distance_meters" to l.distanceMeters
                    )
                }
                val r2 = supabaseService.uploadLaps(
                    NetworkModule.SUPABASE_ANON_KEY,
                    authHeader,
                    contentType,
                    prefer,
                    lapsPayload
                )
                if (!r2.isSuccessful) return@withContext Result.retry()
            }

            // Mark as synced locally
            sessions.forEach { s -> repository.markSessionSynced(s.id) }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
