package com.scholr.app.data.network

import com.scholr.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    /** Retries once, after a short pause, if the server responds 429 — smooths
     *  over transient rate-limit spikes without hammering the API. */
    private class RetryOn429Interceptor(private val delayMs: Long = 1500) : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.code != 429) return response
            response.close()
            Thread.sleep(delayMs)
            return chain.proceed(request)
        }
    }

    /** OpenAlex needs no API key — a `mailto` param just moves us into their
     *  higher-throughput "polite pool" instead of the anonymous default. */
    private val openAlexOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val urlWithMailto = chain.request().url.newBuilder()
                    .addQueryParameter("mailto", "scholr.prototype@example.com")
                    .build()
                chain.proceed(chain.request().newBuilder().url(urlWithMailto).build())
            }
            .addInterceptor(RetryOn429Interceptor())
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val geminiOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val authed = chain.request().newBuilder()
                    .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                    .build()
                chain.proceed(authed)
            }
            .addInterceptor(RetryOn429Interceptor())
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val openAlexApi: OpenAlexApi by lazy {
        Retrofit.Builder()
            .baseUrl(OpenAlexApi.BASE_URL)
            .client(openAlexOkHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenAlexApi::class.java)
    }

    val geminiApi: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(GeminiApi.BASE_URL)
            .client(geminiOkHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApi::class.java)
    }
}
