package com.shame.tracker.ui.detail

import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shame.tracker.R
import com.shame.tracker.ui.session.LapSplitAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SessionDetailFragment : Fragment(R.layout.fragment_session_detail) {

    private val viewModel: SessionDetailViewModel by viewModels()
    private lateinit var mapView: MapView
    private lateinit var lapAdapter: LapSplitAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))

        mapView = view.findViewById(R.id.mapView)
        mapView.setMultiTouchControls(true)

        lapAdapter = LapSplitAdapter()
        val rv = view.findViewById<RecyclerView>(R.id.rvDetailLaps)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = lapAdapter

        val tvStats = view.findViewById<TextView>(R.id.tvDetailStats)
        val tvTitle = view.findViewById<TextView>(R.id.tvDetailTitle)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.session.collect { session ->
                        if (session != null) {
                            val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
                            tvTitle.text = sdf.format(Date(session.startTimeMs))
                            
                            val sec = session.totalDurationMs / 1000
                            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60)
                            val distStr = String.format(Locale.getDefault(), "%.2f km", session.totalDistanceMeters / 1000.0)
                            tvStats.text = "Distance: $distStr\nTime: $timeStr\nLaps: ${session.totalLaps}"
                        }
                    }
                }
                launch {
                    viewModel.laps.collect { laps ->
                        lapAdapter.submitList(laps)
                    }
                }
                launch {
                    viewModel.points.collect { points ->
                        if (points.isNotEmpty()) {
                            drawMap(points)
                        }
                    }
                }
            }
        }
    }

    private fun drawMap(points: List<com.shame.tracker.data.db.entity.GpsPoint>) {
        val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }

        mapView.overlays.clear()

        // Constrain zoom so the map never shows the whole world
        mapView.minZoomLevel = 12.0
        mapView.maxZoomLevel = 19.0

        if (geoPoints.isNotEmpty()) {
            val line = Polyline()
            line.setPoints(geoPoints)
            line.outlinePaint.color = Color.parseColor("#F97316")
            line.outlinePaint.strokeWidth = 10f
            mapView.overlays.add(line)

            if (geoPoints.size == 1) {
                // Single point — centre with ~5 km zoom
                mapView.controller.setZoom(14.5)
                mapView.controller.setCenter(geoPoints[0])
            } else {
                val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                // Ensure the bounding box covers at least ~500 m so we don't over-zoom
                val minSpan = 0.005 // ~500 m in degrees
                val north = boundingBox.latNorth.coerceAtLeast(boundingBox.latSouth + minSpan)
                val east  = boundingBox.lonEast.coerceAtLeast(boundingBox.lonWest + minSpan)
                val safeBox = BoundingBox(north, east, boundingBox.latSouth, boundingBox.lonWest)
                mapView.post {
                    mapView.zoomToBoundingBox(safeBox, true, 80)
                }
            }
        }
        mapView.invalidate()
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
