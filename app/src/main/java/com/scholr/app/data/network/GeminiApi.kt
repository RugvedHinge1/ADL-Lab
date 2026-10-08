package com.scholr.app.data.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/** Google's Gemini generateContent REST API. */
interface GeminiApi {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/"

        /** Google's self-updating alias for their current fastest/lowest-latency model —
         *  avoids hardcoding a dated model id that later gets retired. */
        const val MODEL = "gemini-flash-lite-latest"
    }
}
