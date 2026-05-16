package com.obiterjus.domain.usecase

import com.obiterjus.data.agenda.local.PrazoSugeridoDao
import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.domain.model.PrazoAgendaItem
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.model.ProvedorCalendario
import com.obiterjus.domain.repository.PublicacoesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObservarAgendaPrazos(
    private val repository: PublicacoesRepository,
    private val prazoSugeridoDao: PrazoSugeridoDao,
    private val classificarPublicacaoUC: ClassificarPublicacaoUC,
) {
    operator fun invoke(): Flow<List<PrazoAgendaItem>> =
        combine(
            repository.observarPublicacoes(),
            prazoSugeridoDao.observeAll(),
        ) { publicacoes, prazosSugeridos ->
            val prazosPorPublicacao = prazosSugeridos.associateBy { it.publicacaoId }
            publicacoes
                .map { publicacao ->
                    if (publicacao.prazo?.dataLimiteEstimada == null) {
                        classificarPublicacaoUC(publicacao)
                    } else {
                        publicacao
                    }
                }
                .mapNotNull { publicacao ->
                    publicacao.prazo?.let { prazo ->
                        PrazoAgendaItem(
                            publicacao = publicacao,
                            prazo = prazo.aplicarConfirmacao(prazosPorPublicacao[publicacao.id]),
                        )
                    }
                }
                .sortedWith(prazoAgendaComparator)
        }

    private companion object {
        val prazoAgendaComparator = Comparator<PrazoAgendaItem> { first, second ->
            val firstDate = first.prazo.dataLimiteEstimada
            val secondDate = second.prazo.dataLimiteEstimada
            when {
                firstDate != null && secondDate != null && firstDate != secondDate ->
                    firstDate.compareTo(secondDate)
                firstDate != null && secondDate == null -> -1
                firstDate == null && secondDate != null -> 1
                else -> compareValuesBy(second, first) { item -> item.publicacao.dataDisponibilizacao }
            }
        }
    }
}

private fun PublicacaoPrazo.aplicarConfirmacao(
    prazoSugerido: PrazoSugeridoEntity?,
): PublicacaoPrazo =
    copy(
        isConfirmado = prazoSugerido?.isConfirmado ?: isConfirmado,
        idExternoCalendario = prazoSugerido?.idExternoCalendario ?: idExternoCalendario,
        provedorCalendario = ProvedorCalendario.fromCodigo(prazoSugerido?.provedorCalendario)?.codigo
            ?: provedorCalendario,
    )
