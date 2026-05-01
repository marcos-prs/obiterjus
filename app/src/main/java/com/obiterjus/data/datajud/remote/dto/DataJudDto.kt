package com.obiterjus.data.datajud.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class DataJudSearchRequestDto(
    val size: Int = 1,
    val query: DataJudQueryDto,
) {
    companion object {
        fun byNumeroProcesso(numeroProcesso: String): DataJudSearchRequestDto =
            DataJudSearchRequestDto(
                query = DataJudQueryDto(
                    match = DataJudMatchDto(numeroProcesso = numeroProcesso),
                ),
            )
    }
}

@Serializable
data class DataJudQueryDto(
    val match: DataJudMatchDto,
)

@Serializable
data class DataJudMatchDto(
    val numeroProcesso: String,
)

@Serializable
data class DataJudSearchResponseDto(
    val took: Int? = null,
    @SerialName("timed_out")
    val timedOut: Boolean? = null,
    val hits: DataJudHitsDto? = null,
)

@Serializable
data class DataJudHitsDto(
    val total: DataJudTotalDto? = null,
    val hits: List<DataJudHitDto> = emptyList(),
)

@Serializable
data class DataJudTotalDto(
    val value: Int? = null,
    val relation: String? = null,
)

@Serializable
data class DataJudHitDto(
    @SerialName("_index")
    val index: String? = null,
    @SerialName("_id")
    val id: String? = null,
    @SerialName("_source")
    val source: DataJudProcessoDto? = null,
)

@Serializable
data class DataJudProcessoDto(
    val numeroProcesso: String? = null,
    val tribunal: String? = null,
    val grau: String? = null,
    val classe: DataJudCodigoNomeDto? = null,
    val orgaoJulgador: DataJudOrgaoJulgadorDto? = null,
    val assuntos: List<DataJudCodigoNomeDto> = emptyList(),
    val movimentos: List<DataJudMovimentoDto> = emptyList(),
    val polos: List<DataJudPoloDto> = emptyList(),
    val nivelSigilo: Int? = null,
    val dataAjuizamento: String? = null,
)

@Serializable
data class DataJudCodigoNomeDto(
    val codigo: Int? = null,
    val nome: String? = null,
)

@Serializable
data class DataJudOrgaoJulgadorDto(
    val codigo: Int? = null,
    val nome: String? = null,
)

@Serializable
data class DataJudMovimentoDto(
    val codigo: Int? = null,
    val nome: String? = null,
    val dataHora: String? = null,
    val complementosTabelados: List<JsonElement> = emptyList(),
)

@Serializable
data class DataJudPoloDto(
    val polo: String? = null,
    val partes: List<DataJudParteDto> = emptyList(),
)

@Serializable
data class DataJudParteDto(
    val tipoParticipacao: String? = null,
    val pessoa: DataJudPessoaDto? = null,
)

@Serializable
data class DataJudPessoaDto(
    val nome: String? = null,
    val tipoPessoa: String? = null,
)
