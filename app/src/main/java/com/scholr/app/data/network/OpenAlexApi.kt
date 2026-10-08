package com.scholr.app.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * OpenAlex — free, no API key required. Docs: https://docs.openalex.org
 * We identify ourselves via a `mailto` param on every request (added by
 * [NetworkModule]) to get the higher-throughput "polite pool" rate limit.
 */
interface OpenAlexApi {

    @GET("works")
    suspend fun search(
        @Query("search") query: String,
        @Query("per-page") perPage: Int = 25,
        @Query("select") select: String = FIELDS
    ): OpenAlexSearchResponse

    @GET("works/{id}")
    suspend fun getWork(
        @Path("id") id: String,
        @Query("select") select: String = FIELDS
    ): OpenAlexWorkDto

    companion object {
        const val BASE_URL = "https://api.openalex.org/"
        const val FIELDS = "id,title,publication_year,cited_by_count,authorships," +
            "primary_location,open_access,abstract_inverted_index,concepts"
    }
}
