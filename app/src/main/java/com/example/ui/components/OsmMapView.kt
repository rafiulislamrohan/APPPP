package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.example.data.local.GpsPoint
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun OsmInteractiveMap(
  points: List<GpsPoint>,
  currentHighlightIndex: Int? = null,
  isOnline: Boolean,
  modifier: Modifier = Modifier,
  onMapViewReady: (MapView) -> Unit = {},
) {
  val context = LocalContext.current
  val mapView = remember {
    MapView(context).apply {
      setTileSource(TileSourceFactory.MAPNIK)
      setMultiTouchControls(true)
      controller.setZoom(15.0)
      controller.setCenter(GeoPoint(23.8103, 90.4125))
    }
  }

  DisposableEffect(Unit) {
    mapView.onResume()
    onMapViewReady(mapView)
    onDispose { mapView.onPause() }
  }

  Box(modifier = modifier) {
    AndroidView(
      factory = { mapView },
      modifier = Modifier.fillMaxSize(),
      update = { mv ->
        mv.setUseDataConnection(isOnline)
        mv.overlays.clear()

        if (points.isNotEmpty()) {
          val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
          val polyline =
            Polyline(mv).apply {
              setPoints(geoPoints)
              outlinePaint.color = "#526B57".toColorInt()
              outlinePaint.strokeWidth = 10f
            }
          mv.overlays.add(polyline)

          // Start marker
          val startMarker =
            Marker(mv).apply {
              position = geoPoints.first()
              title = "Start"
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
          mv.overlays.add(startMarker)

          // Current / Replay / End marker
          val highlightIdx =
            (currentHighlightIndex ?: (geoPoints.size - 1)).coerceIn(0, geoPoints.lastIndex)
          val activeMarker =
            Marker(mv).apply {
              position = geoPoints[highlightIdx]
              title = if (currentHighlightIndex != null) "Replay Position" else "Current / Finish"
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
          mv.overlays.add(activeMarker)

          if (currentHighlightIndex != null) {
            mv.controller.animateTo(geoPoints[highlightIdx])
          } else if (geoPoints.size == 1) {
            mv.controller.setCenter(geoPoints.first())
            mv.controller.setZoom(16.5)
          } else {
            val box = BoundingBox.fromGeoPointsSafe(geoPoints)
            mv.post {
              runCatching { mv.zoomToBoundingBox(box, true, 80) }
            }
          }
        }
        mv.invalidate()
      },
    )
  }
}

fun downloadVisibleMapRegionTiles(
  mapView: MapView,
  onCompleted: (downloadedCount: Int, errorCount: Int) -> Unit,
  onError: (String) -> Unit,
) {
  runCatching {
    val cacheManager = CacheManager(mapView)
    val box = mapView.boundingBox
    val minZoom = mapView.zoomLevelDouble.toInt().coerceIn(10, 16)
    val maxZoom = (minZoom + 2).coerceAtMost(17)
    cacheManager.downloadAreaAsync(
      mapView.context,
      box,
      minZoom,
      maxZoom,
      object : CacheManager.CacheManagerCallback {
        override fun onTaskComplete() {
          onCompleted(1, 0)
        }

        override fun updateProgress(
          progress: Int,
          currentZoomLevel: Int,
          zoomMin: Int,
          zoomMax: Int,
        ) = Unit

        override fun downloadStarted() = Unit

        override fun setPossibleTilesInArea(total: Int) = Unit

        override fun onTaskFailed(errors: Int) {
          onCompleted(0, errors)
        }
      },
    )
  }.onFailure { err -> onError(err.localizedMessage ?: "Failed to cache map region") }
}
