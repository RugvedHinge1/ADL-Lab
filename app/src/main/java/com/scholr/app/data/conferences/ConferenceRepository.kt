package com.scholr.app.data.conferences

import android.content.Context
import android.util.Log
import com.scholr.app.data.model.Conference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Serializable
private data class ConferenceCache(val fetchedAt: Long, val items: List<Conference>)

/**
 * Live conference data, no bundled samples:
 *  - CCF Deadlines (ranked CS conferences, with real dates / places / deadlines)
 *  - WikiCFP topic listings for the non-CS fields.
 * Merged, filtered to upcoming events, and cached on disk for 24 hours.
 */
class ConferenceRepository(context: Context) {

    private val cacheDir = context.applicationContext.cacheDir
    private val cacheFile = File(cacheDir, "conferences-cache.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    suspend fun load(forceRefresh: Boolean = false): List<Conference> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val cached = readCache(cacheFile)

        if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MS) {
            return@withContext cached.items.prepare(today)
        }

        try {
            val fresh = fetchAll(today)
            if (fresh.isEmpty()) error("No conferences returned")
            writeCache(cacheFile, ConferenceCache(System.currentTimeMillis(), fresh))
            fresh.prepare(today)
        } catch (e: Exception) {
            Log.w(TAG, "conference fetch failed", e)
            // Stale data beats an empty screen.
            cached?.items?.prepare(today) ?: throw e
        }
    }

    /** Conferences listed for one country (venue city + date), cached per country for 24 h. */
    suspend fun loadCountry(country: String): List<Conference> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val slug = country.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        if (slug.isEmpty()) return@withContext emptyList()
        val file = File(cacheDir, "conferences-country-$slug.json")
        val cached = readCache(file)
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MS) {
            return@withContext cached.items.prepare(today)
        }
        try {
            val fresh = ConferenceIndexParser.parse(get(ConferenceIndexParser.url(country)), today)
            writeCache(file, ConferenceCache(System.currentTimeMillis(), fresh))
            fresh.prepare(today)
        } catch (e: Exception) {
            Log.w(TAG, "country listing failed for $country", e)
            cached?.items?.prepare(today) ?: emptyList()
        }
    }

    private suspend fun fetchAll(today: LocalDate): List<Conference> {
        val all = LinkedHashMap<String, Conference>()

        // Primary source: one request. If it fails we still try WikiCFP below.
        var ccfOk = true
        try {
            CcfDeadlinesParser.parse(get(CcfDeadlinesParser.URL), today).forEach { all[it.id] = it }
        } catch (e: Exception) {
            ccfOk = false
            Log.w(TAG, "CCF deadlines failed", e)
        }

        var wikiOk = 0
        for ((topic, field) in WikiCfpParser.topics) {
            try {
                WikiCfpParser.parse(get(WikiCfpParser.url(topic)), field, today).forEach { c ->
                    // Same event listed under several topics: keep the first.
                    if (all.values.none { it.acronym.equals(c.acronym, true) && it.startEpochDay == c.startEpochDay }) {
                        all.putIfAbsent(c.id, c)
                    }
                }
                wikiOk++
            } catch (e: Exception) {
                Log.w(TAG, "WikiCFP '$topic' failed", e)
            }
            delay(WIKI_GAP_MS) // be polite to a volunteer-run site
        }

        if (!ccfOk && wikiOk == 0) error("All conference sources failed")
        return all.values.sortedBy { it.startEpochDay }
    }

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) Scholr/1.0 academic-discovery-app")
            .header("Accept", "text/html,application/x-yaml,*/*")
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code} for $url" }
            return decode(response.body?.bytes() ?: ByteArray(0))
        }
    }

    /** UTF-8 if the bytes are valid UTF-8, otherwise Latin-1 (WikiCFP is inconsistent). */
    private fun decode(bytes: ByteArray): String = try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
    } catch (e: CharacterCodingException) {
        String(bytes, Charsets.ISO_8859_1)
    }

    private fun List<Conference>.prepare(today: LocalDate): List<Conference> =
        filter { it.startEpochDay >= today.toEpochDay() }
            .map { it.withDeadlineRelativeTo(today) }
            .sortedBy { it.startEpochDay }

    private fun readCache(file: File): ConferenceCache? = runCatching {
        if (file.exists()) json.decodeFromString<ConferenceCache>(file.readText()) else null
    }.getOrNull()

    private fun writeCache(file: File, cache: ConferenceCache) {
        runCatching { file.writeText(json.encodeToString(ConferenceCache.serializer(), cache)) }
    }

    private companion object {
        const val TAG = "ConferenceRepository"
        const val CACHE_TTL_MS = 24L * 60 * 60 * 1000
        const val WIKI_GAP_MS = 700L
    }
}
