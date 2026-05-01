package com.obiterjus.data.datajud.remote

import com.obiterjus.core.parser.NumeroProcessoNormalizer
import com.obiterjus.data.datajud.remote.dto.DataJudProcessoDto
import com.obiterjus.data.datajud.remote.dto.DataJudSearchRequestDto

class DataJudRemoteDataSource(
    private val api: DataJudApi,
    private val apiKey: String,
    private val tribunalResolver: DataJudTribunalResolver = DataJudTribunalResolver(),
) {
    suspend fun buscarProcesso(
        numeroProcesso: String,
        tribunal: String?,
    ): DataJudProcessoRemoteResult {
        val normalizedNumber = NumeroProcessoNormalizer.normalize(numeroProcesso)
            ?: throw IllegalArgumentException("Número processual inválido: $numeroProcesso")
        val resolvedTribunal = tribunalResolver.resolve(
            tribunal = tribunal,
            numeroProcesso = normalizedNumber,
        ) ?: throw UnknownDataJudTribunalException(tribunal, normalizedNumber)

        val response = api.buscarProcesso(
            endpoint = "${resolvedTribunal.indexName}/_search",
            authorization = "APIKey ${apiKey.trim()}",
            request = DataJudSearchRequestDto.byNumeroProcesso(normalizedNumber),
        )
        val source = response.hits
            ?.hits
            ?.firstOrNull { hit ->
                NumeroProcessoNormalizer.normalize(hit.source?.numeroProcesso) == normalizedNumber
            }
            ?.source
            ?: response.hits?.hits?.firstOrNull()?.source

        return DataJudProcessoRemoteResult(
            numeroProcesso = normalizedNumber,
            tribunal = resolvedTribunal.tribunal,
            indexName = resolvedTribunal.indexName,
            processo = source,
        )
    }
}

data class DataJudProcessoRemoteResult(
    val numeroProcesso: String,
    val tribunal: String,
    val indexName: String,
    val processo: DataJudProcessoDto?,
)

class UnknownDataJudTribunalException(
    tribunal: String?,
    numeroProcesso: String,
) : IllegalArgumentException(
    "Não foi possível resolver o índice DataJud para tribunal='$tribunal' e processo='$numeroProcesso'.",
)
