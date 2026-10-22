package com.scholr.app.data.scan

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** A book resolved from its ISBN. */
data class Book(
    val isbn13: String,
    val title: String,
    val authors: List<String>,
    val categories: List<String>,
    val description: String,
    val publisher: String,
    val year: String
)

/**
 * ISBN → book metadata. Google Books first (richest, keyless); Open Library as
 * a fallback for titles Google doesn't know. Both are free and need no key.
 */
class BookRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun lookup(isbn13: String): Book? = withContext(Dispatchers.IO) {
        fromGoogleBooks(isbn13) ?: fromOpenLibrary(isbn13)
    }

    private fun get(url: String): JsonElement? = try {
        client.newCall(Request.Builder().url(url).header("Accept", "application/json").build())
            .execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.string()?.let { json.parseToJsonElement(it) }
            }
    } catch (e: Exception) {
        Log.w(TAG, "GET $url failed", e)
        null
    }

    private fun fromGoogleBooks(isbn: String): Book? {
        val root = get("https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn&maxResults=1")
            as? JsonObject ?: return null
        val info = ((root["items"] as? JsonArray)?.firstOrNull() as? JsonObject)
            ?.get("volumeInfo") as? JsonObject ?: return null
        val title = info.str("title") ?: return null
        return Book(
            isbn13 = isbn,
            title = title,
            authors = info.strList("authors"),
            categories = info.strList("categories").flatMap { it.split('/') }.map { it.trim() }
                .filter { it.isNotEmpty() }.distinct(),
            description = info.str("description") ?: info.str("subtitle").orEmpty(),
            publisher = info.str("publisher").orEmpty(),
            year = info.str("publishedDate")?.take(4).orEmpty()
        )
    }

    private fun fromOpenLibrary(isbn: String): Book? {
        val root = get("https://openlibrary.org/api/books?bibkeys=ISBN:$isbn&format=json&jscmd=data")
            as? JsonObject ?: return null
        val data = root["ISBN:$isbn"] as? JsonObject ?: return null
        val title = data.str("title") ?: return null
        return Book(
            isbn13 = isbn,
            title = title,
            authors = data["authors"].namedList(),
            categories = data["subjects"].namedList().take(5),
            description = data.str("subtitle").orEmpty(),
            publisher = data["publishers"].namedList().firstOrNull().orEmpty(),
            year = data.str("publish_date")?.takeLast(4).orEmpty()
        )
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.strList(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()

    /** Open Library lists are arrays of `{ "name": "…" }` objects. */
    private fun JsonElement?.namedList(): List<String> =
        (this as? JsonArray)?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }.orEmpty()

    private companion object {
        const val TAG = "BookRepository"
    }
}
