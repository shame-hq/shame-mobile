package com.shame.tracker.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import com.shame.tracker.data.db.entity.GpsPoint
import com.shame.tracker.data.db.entity.Lap
import com.shame.tracker.data.db.entity.Session
import com.shame.tracker.data.model.TrackingState
import com.shame.tracker.data.repository.SessionRepository
import com.shame.tracker.util.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : Service(), LocationListener, SensorEventListener {

    @Inject
    lateinit var repository: SessionRepository

    inner class LocalBinder : Binder() {
        fun getService(): TrackingService = this@TrackingService
    }

    private val binder = LocalBinder()

    companion object {
        private val _trackingState = MutableStateFlow(TrackingState())
        val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

        private val _laps = MutableStateFlow<List<Lap>>(emptyList())
        val laps: StateFlow<List<Lap>> = _laps.asStateFlow()

        /** Live GPS points for the active session, keyed by lapId → list of points */
        private val _gpsPoints = MutableStateFlow<Map<Long, List<GpsPoint>>>(emptyMap())
        val gpsPoints: StateFlow<Map<Long, List<GpsPoint>>> = _gpsPoints.asStateFlow()

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_LAP = "ACTION_LAP"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var timerJob: Job? = null

    private lateinit var locationManager: LocationManager
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null

    private var initialStepCount = -1L
    private var currentLapInitialSteps = -1L
    private var lastLocation: Location? = null

    private var activeSessionId: Long = 0L
    private var activeLapId: Long = 0L

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_PAUSE -> pauseTracking()
            ACTION_STOP  -> stopTracking()
            ACTION_LAP   -> recordLap()
        }
        return START_STICKY
    }

    // ── Public API for Fragment binding ──────────────────────────────────────

    fun isPaused() = _trackingState.value.isPaused

    fun pauseSession() = pauseTracking()

    fun resumeSession() {
        _trackingState.update { it.copy(isPaused = false) }
        startTimer()
    }

    fun recordLap() {
        if (!_trackingState.value.isRunning || _trackingState.value.isPaused) return
        serviceScope.launch {
            val state = _trackingState.value
            repository.getActiveLap(activeSessionId)?.let { activeLap ->
                val finalLap = activeLap.copy(
                    endTimeMs = System.currentTimeMillis(),
                    durationMs = state.currentLapElapsedMs,
                    distanceMeters = state.currentLapDistanceM,
                    steps = state.currentLapSteps,
                    avgPaceSecPerKm = if (state.currentLapDistanceM > 0)
                        (state.currentLapElapsedMs / 1000.0) / (state.currentLapDistanceM / 1000.0)
                    else 0.0
                )
                repository.updateLap(finalLap)
            }
            val newLapCount = state.completedLapCount + 1
            currentLapInitialSteps = -1L
            startNewLap(newLapCount + 1)
            _trackingState.update {
                it.copy(
                    completedLapCount = newLapCount,
                    currentLapId = activeLapId,
                    lastLapDurationMs = state.currentLapElapsedMs,
                    currentLapElapsedMs = 0L,
                    currentLapDistanceM = 0.0,
                    currentLapSteps = 0L
                )
            }
            refreshLaps()
        }
    }

    suspend fun endSessionAndSave(): Long {
        pauseTracking()
        val state = _trackingState.value
        repository.getActiveLap(activeSessionId)?.let { activeLap ->
            val finalLap = activeLap.copy(
                endTimeMs = System.currentTimeMillis(),
                durationMs = state.currentLapElapsedMs,
                distanceMeters = state.currentLapDistanceM,
                steps = state.currentLapSteps,
                avgPaceSecPerKm = if (state.currentLapDistanceM > 0)
                    (state.currentLapElapsedMs / 1000.0) / (state.currentLapDistanceM / 1000.0)
                else 0.0
            )
            repository.updateLap(finalLap)
        }
        repository.getSession(activeSessionId)?.let { session ->
            val finalSession = session.copy(
                endTimeMs = System.currentTimeMillis(),
                totalLaps = state.completedLapCount + 1,
                totalDistanceMeters = state.totalDistanceM,
                totalSteps = state.totalSteps,
                totalDurationMs = state.sessionElapsedMs
            )
            repository.updateSession(finalSession)
        }
        locationManager.removeUpdates(this@TrackingService)
        sensorManager?.unregisterListener(this@TrackingService)
        
        // Trigger background sync to Supabase immediately after run finishes
        SyncScheduler.scheduleImmediate(this@TrackingService)
        
        return activeSessionId
    }

    // ─────────────────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        if (_trackingState.value.isRunning && !_trackingState.value.isPaused) return

        if (_trackingState.value.isPaused) {
            _trackingState.update { it.copy(isPaused = false) }
            startTimer()
            return
        }

        val notification = NotificationHelper.buildNotification(this, "00:00", 1)
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2f, this)
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        serviceScope.launch {
            val session = Session(name = "Run Session", startTimeMs = System.currentTimeMillis())
            activeSessionId = repository.createSession(session)
            startNewLap(1)
            _trackingState.update {
                TrackingState(
                    isRunning = true,
                    isPaused = false,
                    sessionId = activeSessionId,
                    currentLapId = activeLapId,
                    completedLapCount = 0
                )
            }
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (true) {
                delay(1000)
                if (!_trackingState.value.isPaused) {
                    _trackingState.update {
                        val newLapMs = it.currentLapElapsedMs + 1000
                        val newSesMs = it.sessionElapsedMs + 1000
                        val pace = if (it.currentLapDistanceM > 0)
                            (newLapMs / 1000.0 / 60.0) / (it.currentLapDistanceM / 1000.0)
                        else 0.0
                        it.copy(
                            sessionElapsedMs = newSesMs,
                            currentLapElapsedMs = newLapMs,
                            currentPaceSecPerKm = pace
                        )
                    }
                    val seconds = (_trackingState.value.sessionElapsedMs / 1000) % 60
                    val minutes = (_trackingState.value.sessionElapsedMs / 1000) / 60
                    val timeStr = String.format("%02d:%02d", minutes, seconds)
                    val notif = NotificationHelper.buildNotification(
                        this@TrackingService, timeStr,
                        _trackingState.value.completedLapCount + 1
                    )
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    manager.notify(NotificationHelper.NOTIFICATION_ID, notif)
                }
            }
        }
    }

    private fun pauseTracking() {
        _trackingState.update { it.copy(isPaused = true) }
        timerJob?.cancel()
    }

    private fun stopTracking() {
        serviceScope.launch {
            endSessionAndSave()
            _trackingState.value = TrackingState()
            _laps.value = emptyList()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun startNewLap(lapNum: Int) {
        val lap = Lap(sessionId = activeSessionId, lapNumber = lapNum, startTimeMs = System.currentTimeMillis())
        activeLapId = repository.insertLap(lap)
        refreshLaps()
    }

    private suspend fun refreshLaps() {
        _laps.value = repository.getLaps(activeSessionId)
    }

    private suspend fun refreshGpsPoints() {
        val allLaps = repository.getLaps(activeSessionId)
        val map = mutableMapOf<Long, List<GpsPoint>>()
        for (lap in allLaps) {
            map[lap.id] = repository.getGpsPointsForLap(lap.id)
        }
        _gpsPoints.value = map
    }

    override fun onLocationChanged(location: Location) {
        if (!_trackingState.value.isRunning || _trackingState.value.isPaused) return
        serviceScope.launch {
            val pt = GpsPoint(
                sessionId = activeSessionId,
                lapId = activeLapId,
                latitude = location.latitude,
                longitude = location.longitude,
                timestampMs = System.currentTimeMillis(),
                accuracyM = location.accuracy
            )
            repository.insertGpsPoint(pt)
            lastLocation?.let { lastLoc ->
                val distance = lastLoc.distanceTo(location).toDouble()
                _trackingState.update {
                    it.copy(
                        currentLapDistanceM = it.currentLapDistanceM + distance,
                        totalDistanceM = it.totalDistanceM + distance
                    )
                }
            }
            lastLocation = location
            refreshGpsPoints()
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsVal = event.values[0].toLong()
            if (initialStepCount == -1L) initialStepCount = totalStepsVal
            if (currentLapInitialSteps == -1L) currentLapInitialSteps = totalStepsVal
            val lapSteps = totalStepsVal - currentLapInitialSteps
            val sessionSteps = totalStepsVal - initialStepCount
            _trackingState.update { it.copy(currentLapSteps = lapSteps, totalSteps = sessionSteps) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
