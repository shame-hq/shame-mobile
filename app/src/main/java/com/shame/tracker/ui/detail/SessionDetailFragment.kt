package com.shame.tracker.ui.detail

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shame.tracker.R
import com.shame.tracker.data.db.entity.GpsPoint
import com.shame.tracker.data.db.entity.Lap
import com.shame.tracker.service.EsriSatTileSource
import com.shame.tracker.ui.session.LapSplitAdapter
import com.shame.tracker.util.LapColors
import com.shame.tracker.util.SatelliteZone
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SessionDetailFragment : Fragment(R.layout.fragment_session_detail) {

    private val viewModel: SessionDetailViewModel by viewModels()
    private lateinit var mapView: MapView
    private lateinit var lapAdapter: LapSplitAdapter

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        mapView = view.findViewById(R.id.mapView)
        // Default to satellite tile source
        mapView.setTileSource(EsriSatTileSource())
        mapView.setMultiTouchControls(true)

        // ── Lock zoom to neighbourhood scale immediately ──────────────────────
        mapView.minZoomLevel = 12.0
        mapView.maxZoomLevel = 19.0

        // Default: centre on device's last known location at zoom 15 (~2km across)
        try {
            val lm = requireContext().getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            val loc: Location? = providers
                .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.accuracy }
            if (loc != null) {
                mapView.controller.setZoom(16.0)
                mapView.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
            } else {
                mapView.controller.setZoom(16.0)
                mapView.controller.setCenter(GeoPoint(SatelliteZone.HOME_LAT, SatelliteZone.HOME_LON))
            }
        } catch (e: Exception) {
            mapView.controller.setZoom(16.0)
            mapView.controller.setCenter(GeoPoint(SatelliteZone.HOME_LAT, SatelliteZone.HOME_LON))
        }

        lapAdapter = LapSplitAdapter()
        val rv = view.findViewById<RecyclerView>(R.id.rvDetailLaps)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = lapAdapter

        val tvStats  = view.findViewById<TextView>(R.id.tvDetailStats)
        val tvTitle  = view.findViewById<TextView>(R.id.tvDetailTitle)
        val tvDistance = view.findViewById<TextView?>(R.id.tvDetailDistance)
        val tvDuration = view.findViewById<TextView?>(R.id.tvDetailDuration)
        val tvLaps = view.findViewById<TextView?>(R.id.tvDetailLaps)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.session.collect { session ->
                        if (session != null) {
                            val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
                            tvTitle.text = sdf.format(Date(session.startTimeMs))
                            val sec     = session.totalDurationMs / 1000
                            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60)
                            val distStr = String.format(Locale.getDefault(), "%.2f km", session.totalDistanceMeters / 1000.0)
                            tvStats.text = "Distance: $distStr\nTime: $timeStr\nLaps: ${session.totalLaps}"
                            tvDistance?.text = distStr
                            tvDuration?.text = timeStr
                            tvLaps?.text = "${session.totalLaps}"
                        }
                    }
                }
                launch { viewModel.laps.collect { lapAdapter.submitList(it) } }
                launch {
                    viewModel.points.collect { pts ->
                        if (pts.isNotEmpty()) {
                            drawSessionRoute(pts, viewModel.laps.value)
                        }
                    }
                }
            }
        }
    }

    private fun drawSessionRoute(points: List<GpsPoint>, laps: List<Lap>) {
        mapView.overlays.removeAll { it is Polyline || (it is Marker && it.id?.startsWith("seg_") == true) }

        val allGeoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
        if (allGeoPoints.isEmpty()) return

        // Check if route is in satellite zone
        val firstPt = allGeoPoints.first()
        val inZone = SatelliteZone.contains(firstPt.latitude, firstPt.longitude)
        if (inZone) {
            mapView.setTileSource(EsriSatTileSource())
        } else {
            mapView.setTileSource(TileSourceFactory.MAPNIK)
        }

        // Group points by lapId
        val pointsByLap = points.groupBy { it.lapId }
        val lapMap = laps.associateBy { it.id }
        val existingMarkerPositions = mutableListOf<GeoPoint>()

        pointsByLap.forEach { (lapId, lapPts) ->
            val lap = lapMap[lapId]
            val lapNum = lap?.lapNumber ?: 1
            val color = LapColors.forLapNumber(lapNum)
            val lapGeoPoints = lapPts.map { GeoPoint(it.latitude, it.longitude) }

            if (lapGeoPoints.size >= 2) {
                val line = Polyline(mapView)
                line.setPoints(lapGeoPoints)
                line.outlinePaint.color = color
                line.outlinePaint.strokeWidth = 9f
                line.outlinePaint.alpha = 220
                line.infoWindow = null
                mapView.overlays.add(line)
            }

            // Clickable segment parts: distance chunks of at least 250m
            val minSegmentMeters = 250.0
            val minMarkerSeparationMeters = 25.0

            var segStartIndex = 0
            var accumulatedDistance = 0.0
            var segmentNumber = 1

            for (i in 0 until lapPts.size - 1) {
                val a = lapPts[i]
                val b = lapPts[i + 1]
                val locA = Location("").apply { latitude = a.latitude; longitude = a.longitude }
                val locB = Location("").apply { latitude = b.latitude; longitude = b.longitude }
                accumulatedDistance += locA.distanceTo(locB)

                val isLastPoint = (i == lapPts.size - 2)
                if (accumulatedDistance >= minSegmentMeters || (isLastPoint && accumulatedDistance >= 100.0 && segmentNumber == 1)) {
                    val segPts = lapPts.subList(segStartIndex, i + 2)
                    val midIdx = (segStartIndex + i + 1) / 2
                    val rawMidGeo = lapGeoPoints[midIdx]

                    // Prevent overlapping markers across all laps / segments
                    var finalMidGeo = rawMidGeo
                    val tooClose = existingMarkerPositions.any { existing ->
                        val loc1 = Location("").apply { latitude = existing.latitude; longitude = existing.longitude }
                        val loc2 = Location("").apply { latitude = finalMidGeo.latitude; longitude = finalMidGeo.longitude }
                        loc1.distanceTo(loc2) < minMarkerSeparationMeters
                    }

                    if (tooClose) {
                        finalMidGeo = GeoPoint(
                            rawMidGeo.latitude + 0.0003 * (if (segmentNumber % 2 == 0) 1 else -1),
                            rawMidGeo.longitude + 0.0003 * (if (lapNum % 2 == 0) 1 else -1)
                        )
                    }
                    existingMarkerPositions.add(finalMidGeo)

                    val segDurationMs = maxOf(1L, segPts.last().timestampMs - segPts.first().timestampMs)
                    val segDurationSec = segDurationMs / 1000.0
                    val segPaceSec: Long = if (accumulatedDistance > 5.0) (segDurationSec / (accumulatedDistance / 1000.0)).toLong() else 0L

                    val lapTotalSteps = lap?.steps ?: 0L
                    val lapTotalPts = lapPts.size
                    val segStepsEst = if (lapTotalPts > 0) (lapTotalSteps * segPts.size / lapTotalPts) else 0L

                    val segDistKm = accumulatedDistance / 1000.0
                    val paceStr = if (segPaceSec > 0) {
                        "${segPaceSec / 60}:${String.format(Locale.getDefault(), "%02d", segPaceSec % 60)} /km"
                    } else "-- /km"

                    val marker = Marker(mapView).apply {
                        id = "seg_${lapId}_$segmentNumber"
                        position = finalMidGeo
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = ContextCompat.getDrawable(requireContext(), R.drawable.circle_shape)
                            ?.mutate()?.also { d ->
                                (d as? android.graphics.drawable.GradientDrawable)?.setColor(color)
                            }
                        title = "Lap $lapNum · Part $segmentNumber"
                        snippet = buildString {
                            append("Distance: ${String.format(Locale.getDefault(), "%.2f", segDistKm)} km\n")
                            append("Pace: $paceStr\n")
                            append("Steps: ~$segStepsEst")
                        }
                    }
                    mapView.overlays.add(marker)

                    segStartIndex = i + 1
                    accumulatedDistance = 0.0
                    segmentNumber++
                }
            }
        }

        mapView.post {
            if (allGeoPoints.size == 1) {
                mapView.controller.setZoom(16.0)
                mapView.controller.setCenter(allGeoPoints[0])
            } else {
                val box = BoundingBox.fromGeoPoints(allGeoPoints)
                val minDeg = 0.005
                val safe = BoundingBox(
                    box.latNorth.coerceAtLeast(box.latSouth + minDeg),
                    box.lonEast.coerceAtLeast(box.lonWest + minDeg),
                    box.latSouth,
                    box.lonWest
                )
                mapView.zoomToBoundingBox(safe, true, 80)
            }
            mapView.invalidate()
        }
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause()  { super.onPause();  mapView.onPause()  }
}
