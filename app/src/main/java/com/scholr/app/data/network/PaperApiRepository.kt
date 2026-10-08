package com.scholr.app.data.network

import android.util.Log
import com.scholr.app.data.model.Paper
import retrofit2.HttpException

class PaperApiException(message: String, cause: Throwable) : Exception(message, cause)

/** Live academic-paper search and lookup, backed by the OpenAlex API. */
class PaperApiRepository(private val api: OpenAlexApi = NetworkModule.openAlexApi) {

    suspend fun search(query: String, limit: Int = 25): List<Paper> {
        if (query.isBlank()) return emptyList()
        return try {
            api.search(query = query, perPage = limit).results
                ?.mapNotNull { it.toDomain() }
                .orEmpty()
        } catch (e: HttpException) {
            Log.e("PaperApiRepository", "Search failed: ${e.code()} ${e.response()?.errorBody()?.string()}", e)
            val message = if (e.code() == 429) {
                "The paper search API is rate-limited right now — try again in a minute."
            } else {
                "The paper search service returned an error (${e.code()})."
            }
            throw PaperApiException(message, e)
        } catch (e: Exception) {
            Log.e("PaperApiRepository", "Search failed", e)
            throw PaperApiException("Couldn't reach the paper search service.", e)
        }
    }

    suspend fun getPaper(id: String): Paper? =
        try {
            api.getWork(id).toDomain()
        } catch (e: Exception) {
            Log.e("PaperApiRepository", "getPaper($id) failed", e)
            null
        }
}
