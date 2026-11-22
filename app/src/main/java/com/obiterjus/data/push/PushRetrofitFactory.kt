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

    // Host do emissor. Primeira escolha, em 13/11/2026, foi
    // `push.obiterjus.duckdns.org` — **errada**: o DuckDNS responde qualquer
    // subdomínio, mas todos com o IP do domínio base, que é a VM da API de
    // minutas (157.151.31.34). O emissor roda em máquina própria
    // (157.151.26.150), então o nome tem de ser um domínio DuckDNS dele.
    //
    // Um nome por máquina, e não `/push` na VM de minutas repassando para cá:
    // aquilo devolveria o acoplamento que motivou a máquina separada.
    //
    // Fica compilado no APK, então trocar exige build e envio novos. O nome tem
    // de estar roteado antes de `push_pre_checagem_disponivel` ligar: sem rota,
    // o registro falha em silêncio, que é o comportamento desenhado abaixo. Na
    // Fase 1 ninguém chama este host — o emissor só fala com o FCM.
    private const val BASE_URL = "https://obiterpush.duckdns.org/"

    // O registro roda na abertura do app e não deve atrasar nada: timeout curto,
    // e a falha é silenciosa — tenta de novo na próxima abertura.
    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val CALL_TIMEOUT_SECONDS = 20L
}
