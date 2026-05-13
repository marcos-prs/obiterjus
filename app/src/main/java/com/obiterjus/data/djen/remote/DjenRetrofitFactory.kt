package com.obiterjus.data.djen.remote

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object DjenRetrofitFactory {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun createApi(
        baseUrl: String,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        okHttpClient: OkHttpClient? = null,
    ): DjenApi {
        val client = okHttpClient ?: OkHttpClient.Builder()
            .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl.withApiV1BasePath())
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DjenApi::class.java)
    }

    private fun String.withApiV1BasePath(): String {
        val trimmed = trim().trimEnd('/')
        val apiBase = if (trimmed.endsWith("/api/v1", ignoreCase = true)) {
            trimmed
        } else {
            "$trimmed/api/v1"
        }
        return "$apiBase/"
    }

    private const val DEFAULT_TIMEOUT_SECONDS = 30L
}
