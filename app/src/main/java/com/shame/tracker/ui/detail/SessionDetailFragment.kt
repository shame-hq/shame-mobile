package com.shame.tracker.ui.detail

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shame.tracker.R
import com.shame.tracker.ui.session.LapSplitAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
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

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        mapView = view.findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
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
                .maxByOrNull { it?.accuracy ?: Float.MAX_VALUE }
            if (loc != null) {
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
            } else {
                // Absolute fallback: zoom 15, world centre (0,0) — still not zoom-1!
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(GeoPoint(0.0, 0.0))
            }
        } catch (e: Exception) {
            mapView.controller.setZoom(15.0)
        }

        lapAdapter = LapSplitAdapter()
        val rv = view.findViewById<RecyclerView>(R.id.rvDetailLaps)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = lapAdapter

        val tvStats  = view.findViewById<TextView>(R.id.tvDetailStats)
        val tvTitle  = view.findViewById<TextView>(R.id.tvDetailTitle)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.session.collect { session ->
                        if (session != null) {
                            val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
                            tvTitle.text = sdf.format(Date(session.startTimeMs))
                            val sec     = session.totalDurationMs / 1000
                            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60)
                            val distStr = String.format(Locale.getDefault(), "%.2f km", session.totalDistanceMeters / 1000.0)
                            tvStats.text = "Distance: $distStr\nTime: $timeStr\nLaps: ${session.totalLaps}"
                        }
                    }
                }
                launch { viewModel.laps.collect  { lapAdapter.submitList(it) } }
                launch { viewModel.points.collect { if (it.isNotEmpty()) drawRoute(it) } }
            }
        }
    }

    private fun drawRoute(points: List<com.shame.tracker.data.db.entity.GpsPoint>) {
        val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }

        // Remove only previous route overlays, keep tile layer
        mapView.overlays.removeAll { it is Polyline }

        val line = Polyline()
        line.setPoints(geoPoints)
        line.outlinePaint.color  = Color.parseColor("#F97316")
        line.outlinePaint.strokeWidth = 10f
        mapView.overlays.add(line)

        mapView.post {
            if (geoPoints.size == 1) {
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(geoPoints[0])
            } else {
                val box = BoundingBox.fromGeoPoints(geoPoints)
                // Guarantee at least ~500 m span so we don't over-zoom tiny routes
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
