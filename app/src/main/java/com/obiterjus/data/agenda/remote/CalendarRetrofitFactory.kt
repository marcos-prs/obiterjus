package com.obiterjus.data.agenda.remote

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object CalendarRetrofitFactory {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun buildRetrofit(
        baseUrl: String,
        okHttpClient: OkHttpClient,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    fun createGoogleApi(
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        tokenProvider: () -> String? = { null },
    ): GoogleCalendarDataSource {
        val client = OkHttpClient.Builder()
            .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenProvider))
            .build()
        return buildRetrofit(GOOGLE_BASE_URL, client).create(GoogleCalendarDataSource::class.java)
    }

    fun createOutlookApi(
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        tokenProvider: () -> String? = { null },
    ): OutlookCalendarDataSource {
        val client = OkHttpClient.Builder()
            .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenProvider))
            .build()
        return buildRetrofit(OUTLOOK_BASE_URL, client).create(OutlookCalendarDataSource::class.java)
    }


    private const val GOOGLE_BASE_URL = "https://www.googleapis.com/"
    private const val OUTLOOK_BASE_URL = "https://graph.microsoft.com/"
    private const val DEFAULT_TIMEOUT_SECONDS = 30L
}
