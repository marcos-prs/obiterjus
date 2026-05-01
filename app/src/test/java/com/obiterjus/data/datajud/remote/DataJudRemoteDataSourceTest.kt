package com.obiterjus.data.datajud.remote

import com.obiterjus.data.datajud.remote.dto.DataJudHitDto
import com.obiterjus.data.datajud.remote.dto.DataJudHitsDto
import com.obiterjus.data.datajud.remote.dto.DataJudProcessoDto
import com.obiterjus.data.datajud.remote.dto.DataJudSearchRequestDto
import com.obiterjus.data.datajud.remote.dto.DataJudSearchResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DataJudRemoteDataSourceTest {
    @Test
    fun searchesResolvedTribunalIndexWithApiKeyHeader() = runBlocking {
        val api = FakeDataJudApi(
            response = DataJudSearchResponseDto(
                hits = DataJudHitsDto(
                    hits = listOf(
                        DataJudHitDto(
                            index = "api_publica_tjmg",
                            source = DataJudProcessoDto(
                                numeroProcesso = "50110879520258130245",
                                tribunal = "TJMG",
                            ),
                        ),
                    ),
                ),
            ),
        )
        val dataSource = DataJudRemoteDataSource(
            api = api,
            apiKey = "secret",
        )

        val result = dataSource.buscarProcesso(
            numeroProcesso = "5011087-95.2025.8.13.0245",
            tribunal = "tj-mg",
        )

        assertEquals("api_publica_tjmg/_search", api.lastEndpoint)
        assertEquals("APIKey secret", api.lastAuthorization)
        assertEquals("50110879520258130245", api.lastRequest?.query?.match?.numeroProcesso)
        assertEquals("TJMG", result.tribunal)
        assertEquals("api_publica_tjmg", result.indexName)
        assertEquals("TJMG", result.processo?.tribunal)
    }

    @Test(expected = UnknownDataJudTribunalException::class)
    fun rejectsUnknownTribunalWhenItCannotInferFromNumber() {
        runBlocking {
            DataJudRemoteDataSource(
                api = FakeDataJudApi(DataJudSearchResponseDto()),
                apiKey = "secret",
            ).buscarProcesso(
                numeroProcesso = "00000000020269000000",
                tribunal = "desconhecido",
            )
        }
    }

    private class FakeDataJudApi(
        private val response: DataJudSearchResponseDto,
    ) : DataJudApi {
        var lastEndpoint: String? = null
        var lastAuthorization: String? = null
        var lastRequest: DataJudSearchRequestDto? = null

        override suspend fun buscarProcesso(
            endpoint: String,
            authorization: String,
            request: DataJudSearchRequestDto,
        ): DataJudSearchResponseDto {
            lastEndpoint = endpoint
            lastAuthorization = authorization
            lastRequest = request
            return response
        }
    }
}
