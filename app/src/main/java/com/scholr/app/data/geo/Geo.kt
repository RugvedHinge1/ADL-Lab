package com.scholr.app.data.geo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLng(val lat: Double, val lng: Double)

/** Great-circle distance in km (haversine formula). */
fun haversineKm(a: LatLng, b: LatLng): Double {
    val r = 6371.0088
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
    return 2 * r * asin(sqrt(h))
}

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** Current device position, or null if unavailable. Caller must hold a location permission. */
@SuppressLint("MissingPermission")
suspend fun currentLocation(context: Context): LatLng? {
    if (!hasLocationPermission(context)) return null
    return try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val fresh = client.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            CancellationTokenSource().token
        ).await()
        val loc = fresh ?: client.lastLocation.await()
        loc?.let { LatLng(it.latitude, it.longitude) }
    } catch (e: Exception) {
        null
    }
}

// ── Nearby libraries (OpenStreetMap Overpass API — free, no key) ─────────────

data class NearbyPlace(val name: String, val position: LatLng, val distanceKm: Double)

@Serializable
private data class OverpassResponse(val elements: List<OverpassElement> = emptyList())

@Serializable
private data class OverpassElement(
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String> = emptyMap()
)

@Serializable
private data class OverpassCenter(val lat: Double, val lon: Double)

object LibrariesRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    /** Libraries within [radiusMeters] of [origin], nearest first. Throws on network failure. */
    suspend fun nearby(origin: LatLng, radiusMeters: Int = 8000, max: Int = 30): List<NearbyPlace> =
        withContext(Dispatchers.IO) {
            val query = "[out:json][timeout:20];" +
                "nwr[\"amenity\"=\"library\"](around:$radiusMeters,${origin.lat},${origin.lng});" +
                "out center $max;"
            val request = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .header("User-Agent", "Scholr/1.0 (academic discovery app)")
                .post(okhttp3.FormBody.Builder().add("data", query).build())
                .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Overpass error ${response.code}" }
                val body = response.body?.string().orEmpty()
                json.decodeFromString<OverpassResponse>(body).elements.mapNotNull { e ->
                    val lat = e.lat ?: e.center?.lat ?: return@mapNotNull null
                    val lon = e.lon ?: e.center?.lon ?: return@mapNotNull null
                    val pos = LatLng(lat, lon)
                    NearbyPlace(
                        name = e.tags["name"] ?: "Library",
                        position = pos,
                        distanceKm = haversineKm(origin, pos)
                    )
                }.sortedBy { it.distanceKm }
            }
        }
}

/**
 * Opens turn-by-turn directions to a place in Google Maps (falls back to the
 * browser version if the app isn't installed). Uses coordinates when known,
 * otherwise the venue text. Maps starts from the user's current location.
 */
fun openDirections(context: Context, label: String, lat: Double?, lng: Double?, fallbackQuery: String) {
    val destination = if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
        "$lat,$lng"
    } else fallbackQuery
    val uri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destination) +
            "&travelmode=driving"
    )
    val mapsApp = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(mapsApp)
    } catch (e: Exception) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
