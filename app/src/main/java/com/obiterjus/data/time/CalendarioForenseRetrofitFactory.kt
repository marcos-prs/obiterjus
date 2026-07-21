package com.obiterjus.data.time

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object CalendarioForenseRetrofitFactory {
    // encodeDefaults é obrigatório: a API (FastAPI/Pydantic) exige campos como
    // origem, classe e termo_inicial mesmo quando têm o valor padrão do DTO —
    // sem isso toda chamada é rejeitada com HTTP 422.
    internal val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun createApi(): CalendarioForenseDataSource {
        val client = OkHttpClient.Builder()
            .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CalendarioForenseDataSource::class.java)
    }

    private const val BASE_URL = "https://calendario-forense-br.onrender.com/"

    // O plano free do Render hiberna o serviço; o cold start pode passar de
    // 30s. Timeout curto derrubava a primeira chamada do dia.
    private const val TIMEOUT_SECONDS = 45L
}
