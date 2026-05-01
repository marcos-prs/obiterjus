package com.obiterjus.domain.model

import java.time.Instant

data class ProcessoMonitorado(
    val numeroProcesso: String,
    val tribunal: String?,
    val grau: String?,
    val classeCodigo: Int?,
    val classeNome: String?,
    val assuntos: List<String>,
    val orgaoJulgadorCodigo: Int?,
    val orgaoJulgadorNome: String?,
    val nivelSigilo: Int?,
    val dataAjuizamento: Instant?,
    val syncStatus: ProcessoSyncStatus,
    val capturadoEm: Instant,
    val atualizadoEm: Instant,
    val participantes: List<ParticipanteProcesso> = emptyList(),
)

data class ParticipanteProcesso(
    val idLocal: String,
    val numeroProcesso: String,
    val polo: String?,
    val nome: String?,
    val tipoPessoa: String?,
    val tipoParticipacao: String?,
)

data class MovimentoProcesso(
    val idLocal: String,
    val numeroProcesso: String,
    val codigo: Int?,
    val nome: String?,
    val dataHora: Instant?,
    val complementosJson: String?,
)
