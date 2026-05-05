package com.obiterjus.domain.model

import java.time.Instant
import java.time.LocalDate

data class OabCadastro(
    val numero: String = "",
    val uf: String = "",
    val nomeAdvogado: String = "",
    val tipoInscricao: String = "",
    val nomeEscritorio: String = "",
    val areasAtuacao: List<String> = emptyList(),
    val dataInicio: LocalDate? = null,
    val dataFim: LocalDate? = null,
) {
    val isValid: Boolean
        get() = nomeAdvogado.isNotBlank() && numero.isNotBlank() && uf.length == 2
}

data class SincronizacaoStatus(
    val ultimaExecucaoEm: Instant? = null,
    val ultimoSucessoEm: Instant? = null,
    val ultimaFalha: String? = null,
    val novasPublicacoesUltimaExecucao: Int = 0,
)
