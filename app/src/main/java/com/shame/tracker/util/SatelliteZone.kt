package com.shame.tracker.util

import android.graphics.Color
import org.osmdroid.util.BoundingBox
import kotlin.math.*

/**
 * Defines the geographic zone where offline satellite tiles are pre-fetched.
 *
 * Zone shape: two anchor circles (home + college, 5 km radius each)
 * connected by a ~10 km wide corridor along the straight line between them.
 *
 * Anchors derived from actual GPS history in the Room DB.
 */
object SatelliteZone {

    // ── Anchors (from GPS cluster analysis) ────────────────────────────────
    const val HOME_LAT    = 27.7165
    const val HOME_LON    = 85.3234
    const val COLLEGE_LAT = 27.7279
    const val COLLEGE_LON = 85.3181

    /** Radius of the circle around each anchor point, in metres. */
    private const val ANCHOR_RADIUS_M = 5_000.0

    /** Half-width of the corridor joining the two anchor circles, in metres. */
    private const val CORRIDOR_HALF_WIDTH_M = 10_000.0

    // ── Tile download bounding box ──────────────────────────────────────────
    // We pad an axis-aligned box that fully contains both circles + corridor.
    val downloadBounds: BoundingBox by lazy {
        val pad = CORRIDOR_HALF_WIDTH_M   // 10 km each side = 10 km per side
        val latPerM = 1.0 / 110_574.0
        val lonPerM = 1.0 / (111_320.0 * cos(Math.toRadians((HOME_LAT + COLLEGE_LAT) / 2)))

        val minLat = minOf(HOME_LAT, COLLEGE_LAT) - pad * latPerM
        val maxLat = maxOf(HOME_LAT, COLLEGE_LAT) + pad * latPerM
        val minLon = minOf(HOME_LON, COLLEGE_LON) - pad * lonPerM
        val maxLon = maxOf(HOME_LON, COLLEGE_LON) + pad * lonPerM

        BoundingBox(maxLat, maxLon, minLat, minLon)
    }

    /** Zoom levels to pre-cache for offline use. */
    val offlineZoomLevels = 14..16

    // ── Point-in-zone test ──────────────────────────────────────────────────

    /**
     * Returns true if [lat]/[lon] falls inside the zone:
     *  • within [ANCHOR_RADIUS_M] of either anchor, OR
     *  • within [CORRIDOR_HALF_WIDTH_M] of the line segment home↔college.
     */
    fun contains(lat: Double, lon: Double): Boolean {
        if (distanceTo(lat, lon, HOME_LAT, HOME_LON) <= ANCHOR_RADIUS_M) return true
        if (distanceTo(lat, lon, COLLEGE_LAT, COLLEGE_LON) <= ANCHOR_RADIUS_M) return true
        return distToSegment(lat, lon) <= CORRIDOR_HALF_WIDTH_M
    }

    // ── Geometry helpers ────────────────────────────────────────────────────

    /** Haversine distance in metres between two lat/lon points. */
    fun distanceTo(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * R * asin(sqrt(a))
    }

    /**
     * Perpendicular distance (metres) from point to the home↔college segment.
     * Projects [lat]/[lon] onto the segment and clamps to endpoints.
     */
    private fun distToSegment(lat: Double, lon: Double): Double {
        // Convert everything to a local metric frame (metres) relative to HOME
        val latPerM = 1.0 / 110_574.0
        val lonPerM = 1.0 / (111_320.0 * cos(Math.toRadians(HOME_LAT)))

        val px = (lon - HOME_LON) / lonPerM
        val py = (lat - HOME_LAT) / latPerM
        val ax = 0.0
        val ay = 0.0
        val bx = (COLLEGE_LON - HOME_LON) / lonPerM
        val by = (COLLEGE_LAT - HOME_LAT) / latPerM

        val abLen2 = bx * bx + by * by
        if (abLen2 == 0.0) return sqrt(px * px + py * py)  // degenerate

        val t = ((px - ax) * bx + (py - ay) * by) / abLen2
        val tClamped = t.coerceIn(0.0, 1.0)
        val closestX = ax + tClamped * bx
        val closestY = ay + tClamped * by
        val dx = px - closestX
        val dy = py - closestY
        return sqrt(dx * dx + dy * dy)
    }
}
