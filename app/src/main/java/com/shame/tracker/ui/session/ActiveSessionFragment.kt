package com.shame.tracker.ui.session

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shame.tracker.R
import com.shame.tracker.data.db.entity.GpsPoint
import com.shame.tracker.data.db.entity.Lap
import com.shame.tracker.data.model.TrackingState
import com.shame.tracker.service.EsriSatTileSource
import com.shame.tracker.service.SatelliteTileDownloadService
import com.shame.tracker.service.TrackingService
import com.shame.tracker.util.LapColors
import com.shame.tracker.util.SatelliteZone
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

@AndroidEntryPoint
class ActiveSessionFragment : Fragment(R.layout.fragment_active_session) {

    private var trackingService: TrackingService? = null
    private var isBound = false
    private lateinit var lapAdapter: LapSplitAdapter
    private var mapView: MapView? = null

    // Track which polyline overlays we have drawn (lapId → Polyline)
    private val lapPolylines = mutableMapOf<Long, Polyline>()
    private var userNavMarker: Marker? = null
    private var previousGeoPoint: GeoPoint? = null
    private var lastBearing: Float = 0f

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TrackingService.LocalBinder
            trackingService = binder.getService()
            isBound = true
            observeState()
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── OSMDroid config ──────────────────────────────────────────────────
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )

        val esriTileSource = EsriSatTileSource()
        mapView = view.findViewById<MapView>(R.id.mapView).apply {
            setTileSource(esriTileSource)
            setMultiTouchControls(true)
            controller.setZoom(17.5)
        }

        // Fetch last known location immediately so user sees their location right away
        initUserCurrentLocation()

        // Start offline satellite tile prefetch if not downloaded yet
        if (SatelliteTileDownloadService.shouldDownload(requireContext())) {
            val satIntent = Intent(requireContext(), SatelliteTileDownloadService::class.java).apply {
                action = SatelliteTileDownloadService.ACTION_START_DOWNLOAD
            }
            requireContext().startService(satIntent)
        }

        // ── Lap split list ───────────────────────────────────────────────────
        lapAdapter = LapSplitAdapter()
        view.findViewById<RecyclerView>(R.id.rvLaps).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lapAdapter
        }

        // ── Start the tracking service ────────────────────────────────────────
        val startIntent = Intent(requireContext(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
        }
        ContextCompat.startForegroundService(requireContext(), startIntent)
        requireContext().bindService(
            Intent(requireContext(), TrackingService::class.java),
            connection, Context.BIND_AUTO_CREATE
        )

        // ── Button handlers ───────────────────────────────────────────────────
        view.findViewById<Button>(R.id.btnEndLap).setOnClickListener {
            trackingService?.recordLap()
        }

        view.findViewById<Button>(R.id.btnPause).setOnClickListener {
            val srv = trackingService ?: return@setOnClickListener
            if (srv.isPaused()) srv.resumeSession() else srv.pauseSession()
        }

        view.findViewById<Button>(R.id.btnEndSession).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_end_session)
                .setMessage(R.string.confirm_end_session_msg)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    val srv = trackingService ?: return@setPositiveButton
                    lifecycleScope.launch {
                        val sessionId = srv.endSessionAndSave()
                        requireContext().unbindService(connection)
                        isBound = false
                        requireContext().startService(
                            Intent(requireContext(), TrackingService::class.java).apply {
                                action = TrackingService.ACTION_STOP
                            }
                        )
                        findNavController().navigate(
                            R.id.action_session_to_summary,
                            Bundle().apply { putLong("sessionId", sessionId) }
                        )
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initUserCurrentLocation() {
        try {
            val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            val loc: Location? = providers
                .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.accuracy }

            if (loc != null) {
                val geo = GeoPoint(loc.latitude, loc.longitude)
                mapView?.controller?.setCenter(geo)
                updateUserNavMarker(geo, loc.bearing)
            } else {
                mapView?.controller?.setCenter(GeoPoint(SatelliteZone.HOME_LAT, SatelliteZone.HOME_LON))
            }
        } catch (e: Exception) {
            mapView?.controller?.setCenter(GeoPoint(SatelliteZone.HOME_LAT, SatelliteZone.HOME_LON))
        }
    }

    // ── Observe coroutines ────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    TrackingService.trackingState.collect { state ->
                        updateUI(state)
                    }
                }

                launch {
                    TrackingService.laps.collect { laps ->
                        lapAdapter.submitList(laps)
                        view?.findViewById<RecyclerView>(R.id.rvLaps)
                            ?.scrollToPosition(laps.size - 1)
                    }
                }

                launch {
                    TrackingService.gpsPoints.collect { pointsMap ->
                        val laps = TrackingService.laps.value
                        updateMapOverlays(laps, pointsMap)
                    }
                }
            }
        }
    }

    // ── UI update ─────────────────────────────────────────────────────────────

    private fun updateUI(state: TrackingState) {
        val v = view ?: return
        v.findViewById<TextView>(R.id.tvLap).apply {
            text = "Lap ${state.lapCount}"
            setTextColor(LapColors.forLapNumber(state.lapCount))
        }

        val lapSecs = state.currentLapElapsedMs / 1000
        v.findViewById<TextView>(R.id.tvTime).text =
            String.format(Locale.getDefault(), "%02d:%02d", lapSecs / 60, lapSecs % 60)

        val sesSecs = state.sessionElapsedMs / 1000
        v.findViewById<TextView>(R.id.tvSessionTime).text =
            String.format(Locale.getDefault(), "Total %02d:%02d", sesSecs / 60, sesSecs % 60)

        v.findViewById<TextView>(R.id.tvDistance).text =
            String.format(Locale.getDefault(), "%.2f km", state.sessionDistanceMeters / 1000.0)

        val paceSec = state.currentPaceMinutesPerKm.toLong()
        v.findViewById<TextView>(R.id.tvPace).text =
            if (paceSec > 0) String.format(Locale.getDefault(), "%d:%02d /km", paceSec / 60, paceSec % 60)
            else "0:00 /km"

        v.findViewById<Button>(R.id.btnPause).text =
            if (state.isPaused) getString(R.string.resume_session) else getString(R.string.pause_session)
    }

    // ── Map overlay logic ─────────────────────────────────────────────────────

    private fun updateMapOverlays(laps: List<Lap>, pointsMap: Map<Long, List<GpsPoint>>) {
        val map = mapView ?: return

        // Remove stale overlays for laps that no longer exist
        val currentLapIds = laps.map { it.id }.toSet()
        val staleLapIds = lapPolylines.keys - currentLapIds
        staleLapIds.forEach { id ->
            lapPolylines[id]?.let { map.overlays.remove(it) }
            lapPolylines.remove(id)
        }

        var latestGeoPoint: GeoPoint? = null

        for (lap in laps) {
            val pts = pointsMap[lap.id] ?: continue
            if (pts.isEmpty()) continue

            latestGeoPoint = GeoPoint(pts.last().latitude, pts.last().longitude)

            val color = LapColors.forLapNumber(lap.lapNumber)
            val geoPoints = pts.map { GeoPoint(it.latitude, it.longitude) }

            // ── Main polyline (only if >= 2 points) ─────────────────────────
            if (geoPoints.size >= 2) {
                val poly = lapPolylines.getOrPut(lap.id) {
                    Polyline(map).also { p ->
                        p.outlinePaint.color = color
                        p.outlinePaint.strokeWidth = 8f
                        p.outlinePaint.alpha = 220
                        p.infoWindow = null
                        map.overlays.add(p)
                    }
                }
                poly.setPoints(geoPoints)
            }

            // ── Clear previous segment markers for this lap ─────────────────
            map.overlays.removeAll(
                map.overlays.filterIsInstance<Marker>()
                    .filter { it.id?.startsWith("seg_${lap.id}_") == true }
            )

            // ── Segment markers: partition lap route into visible chunks ─────
            val segSize = SEGMENT_SIZE
            val numSegments = maxOf(1, (pts.size + segSize - 1) / segSize)

            for (seg in 0 until numSegments) {
                val startIdx = seg * segSize
                val endIdx = minOf(startIdx + segSize, pts.size - 1)
                if (startIdx > endIdx || startIdx >= pts.size) continue

                val segPts = pts.subList(startIdx, endIdx + 1)
                val midIdx = (startIdx + endIdx) / 2
                val midGeo = geoPoints[midIdx]

                // Distance for this segment (metres)
                var segDistM = 0.0
                for (i in startIdx until endIdx) {
                    val a = pts[i]; val b = pts[i + 1]
                    val loc = Location("").apply { latitude = a.latitude; longitude = a.longitude }
                    val loc2 = Location("").apply { latitude = b.latitude; longitude = b.longitude }
                    segDistM += loc.distanceTo(loc2)
                }

                // Duration for this segment
                val segDurationMs = maxOf(1L, segPts.last().timestampMs - segPts.first().timestampMs)
                val segDurationSec = segDurationMs / 1000.0

                // Pace (sec/km)
                val segPaceSec: Long = if (segDistM > 5.0) {
                    (segDurationSec / (segDistM / 1000.0)).toLong()
                } else 0L

                // Steps (estimated proportionally from lap total)
                val lapTotalSteps = laps.find { it.id == lap.id }?.steps ?: 0L
                val lapTotalPts = pts.size
                val segStepsEst = if (lapTotalPts > 0) (lapTotalSteps * segPts.size / lapTotalPts) else 0L

                val segDistKm = segDistM / 1000.0
                val paceStr = if (segPaceSec > 0) {
                    "${segPaceSec / 60}:${String.format(Locale.getDefault(), "%02d", segPaceSec % 60)} /km"
                } else "-- /km"

                val marker = Marker(map).apply {
                    id = "seg_${lap.id}_$seg"
                    position = midGeo
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.circle_shape)
                        ?.mutate()?.also { d ->
                            (d as? android.graphics.drawable.GradientDrawable)?.setColor(color)
                        }
                    title = "Lap ${lap.lapNumber} · Part ${seg + 1}"
                    snippet = buildString {
                        append("Distance: ${String.format(Locale.getDefault(), "%.2f", segDistKm)} km\n")
                        append("Pace: $paceStr\n")
                        append("Steps: ~$segStepsEst")
                    }
                }
                map.overlays.add(marker)
            }
        }

        // Update latest position & car navigation arrow marker
        latestGeoPoint?.let { currentGeo ->
            mapView?.controller?.animateTo(currentGeo)

            // Switch to 2D standard OSM map if outside the 10km corridor / 5km college-home circles
            val inZone = SatelliteZone.contains(currentGeo.latitude, currentGeo.longitude)
            val currentTileSource = mapView?.tileProvider?.tileSource
            if (inZone) {
                if (currentTileSource?.name() != "ESRIWorldImagery") {
                    mapView?.setTileSource(EsriSatTileSource())
                }
            } else {
                if (currentTileSource?.name() != TileSourceFactory.MAPNIK.name()) {
                    mapView?.setTileSource(TileSourceFactory.MAPNIK)
                }
            }

            var bearing = 0f
            if (previousGeoPoint != null) {
                val prevLoc = Location("").apply {
                    latitude = previousGeoPoint!!.latitude
                    longitude = previousGeoPoint!!.longitude
                }
                val curLoc = Location("").apply {
                    latitude = currentGeo.latitude
                    longitude = currentGeo.longitude
                }
                if (prevLoc.distanceTo(curLoc) > 1.5f) {
                    bearing = prevLoc.bearingTo(curLoc)
                    lastBearing = bearing
                } else {
                    bearing = lastBearing
                }
            }

            updateUserNavMarker(currentGeo, bearing)
            previousGeoPoint = currentGeo
        }

        map.invalidate()
    }

    private fun updateUserNavMarker(geo: GeoPoint, bearing: Float) {
        val map = mapView ?: return
        if (userNavMarker == null) {
            userNavMarker = Marker(map).apply {
                id = "user_nav_marker"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_nav_arrow)
                title = "Your Location"
                infoWindow = null
            }
            map.overlays.add(userNavMarker)
        }
        userNavMarker?.position = geo
        userNavMarker?.rotation = bearing
        map.invalidate()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDetach()
        mapView = null
        if (isBound) {
            requireContext().unbindService(connection)
            isBound = false
        }
    }

    companion object {
        /** Number of GPS points per clickable segment on the map */
        private const val SEGMENT_SIZE = 4
    }
}
