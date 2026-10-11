package com.scholr.app.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Semantic Scholar Academic Graph + Recommendations APIs. Authenticated with
 * an `x-api-key` header (added in [NetworkModule]). Docs: https://api.semanticscholar.org
 */
interface SemanticScholarApi {

    @GET("graph/v1/paper/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20,
        @Query("fields") fields: String = FIELDS_WITH_EMBEDDING
    ): S2SearchResponse

    @GET("graph/v1/paper/{id}")
    suspend fun getPaper(
        @Path("id") id: String,
        @Query("fields") fields: String = FIELDS_WITH_EMBEDDING
    ): S2PaperDto

    /** Hydrate many papers in one call (max 500 ids). Unknown ids come back as null. */
    @POST("graph/v1/paper/batch")
    suspend fun batch(
        @Query("fields") fields: String,
        @Body body: S2BatchRequest
    ): List<S2PaperDto?>

    /** Papers similar to a set of positive (and optional negative) seed papers. */
    @POST("recommendations/v1/papers/")
    suspend fun recommend(
        @Query("limit") limit: Int,
        @Query("fields") fields: String,
        @Body body: S2RecommendRequest
    ): S2RecommendResponse

    companion object {
        const val BASE_URL = "https://api.semanticscholar.org/"
        const val FIELDS = "paperId,title,abstract,venue,year,citationCount,isOpenAccess," +
            "fieldsOfStudy,authors.name"
        const val FIELDS_WITH_EMBEDDING = "$FIELDS,embedding.specter_v2"
    }
}
