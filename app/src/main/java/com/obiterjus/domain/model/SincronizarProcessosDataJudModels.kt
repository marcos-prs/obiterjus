package com.obiterjus.domain.model

data class ProcessoDataJudSyncRequest(
    val numeroProcesso: String,
    val tribunal: String? = null,
)

data class SincronizarProcessosDataJudParams(
    val processos: List<ProcessoDataJudSyncRequest>,
)

data class SincronizarProcessosDataJudResumo(
    val solicitados: Int,
    val normalizados: Int,
    val encontrados: Int,
    val naoEncontrados: Int,
    val falhas: Int,
    val movimentosSalvos: Int,
    val resultados: List<ProcessoDataJudSyncResultado>,
)

data class ProcessoDataJudSyncResultado(
    val numeroProcesso: String,
    val tribunal: String?,
    val status: ProcessoDataJudSyncStatus,
    val movimentosSalvos: Int,
    val mensagem: String?,
)

enum class ProcessoDataJudSyncStatus {
    FOUND,
    NOT_FOUND,
    FAILED,
}
