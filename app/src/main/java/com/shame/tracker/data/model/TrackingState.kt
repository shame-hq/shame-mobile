package com.shame.tracker.data.model

data class TrackingState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val sessionElapsedMs: Long = 0L,
    val currentLapElapsedMs: Long = 0L,
    val lastLapDurationMs: Long = 0L,
    val completedLapCount: Int = 0,
    val currentLapSteps: Long = 0L,
    val totalSteps: Long = 0L,
    val currentLapDistanceM: Double = 0.0,
    val totalDistanceM: Double = 0.0,
    val currentPaceSecPerKm: Double = 0.0,
    val currentCadenceSpm: Double = 0.0,
    val sessionId: Long = 0L,
    val currentLapId: Long = 0L
) {
    val lapCount: Int get() = completedLapCount + 1
    val sessionDistanceMeters: Double get() = totalDistanceM
    val currentPaceMinutesPerKm: Double get() = currentPaceSecPerKm
}
