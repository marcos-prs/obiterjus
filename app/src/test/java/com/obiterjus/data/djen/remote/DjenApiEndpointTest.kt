package com.obiterjus.data.djen.remote

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class DjenApiEndpointTest {
    @Test
    fun usesOfficialApiV1PathsWithoutDuplicatingPrefix() = runBlocking {
        val requestedPaths = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val path = chain.request().url.encodedPath
                requestedPaths += path

                val body = when (path) {
                    "/api/v1/comunicacao" ->
                        """{"count":0,"items":[]}""".toResponseBody("application/json".toMediaType())
                    "/api/v1/comunicacao/abc/certidao" ->
                        "pdf".toResponseBody("application/pdf".toMediaType())
                    else -> error("Unexpected path: $path")
                }

                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body)
                    .build()
            }
            .build()

        val api = DjenRetrofitFactory.createApi(
            baseUrl = "https://comunicaapi.pje.jus.br/api/v1",
            okHttpClient = client,
        )

        val resposta = api.buscarComunicacoes(
            numeroOab = "123",
            ufOab = "MG",
            dataDisponibilizacaoInicio = "2026-05-06",
            dataDisponibilizacaoFim = "2026-05-06",
            pagina = 1,
            itensPorPagina = 1,
        )

        assertEquals(0, resposta.count ?: -1)

        api.baixarCertidao("abc")

        assertEquals(
            listOf(
                "/api/v1/comunicacao",
                "/api/v1/comunicacao/abc/certidao",
            ),
            requestedPaths,
        )
    }

    @Test
    fun addsOfficialApiV1PathWhenRemoteConfigUsesHostRoot() = runBlocking {
        val requestedPaths = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestedPaths += chain.request().url.encodedPath

                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"count":0,"items":[]}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val api = DjenRetrofitFactory.createApi(
            baseUrl = "https://comunicaapi.pje.jus.br/",
            okHttpClient = client,
        )

        api.buscarComunicacoes(
            numeroOab = "123",
            ufOab = "MG",
            dataDisponibilizacaoInicio = "2026-05-06",
            dataDisponibilizacaoFim = "2026-05-06",
            pagina = 1,
            itensPorPagina = 1,
        )

        assertEquals(listOf("/api/v1/comunicacao"), requestedPaths)
    }
}
