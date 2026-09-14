package com.shame.tracker.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.shame.tracker.util.SatelliteZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

/**
 * Background service that downloads ESRI World Imagery satellite tiles
 * for the offline corridor zone (home <-> college + buffers) on first launch.
 *
 * Tiles are stored in OSMDroid's standard SQL tile cache, so MapView picks
 * them up automatically when offline.
 */
class SatelliteTileDownloadService : Service() {

    companion object {
        private const val TAG = "SatTileDownload"
        const val ACTION_START_DOWNLOAD = "ACTION_START_SAT_DOWNLOAD"
        const val PREF_KEY_DOWNLOADED = "satellite_tiles_downloaded_v1"

        // ESRI World Imagery – free to use, no API key for basic raster tiles
        private const val ESRI_BASE =
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile"

        private val _progress = MutableStateFlow<DownloadProgress?>(null)
        val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

        data class DownloadProgress(
            val downloaded: Int,
            val total: Int,
            val done: Boolean = false,
            val failed: Boolean = false
        )

        fun shouldDownload(context: Context): Boolean {
            val prefs = context.getSharedPreferences("satellite_prefs", MODE_PRIVATE)
            return !prefs.getBoolean(PREF_KEY_DOWNLOADED, false)
        }

        fun markDownloaded(context: Context) {
            context.getSharedPreferences("satellite_prefs", MODE_PRIVATE)
                .edit().putBoolean(PREF_KEY_DOWNLOADED, true).apply()
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_DOWNLOAD) {
            scope.launch { downloadTiles() }
        }
        return START_NOT_STICKY
    }

    private suspend fun downloadTiles() {
        val tileWriter = SqlTileWriter()
        val esriSource = EsriSatTileSource()
        val bounds = SatelliteZone.downloadBounds

        // Collect all tile (z,x,y) in the bounding box for each zoom level
        val tiles = mutableListOf<Triple<Int, Int, Int>>()
        for (z in SatelliteZone.offlineZoomLevels) {
            val n = 1 shl z
            val xMin = lonToTileX(bounds.lonWest, z)
            val xMax = lonToTileX(bounds.lonEast, z)
            val yMin = latToTileY(bounds.latNorth, z)
            val yMax = latToTileY(bounds.latSouth, z)
            for (x in xMin..xMax) {
                for (y in yMin..yMax) {
                    tiles += Triple(z, x.coerceIn(0, n - 1), y.coerceIn(0, n - 1))
                }
            }
        }

        Log.i(TAG, "Starting download of ${tiles.size} satellite tiles")
        _progress.value = DownloadProgress(0, tiles.size)

        var downloaded = 0
        var failed = 0

        for ((z, x, y) in tiles) {
            try {
                val url = "$ESRI_BASE/$z/$y/$x"
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    setRequestProperty("User-Agent", "ShameTracker/1.0 (Android)")
                }
                if (conn.responseCode == 200) {
                    val bytes = conn.inputStream.readBytes()
                    conn.disconnect()
                    val tileIndex = MapTileIndex.getTileIndex(z, x, y)
                    tileWriter.saveFile(esriSource, tileIndex, bytes.inputStream(), null)
                    downloaded++
                } else {
                    conn.disconnect()
                    failed++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Tile z=$z x=$x y=$y failed: ${e.message}")
                failed++
            }
            _progress.value = DownloadProgress(downloaded, tiles.size)
        }

        val allOk = failed < tiles.size * 0.1  // allow up to 10% failure
        _progress.value = DownloadProgress(downloaded, tiles.size, done = true, failed = !allOk)
        if (allOk) markDownloaded(this)
        Log.i(TAG, "Download done: $downloaded ok, $failed failed")
        stopSelf()
    }

    // ── Tile coordinate helpers ──────────────────────────────────────────────

    private fun lonToTileX(lon: Double, z: Int): Int {
        return floor((lon + 180.0) / 360.0 * (1 shl z)).toInt()
    }

    private fun latToTileY(lat: Double, z: Int): Int {
        val radLat = Math.toRadians(lat)
        return floor((1.0 - ln(tan(radLat) + 1.0 / cos(radLat)) / Math.PI) / 2.0 * (1 shl z)).toInt()
    }
}
