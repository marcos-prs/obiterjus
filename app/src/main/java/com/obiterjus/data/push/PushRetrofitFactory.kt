package com.obiterjus.data.push

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object PushRetrofitFactory {
    private val json = Json {
        ignoreUnknownKeys = true
        // Mantém o `consulta: null` fora do corpo quando não há autorização.
        explicitNulls = false
    }

    fun createApi(): PushRegistroDataSource {
        val client = OkHttpClient.Builder()
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PushRegistroDataSource::class.java)
    }

    // Mesmo host da API Obiter-Minuta: Oracle Cloud Always Free + Caddy/HTTPS,
    // sempre ligado, o que é o requisito do cron horário.
    private const val BASE_URL = "https://obiterjus.duckdns.org/"

    // O registro roda na abertura do app e não deve atrasar nada: timeout curto,
    // e a falha é silenciosa — tenta de novo na próxima abertura.
    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val CALL_TIMEOUT_SECONDS = 20L
}
