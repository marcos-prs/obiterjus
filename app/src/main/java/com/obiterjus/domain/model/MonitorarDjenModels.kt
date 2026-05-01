package com.obiterjus.domain.model

import java.time.LocalDate

data class MonitorarDjenParams(
    val numeroOab: String,
    val ufOab: String,
    val dataInicio: LocalDate,
    val dataFim: LocalDate,
    val modo: MonitorarDjenModo,
)

enum class MonitorarDjenModo {
    MANUAL,
    BACKGROUND,
}

data class MonitorarDjenResumo(
    val totalRemoto: Int?,
    val totalRecebidas: Int,
    val novas: Int,
    val atualizadas: Int,
    val sigilosas: Int,
    val processosNovos: List<String>,
    val processosParaSincronizar: List<ProcessoDataJudSyncRequest>,
    val paginasConsultadas: Int,
    val motivoParada: MonitorarDjenStopReason,
    val falhas: List<String>,
    val novasComPrazo: Int = 0,
    val novasSigilosas: Int = 0,
)

enum class MonitorarDjenStopReason {
    EMPTY_PAGE,
    PARTIAL_PAGE,
    COUNT_CONSUMED,
    REPEATED_PAGE,
    PAGE_LIMIT,
    UNKNOWN,
}
