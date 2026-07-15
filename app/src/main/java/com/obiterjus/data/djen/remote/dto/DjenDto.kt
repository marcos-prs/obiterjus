@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.obiterjus.data.djen.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.Serializable

@Serializable
data class DjenResponseDto(
    val status: String? = null,
    val message: String? = null,
    val count: Int? = null,
    @JsonNames("comunicacoes")
    val items: List<DjenComunicacaoDto> = emptyList(),
)

@Serializable
data class DjenComunicacaoDto(
    val id: Long,
    val hash: String? = null,
    @SerialName("data_disponibilizacao")
    @JsonNames("dataDisponibilizacao")
    val dataDisponibilizacaoIso: String? = null,
    val siglaTribunal: String? = null,
    val tipoComunicacao: String? = null,
    val nomeOrgao: String? = null,
    val idOrgao: Long? = null,
    val texto: String? = null,
    @SerialName("numero_processo")
    @JsonNames("numeroProcesso")
    val numeroProcesso: String? = null,
    val ativo: Boolean? = null,
    val destinatarios: List<DjenDestinatarioDto> = emptyList(),
    @SerialName("destinatarioadvogados")
    @JsonNames("destinatarioAdvogados")
    val destinatarioAdvogados: List<DjenDestinatarioAdvogadoDto> = emptyList(),
)

@Serializable
data class DjenDestinatarioDto(
    val nome: String? = null,
    val polo: String? = null,
)

@Serializable
data class DjenDestinatarioAdvogadoDto(
    val advogado: DjenAdvogadoDto? = null,
)

@Serializable
data class DjenAdvogadoDto(
    val nome: String? = null,
    @SerialName("numero_oab")
    @JsonNames("numeroOab")
    val numeroOab: String? = null,
    @SerialName("uf_oab")
    @JsonNames("ufOab")
    val ufOab: String? = null,
)
