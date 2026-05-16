package com.obiterjus.domain.usecase

import com.obiterjus.core.texto.DatajudParser
import com.obiterjus.domain.model.CorPontoTimeline
import com.obiterjus.domain.model.MovimentoProcesso
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.TimelineProcessoItem
import com.obiterjus.domain.model.TimelineProcessoTipo
import com.obiterjus.domain.repository.ProcessosRepository
import com.obiterjus.domain.repository.PublicacoesRepository
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObservarTimelineProcesso(
    private val repositorioProcessos: ProcessosRepository,
    private val repositorioPublicacoes: PublicacoesRepository,
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

        val corPonto = when {
            tituloLimpo.contains("liminar", ignoreCase = true) ||
            tituloLimpo.contains("tutela", ignoreCase = true) ||
            tituloLimpo.contains("antecipada", ignoreCase = true) -> CorPontoTimeline.DANGER

            tituloLimpo.contains("mero", ignoreCase = true) ||
            tituloLimpo.contains("expediente", ignoreCase = true) ||
            tituloLimpo.contains("despacho", ignoreCase = true) -> CorPontoTimeline.DESPACHO


            tituloLimpo.contains("audiência", ignoreCase = true) -> CorPontoTimeline.WARNING
            
            tituloLimpo.contains("contestação", ignoreCase = true) ||
            tituloLimpo.contains("manifestação", ignoreCase = true) -> CorPontoTimeline.PRIMARY
            
            tituloLimpo.contains("citação", ignoreCase = true) ||
            tituloLimpo.contains("intimação", ignoreCase = true) ||
            tituloLimpo.contains("despacho", ignoreCase = true) -> CorPontoTimeline.MUTED
            
            tituloLimpo.contains("petição", ignoreCase = true) ||
            tituloLimpo.contains("distribuída", ignoreCase = true) -> CorPontoTimeline.SUCCESS
            
            else -> CorPontoTimeline.ACCENT
        }

        return TimelineProcessoItem(
            id = "datajud:$idLocal",
            tipo = TimelineProcessoTipo.MOVIMENTO_DATAJUD,
            fonte = "DataJud",
            titulo = tituloLimpo,
            dataHora = dataHora,
            descricao = DatajudParser.parsearDescricao(complementosJson),
            corPonto = corPonto,
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
            descricao = null, // Texto completo disponível na aba Publicações
            corPonto = CorPontoTimeline.PRIMARY,
            isSigiloso = isSigiloso,
        )
}
