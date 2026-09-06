package com.shame.tracker.util

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geographic utility functions used by the TrackingService.
 *
 * All distance calculations use the Haversine formula, which gives
 * accurate results for the short distances involved in lap tracking
 * (error < 0.5% for distances under 20 km).
 */
object GeoUtils {

    private const val EARTH_RADIUS_M = 6_371_000.0  // metres

    /**
     * Calculate the great-circle distance between two GPS coordinates
     * using the Haversine formula.
     *
     * @return Distance in metres.
     */
    fun distanceMeters(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    /**
     * Returns true if the user has returned within [radiusM] metres of
     * the session start point [startLat]/[startLng].
     *
     * Used by TrackingService to auto-complete a lap when the geofence
     * setting is enabled.
     *
     * @param minDistanceCoveredM Minimum distance that must have been
     *   covered before the geofence is checked (prevents false triggers
     *   at the very start of a lap). Default 200 m.
     */
    fun isInsideStartGeofence(
        currentLat: Double, currentLng: Double,
        startLat: Double, startLng: Double,
        radiusM: Double = 30.0,
        lapDistanceCoveredM: Double,
        minDistanceCoveredM: Double = 200.0
    ): Boolean {
        if (lapDistanceCoveredM < minDistanceCoveredM) return false
        val dist = distanceMeters(currentLat, currentLng, startLat, startLng)
        return dist <= radiusM
    }

    /**
     * Sum the distances between consecutive GPS points.
     *
     * @param points List of (latitude, longitude) pairs in chronological order.
     * @return Total path length in metres.
     */
    fun totalPathDistance(points: List<Pair<Double, Double>>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until points.size) {
            total += distanceMeters(
                points[i - 1].first, points[i - 1].second,
                points[i].first, points[i].second
            )
        }
        return total
    }
}
