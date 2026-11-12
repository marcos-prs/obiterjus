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

    // PENDENTE, e de propósito: este é o host da API de minutas, não do emissor
    // do push. O emissor (Obiter-Push) roda na mesma VM mas não tem porta nem
    // hostname na Fase 1 — só os ganha quando a Fase 2 existir.
    //
    // Nenhum APK publicado chega aqui: a Fase 2 está atrás de
    // `push_pre_checagem_disponivel`, que nasce false. Então trocar esta
    // constante é grátis HOJE. Depois de um APK publicado apontar para cá,
    // mudar passa a depender de o usuário atualizar o app — por isso o host da
    // Fase 2 tem de ser decidido ANTES de ligar aquele flag.
    private const val BASE_URL = "https://obiterjus.duckdns.org/"

    // O registro roda na abertura do app e não deve atrasar nada: timeout curto,
    // e a falha é silenciosa — tenta de novo na próxima abertura.
    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val CALL_TIMEOUT_SECONDS = 20L
}
