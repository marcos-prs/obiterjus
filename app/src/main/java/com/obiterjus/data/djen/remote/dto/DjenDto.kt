package com.obiterjus.data.djen.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DjenResponseDto(
    val status: String? = null,
    val message: String? = null,
    val count: Int? = null,
    val items: List<DjenComunicacaoDto> = emptyList(),
)

@Serializable
data class DjenComunicacaoDto(
    val id: Long,
    val hash: String? = null,
    @SerialName("data_disponibilizacao")
    val dataDisponibilizacaoIso: String? = null,
    val siglaTribunal: String? = null,
    val tipoComunicacao: String? = null,
    val nomeOrgao: String? = null,
    val idOrgao: Long? = null,
    val texto: String? = null,
    @SerialName("numero_processo")
    val numeroProcesso: String? = null,
    val ativo: Boolean? = null,
)
