package com.obiterjus.data.viacep

import kotlinx.serialization.Serializable

@Serializable
data class ViaCepDto(
    val cep: String? = null,
    val logradouro: String? = null,
    val bairro: String? = null,
    val localidade: String? = null,
    val uf: String? = null,
    val erro: Boolean = false,
)
