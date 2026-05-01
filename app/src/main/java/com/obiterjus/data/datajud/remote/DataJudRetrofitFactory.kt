package com.obiterjus.data.datajud.remote

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object DataJudRetrofitFactory {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun createApi(
        baseUrl: String = DEFAULT_BASE_URL,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        okHttpClient: OkHttpClient? = null,
    ): DataJudApi {
        val client = okHttpClient ?: OkHttpClient.Builder()
            .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl.withTrailingSlash())
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DataJudApi::class.java)
    }

    private fun String.withTrailingSlash(): String =
        if (endsWith("/")) this else "$this/"

    private const val DEFAULT_BASE_URL = "https://api-publica.datajud.cnj.jus.br/"
    private const val DEFAULT_TIMEOUT_SECONDS = 30L
}
