package com.obiterjus.domain.model

data class MonitorarCnjResumo(
    val djen: MonitorarDjenResumo,
    val dataJud: SincronizarProcessosDataJudResumo?,
) {
    val totalProcessosSincronizados: Int
        get() = dataJud?.encontrados ?: 0

    val teveSincronizacaoDataJud: Boolean
        get() = dataJud != null
}
