package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.PrazoAgendaItem
import com.obiterjus.domain.repository.RepositorioPublicacoes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObservarAgendaPrazos(
    private val repository: RepositorioPublicacoes,
) {
    operator fun invoke(): Flow<List<PrazoAgendaItem>> =
        repository.observarPublicacoes().map { publicacoes ->
            publicacoes
                .mapNotNull { publicacao ->
                    publicacao.prazo?.let { prazo ->
                        PrazoAgendaItem(
                            publicacao = publicacao,
                            prazo = prazo,
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
