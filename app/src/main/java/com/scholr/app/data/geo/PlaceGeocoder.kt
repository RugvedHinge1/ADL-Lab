package com.scholr.app.data.geo

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
private data class MeteoResponse(val results: List<MeteoHit> = emptyList())

@Serializable
private data class MeteoHit(
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val population: Long? = null
)

@Serializable
private data class NominatimHit(val lat: String, val lon: String)

@Serializable
private data class NominatimReverse(val address: NominatimAddress? = null)

@Serializable
private data class NominatimAddress(val country: String? = null)

/**
 * Turns a free-text venue ("Vancouver, British Columbia, Canada") into
 * coordinates using free, key-less services: Open-Meteo geocoding first, then
 * OpenStreetMap Nominatim. Results (including misses) are cached on-device so
 * each place is looked up once, ever.
 */
class PlaceGeocoder(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("geocache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val nominatimGate = Mutex()
    private var lastNominatimAt = 0L

    /** Cached result, if this place was already looked up (null inside = known miss). */
    fun cached(place: String): Result<LatLng?>? {
        val v = prefs.getString(key(place), null) ?: return null
        if (v == NONE) return Result.success(null)
        val p = v.split(',')
        return Result.success(LatLng(p[0].toDouble(), p[1].toDouble()))
    }

    /**
     * @param allowFallback when false only the fast Open-Meteo lookup is tried and a
     * miss is *not* cached, so a later call with `true` can still try Nominatim.
     */
    suspend fun resolve(place: String, allowFallback: Boolean = true): LatLng? {
        cached(place)?.let { return it.getOrNull() }
        val found = withContext(Dispatchers.IO) {
            runCatching { openMeteo(place) }.onFailure { Log.w(TAG, "open-meteo failed for '$place'", it) }
                .getOrNull()
                ?: if (allowFallback) {
                    runCatching { nominatim(place) }.onFailure { Log.w(TAG, "nominatim failed for '$place'", it) }
                        .getOrNull()
                } else return@withContext null
        }
        // Only remember a miss when the services answered; network errors retry next time.
        prefs.edit().putString(key(place), found?.let { "${it.lat},${it.lng}" } ?: NONE).apply()
        return found
    }

    /** Country name for coordinates (used to geocode nearby venues first). */
    suspend fun countryOf(point: LatLng): String? = withContext(Dispatchers.IO) {
        runCatching {
            throttleNominatim()
            val url = "https://nominatim.openstreetmap.org/reverse".toHttpUrl().newBuilder()
                .addQueryParameter("lat", point.lat.toString())
                .addQueryParameter("lon", point.lng.toString())
                .addQueryParameter("zoom", "3")
                .addQueryParameter("format", "jsonv2")
                .addQueryParameter("accept-language", "en")
                .build()
            val body = fetch(url.toString()) ?: return@runCatching null
            json.decodeFromString<NominatimReverse>(body).address?.country
        }.getOrNull()
    }

    private fun openMeteo(place: String): LatLng? {
        val parts = place.split(',').map { it.substringBefore('(').trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        val country = if (parts.size > 1) normalizeCountry(parts.last()) else null

        // Venue strings are often "Hotel Name, City, ST, Country" — try the first
        // segment, then the second, which is usually the city.
        val candidates = listOfNotNull(parts[0], parts.getOrNull(1)?.takeIf { parts.size >= 3 }).distinct()
        for (city in candidates) {
            val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
                .addQueryParameter("name", city)
                .addQueryParameter("count", "10")
                .addQueryParameter("language", "en")
                .addQueryParameter("format", "json")
                .build()
            val body = fetch(url.toString()) ?: continue
            val hits = json.decodeFromString<MeteoResponse>(body).results
            val match = if (country != null) {
                hits.filter {
                    normalizeCountry(it.country.orEmpty()) == country ||
                        it.countryCode.equals(parts.last(), ignoreCase = true)
                }
            } else hits
            match.maxByOrNull { it.population ?: 0L }?.let { return LatLng(it.latitude, it.longitude) }
        }
        return null
    }

    private suspend fun nominatim(place: String): LatLng? {
        throttleNominatim()
        val url = "https://nominatim.openstreetmap.org/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", place)
            .addQueryParameter("format", "json")
            .addQueryParameter("limit", "1")
            .build()
        val body = fetch(url.toString()) ?: return null
        val hit = json.decodeFromString<List<NominatimHit>>(body).firstOrNull() ?: return null
        return LatLng(hit.lat.toDouble(), hit.lon.toDouble())
    }

    /** Nominatim's usage policy: max one request per second. */
    private suspend fun throttleNominatim() = nominatimGate.withLock {
        val wait = lastNominatimAt + 1100L - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        lastNominatimAt = System.currentTimeMillis()
    }

    private fun fetch(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Scholr/1.0 (academic discovery app)")
            .build()
        client.newCall(request).execute().use { r ->
            if (!r.isSuccessful) return null
            return r.body?.string()
        }
    }

    private fun key(place: String) = place.trim().lowercase()

    companion object {
        private const val TAG = "PlaceGeocoder"
        private const val NONE = "none"

        private val aliases = mapOf(
            "usa" to "united states", "us" to "united states", "u.s.a." to "united states",
            "united states of america" to "united states",
            "uk" to "united kingdom", "u.k." to "united kingdom", "england" to "united kingdom",
            "scotland" to "united kingdom", "wales" to "united kingdom",
            "uae" to "united arab emirates",
            "korea" to "south korea", "republic of korea" to "south korea",
            "czech republic" to "czechia",
            "russian federation" to "russia",
            "turkiye" to "turkey", "türkiye" to "turkey",
            "republic of türkiye" to "turkey", "republic of turkiye" to "turkey",
            "viet nam" to "vietnam",
            "the netherlands" to "netherlands", "holland" to "netherlands",
            "p.r. china" to "china", "pr china" to "china", "people's republic of china" to "china"
        )

        fun normalizeCountry(raw: String): String {
            val s = raw.trim().lowercase().removeSuffix(".")
            return aliases[s] ?: s
        }
    }
}
