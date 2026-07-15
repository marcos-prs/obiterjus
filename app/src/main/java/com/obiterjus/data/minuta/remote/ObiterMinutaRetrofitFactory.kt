package com.obiterjus.data.minuta.remote

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
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
     * [tokenProvider] fornece o Firebase ID token do usuário autenticado;
     * a API valida o token com audience no project id do Firebase.
     */
    fun createApi(tokenProvider: suspend () -> String?): ObiterMinutaDataSource {
        val client = OkHttpClient.Builder()
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = runBlocking { tokenProvider() }
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ObiterMinutaDataSource::class.java)
    }

    private const val BASE_URL = "https://obiter-minuta-api.onrender.com/"

    // Cold start do Render free (~50s) + geração de sugestão via IA (10–45s)
    private const val CONNECT_TIMEOUT_SECONDS = 90L
    private const val CALL_TIMEOUT_SECONDS = 180L
}
