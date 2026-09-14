package com.shame.tracker.service

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex

/**
 * OSMDroid tile source pointing to ESRI World Imagery satellite tiles.
 * Used both for online streaming and as the key when storing/looking up
 * cached tiles written by [SatelliteTileDownloadService].
 */
class EsriSatTileSource : OnlineTileSourceBase(
    /* aName       */ "ESRIWorldImagery",
    /* aZoomMinLevel*/ 0,
    /* aZoomMaxLevel*/ 19,
    /* aTileSizePixels */ 256,
    /* aImageFilenameEnding */ ".jpg",
    /* aBaseUrl     */ arrayOf(
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"
    )
) {
    /**
     * ESRI tile URL format: /zoom/row/col  (note: row = Y, col = X)
     */
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x    = MapTileIndex.getX(pMapTileIndex)
        val y    = MapTileIndex.getY(pMapTileIndex)
        return "${baseUrl}$zoom/$y/$x"
    }
}
