package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.MovimentoProcesso
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.TimelineProcessoItem
import com.obiterjus.domain.model.TimelineProcessoTipo
import com.obiterjus.domain.repository.RepositorioProcessos
import com.obiterjus.domain.repository.RepositorioPublicacoes
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObservarTimelineProcesso(
    private val repositorioProcessos: RepositorioProcessos,
    private val repositorioPublicacoes: RepositorioPublicacoes,
) {
    operator fun invoke(numeroProcesso: String): Flow<List<TimelineProcessoItem>> =
        combine(
            repositorioProcessos.observarMovimentos(numeroProcesso),
            repositorioPublicacoes.observarPublicacoesProcesso(numeroProcesso),
        ) { movimentos, publicacoes ->
            (movimentos.map { it.toTimelineItem() } +
                publicacoes.map { it.toTimelineItem() })
                .sortedWith(
                    compareByDescending<TimelineProcessoItem> { it.dataHora ?: java.time.Instant.EPOCH }
                        .thenByDescending { it.id },
                )
        }

    private fun MovimentoProcesso.toTimelineItem(): TimelineProcessoItem {
        val tituloLimpo = nome ?: "Movimento sem nome"
        val isImportante = tituloLimpo.contains("Sentença", ignoreCase = true) ||
            tituloLimpo.contains("Decisão", ignoreCase = true) ||
            tituloLimpo.contains("Julgamento", ignoreCase = true) ||
            tituloLimpo.contains("Acórdão", ignoreCase = true) ||
            tituloLimpo.contains("Despacho", ignoreCase = true)

        return TimelineProcessoItem(
            id = "datajud:$idLocal",
            tipo = TimelineProcessoTipo.MOVIMENTO_DATAJUD,
            fonte = "DataJud",
            titulo = tituloLimpo,
            dataHora = dataHora,
            descricao = complementosJson,
            isImportante = isImportante,
        )
    }

    private fun Publicacao.toTimelineItem(): TimelineProcessoItem =
        TimelineProcessoItem(
            id = "djen:$id",
            tipo = TimelineProcessoTipo.PUBLICACAO_DJEN,
            fonte = fonte,
            titulo = tipoComunicacao ?: "Publicação DJEN",
            dataHora = dataDisponibilizacao?.atStartOfDay()?.toInstant(ZoneOffset.UTC) ?: capturadoEm,
            descricao = if (isSigiloso) null else textoLimpo,
            isSigiloso = isSigiloso,
        )
}
