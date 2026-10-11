package com.scholr.app.data.network

import android.util.Log
import com.scholr.app.data.model.Paper
import retrofit2.HttpException

class PaperApiException(message: String, cause: Throwable) : Exception(message, cause)

/**
 * Live academic-paper search and lookup. Semantic Scholar is the primary
 * source (authenticated, and the only one that serves document embeddings
 * for the recommender); OpenAlex is kept as a keyless fallback so the app
 * still works if S2 is rate-limited or a bookmark predates the switch.
 */
class PaperApiRepository(
    private val s2: SemanticScholarApi = NetworkModule.semanticScholarApi,
    private val openAlex: OpenAlexApi = NetworkModule.openAlexApi
) {

    suspend fun search(query: String, limit: Int = 25): List<Paper> {
        if (query.isBlank()) return emptyList()
        return try {
            searchEmbedded(query, limit).map { it.paper }
        } catch (s2Error: Exception) {
            Log.w(TAG, "S2 search failed, falling back to OpenAlex", s2Error)
            searchOpenAlex(query, limit)
        }
    }

    /** Search results with their SPECTER2 vectors, for the recommender. Throws on failure. */
    suspend fun searchEmbedded(query: String, limit: Int): List<EmbeddedPaper> =
        s2.search(query = query, limit = limit).data.orEmpty().mapNotNull { it.toEmbedded() }

    suspend fun getPaper(id: String): Paper? =
        try {
            if (isSemanticScholarId(id)) s2.getPaper(id, SemanticScholarApi.FIELDS).toDomain()
            else openAlex.getWork(id).toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "getPaper($id) failed", e)
            null
        }

    /** Hydrate S2 papers (with embeddings) by id. Non-S2 ids are skipped. */
    suspend fun embeddedByIds(ids: List<String>): List<EmbeddedPaper> {
        val s2Ids = ids.filter(::isSemanticScholarId).distinct().take(MAX_BATCH)
        if (s2Ids.isEmpty()) return emptyList()
        return s2.batch(SemanticScholarApi.FIELDS_WITH_EMBEDDING, S2BatchRequest(s2Ids))
            .mapNotNull { it?.toEmbedded() }
    }

    /** Ids of papers S2's own recommender considers similar to [seedIds]. */
    suspend fun similarPaperIds(seedIds: List<String>, limit: Int): List<String> {
        val seeds = seedIds.filter(::isSemanticScholarId).distinct().take(MAX_SEEDS)
        if (seeds.isEmpty()) return emptyList()
        return s2.recommend(limit, "paperId", S2RecommendRequest(positivePaperIds = seeds))
            .recommendedPapers.orEmpty().mapNotNull { it.paperId }
    }

    private suspend fun searchOpenAlex(query: String, limit: Int): List<Paper> =
        try {
            openAlex.search(query = query, perPage = limit).results
                ?.mapNotNull { it.toDomain() }
                .orEmpty()
        } catch (e: HttpException) {
            Log.e(TAG, "Search failed: ${e.code()} ${e.response()?.errorBody()?.string()}", e)
            val message = if (e.code() == 429) {
                "The paper search API is rate-limited right now — try again in a minute."
            } else {
                "The paper search service returned an error (${e.code()})."
            }
            throw PaperApiException(message, e)
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            throw PaperApiException("Couldn't reach the paper search service.", e)
        }

    private companion object {
        const val TAG = "PaperApiRepository"
        const val MAX_BATCH = 100
        const val MAX_SEEDS = 20
    }
}
