package com.obiterjus.domain.model

import java.time.Instant

data class TimelineProcessoItem(
    val id: String,
    val tipo: TimelineProcessoTipo,
    val fonte: String,
    val titulo: String,
    val dataHora: Instant?,
    val descricao: String?,
    val corPonto: CorPontoTimeline,
    val isSigiloso: Boolean = false,
    val isImportante: Boolean = false,
)

enum class TimelineProcessoTipo {
    PUBLICACAO_DJEN,
    MOVIMENTO_DATAJUD,
}
