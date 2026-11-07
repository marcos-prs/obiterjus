package com.obiterjus.data.minuta.remote

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object ObiterMinutaRetrofitFactory {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * O cabeçalho Authorization é parâmetro de cada chamada, resolvido pelo
     * repositório em contexto suspenso. Antes um interceptor buscava o Firebase
     * ID token com runBlocking, travando a thread do dispatcher do OkHttp — e,
     * quando o token precisava de refresh, disparando rede dentro de rede.
     */
    fun createApi(): ObiterMinutaDataSource {
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
            .create(ObiterMinutaDataSource::class.java)
    }

    // Oracle Cloud Always Free + Caddy/HTTPS (não hiberna, sem cold start).
    private const val BASE_URL = "https://obiterjus.duckdns.org/"

    // Sem cold start; folga cobre a geração de sugestão via IA (10–45s).
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val CALL_TIMEOUT_SECONDS = 120L
}
