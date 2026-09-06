package com.shame.tracker.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shame.tracker.data.repository.SessionRepository
import com.shame.tracker.network.NetworkModule
import com.shame.tracker.network.SupabaseService
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

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sessions = repository.getUnsyncedSessions()

            if (sessions.isEmpty()) {
                Log.d(TAG, "Nothing to sync.")
                return@withContext Result.success()
            }

            Log.d(TAG, "Syncing ${sessions.size} sessions to Supabase…")

            val apiKey     = NetworkModule.SUPABASE_ANON_KEY
            val auth       = "Bearer $apiKey"
            val ct         = "application/json"
            val prefer     = "return=minimal,resolution=merge-duplicates"

            // ── 1. Upload sessions ───────────────────────────────────────────
            val sessionPayload = sessions.map { s ->
                mapOf(
                    "id"                    to s.id,
                    "name"                  to s.name,
                    "start_time_ms"         to s.startTimeMs,
                    "end_time_ms"           to s.endTimeMs,
                    "total_laps"            to s.totalLaps,
                    "total_steps"           to s.totalSteps,
                    "total_distance_meters" to s.totalDistanceMeters,
                    "total_duration_ms"     to s.totalDurationMs
                )
            }
            val r1 = supabaseService.uploadSessions(apiKey, auth, ct, prefer, sessionPayload)
            if (!r1.isSuccessful) {
                Log.e(TAG, "Session upload failed: ${r1.code()} ${r1.errorBody()?.string()}")
                return@withContext Result.retry()
            }
            Log.d(TAG, "Sessions uploaded OK")

            // ── 2. Upload laps for those sessions ────────────────────────────
            val laps = sessions.flatMap { s -> repository.getLaps(s.id) }
            if (laps.isNotEmpty()) {
                val lapsPayload = laps.map { l ->
                    mapOf(
                        "id"                 to l.id,
                        "session_id"         to l.sessionId,
                        "lap_number"         to l.lapNumber,
                        "start_time_ms"      to l.startTimeMs,
                        "end_time_ms"        to l.endTimeMs,
                        "duration_ms"        to l.durationMs,
                        "steps"              to l.steps,
                        "distance_meters"    to l.distanceMeters,
                        "avg_pace_sec_per_km" to l.avgPaceSecPerKm
                    )
                }
                val r2 = supabaseService.uploadLaps(apiKey, auth, ct, prefer, lapsPayload)
                if (!r2.isSuccessful) {
                    Log.e(TAG, "Laps upload failed: ${r2.code()} ${r2.errorBody()?.string()}")
                    return@withContext Result.retry()
                }
                Log.d(TAG, "Laps uploaded OK (${laps.size})")
            }

            // ── 3. Upload GPS points for those sessions ──────────────────────
            val points = sessions.flatMap { s -> repository.getGpsPoints(s.id) }
            if (points.isNotEmpty()) {
                // Supabase has a row limit per request — chunk into 500-point batches
                points.chunked(500).forEach { chunk ->
                    val pointsPayload = chunk.map { p ->
                        mapOf(
                            "id"           to p.id,
                            "session_id"   to p.sessionId,
                            "lap_id"       to p.lapId,
                            "latitude"     to p.latitude,
                            "longitude"    to p.longitude,
                            "timestamp_ms" to p.timestampMs,
                            "accuracy_m"   to p.accuracyM
                        )
                    }
                    val r3 = supabaseService.uploadGpsPoints(apiKey, auth, ct, prefer, pointsPayload)
                    if (!r3.isSuccessful) {
                        Log.e(TAG, "GPS points upload failed: ${r3.code()} ${r3.errorBody()?.string()}")
                        return@withContext Result.retry()
                    }
                }
                Log.d(TAG, "GPS points uploaded OK (${points.size})")
            }

            // ── 4. Mark all uploaded sessions as synced ──────────────────────
            sessions.forEach { s -> repository.markSessionSynced(s.id) }
            Log.d(TAG, "All synced. ✓")

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed with exception: ${e.message}", e)
            Result.retry()
        }
    }
}
