package com.shame.tracker.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SupabaseService {
    @POST("rest/v1/run_sessions")
    suspend fun uploadSessions(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("Content-Type") contentType: String,
        @Header("Prefer") prefer: String,
        @Body sessions: List<Map<String, Any?>>
    ): retrofit2.Response<Void>

    @POST("rest/v1/run_laps")
    suspend fun uploadLaps(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("Content-Type") contentType: String,
        @Header("Prefer") prefer: String,
        @Body laps: List<Map<String, Any?>>
    ): retrofit2.Response<Void>

    @POST("rest/v1/run_gps_points")
    suspend fun uploadGpsPoints(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("Content-Type") contentType: String,
        @Header("Prefer") prefer: String,
        @Body points: List<Map<String, Any?>>
    ): retrofit2.Response<Void>
}
