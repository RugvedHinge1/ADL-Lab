package com.scholr.app.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.scholr.app.data.geo.LatLng
import com.scholr.app.data.geo.NearbyPlace
import com.scholr.app.data.model.Conference
import com.scholr.app.data.model.FieldPalette
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

private const val LIBRARY_COLOR = 0xFF10B981.toInt()
private const val USER_COLOR = 0xFF2563EB.toInt()

/**
 * OpenStreetMap view (osmdroid) showing conferences, nearby libraries and the
 * user's position. Tiles need no API key.
 */
@Composable
fun NearbyMap(
    conferences: List<Conference>,
    libraries: List<NearbyPlace>,
    user: LatLng?,
    focusConferenceId: String?,
    onConferenceTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember { createMapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        // Composable may enter in RESUMED state, in which case no ON_RESUME is delivered.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            map.overlays.clear()

            conferences.filter { it.hasLocation }.forEach { c ->
                Marker(map).apply {
                    position = GeoPoint(c.lat, c.lng)
                    title = "${c.acronym} - ${c.name}"
                    snippet = "${c.location} | ${c.dates}"
                    icon = dot(FieldPalette.colorFor(c.field).toArgb(), 40)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { marker, _ ->
                        marker.showInfoWindow()
                        onConferenceTap(c.id)
                        true
                    }
                }.also(map.overlays::add)
            }

            libraries.forEach { lib ->
                Marker(map).apply {
                    position = GeoPoint(lib.position.lat, lib.position.lng)
                    title = lib.name
                    snippet = "Library | %.1f km away".format(lib.distanceKm)
                    icon = dot(LIBRARY_COLOR, 30)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }.also(map.overlays::add)
            }

            user?.let { u ->
                Marker(map).apply {
                    position = GeoPoint(u.lat, u.lng)
                    title = "You are here"
                    icon = dot(USER_COLOR, 34)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }.also(map.overlays::add)
            }

            frameCamera(map, conferences, libraries, user, focusConferenceId)
            map.invalidate()
        }
    )
}

private fun createMapView(context: Context): MapView {
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        userAgentValue = context.packageName
        // Keep tiles in app-private cache so no storage permission is needed.
        osmdroidBasePath = File(context.cacheDir, "osmdroid")
        osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
    }
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        minZoomLevel = 2.0
        controller.setZoom(3.0)
        controller.setCenter(GeoPoint(20.0, 30.0))
    }
}

private fun dot(color: Int, sizePx: Int) = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(color)
    setStroke(5, AndroidColor.WHITE)
    setSize(sizePx, sizePx)
}

/** Re-frames only when the inputs that matter change, so panning isn't undone on each recomposition. */
private fun frameCamera(
    map: MapView,
    conferences: List<Conference>,
    libraries: List<NearbyPlace>,
    user: LatLng?,
    focusId: String?
) {
    val signature = "$focusId|${user?.lat},${user?.lng}|${conferences.size}|${libraries.size}"
    if (map.tag == signature) return
    map.tag = signature

    val focus = conferences.firstOrNull { it.id == focusId && it.hasLocation }
    if (focus != null) {
        map.controller.animateTo(GeoPoint(focus.lat, focus.lng), 7.0, 600L)
        return
    }

    val points = buildList {
        user?.let { add(GeoPoint(it.lat, it.lng)) }
        // User + their 3 nearest conferences (list is already distance-sorted when user is known).
        conferences.filter { it.hasLocation }.take(if (user != null) 3 else conferences.size)
            .forEach { add(GeoPoint(it.lat, it.lng)) }
    }
    if (points.size >= 2) {
        val box = BoundingBox.fromGeoPoints(points)
        map.post { map.zoomToBoundingBox(box, true, 140) }
    } else if (points.size == 1) {
        map.controller.setZoom(8.0)
        map.controller.setCenter(points.first())
    }
}
