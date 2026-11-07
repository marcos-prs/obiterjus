package com.obiterjus.domain.usecase

import com.obiterjus.data.agenda.local.PrazoSugeridoDao
import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.domain.model.PrazoAgendaItem
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.model.ProvedorCalendario
import com.obiterjus.domain.repository.PublicacoesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Leitura pura do banco — nenhuma chamada de rede acontece aqui.
 *
 * O cálculo de data-limite pertence à ingestão ([com.obiterjus.data.djen.mapper.PublicacaoPrazoMapper])
 * e ao recálculo em background ([RecalcularPrazosPendentesUC]), ambos gravando o
 * resultado. Recalcular na leitura significava uma chamada HTTP sequencial por
 * publicação sem data, a cada emissão de qualquer um dos três fluxos, com o
 * resultado descartado no fim — a agenda não pintava enquanto a API não
 * respondesse, e o estado nunca convergia.
 */
class ObservarAgendaPrazos(
    private val repository: PublicacoesRepository,
    private val prazoSugeridoDao: PrazoSugeridoDao,
    private val observarClientesPorProcesso: ObservarClientesPorProcesso,
) {
    operator fun invoke(): Flow<List<PrazoAgendaItem>> =
        combine(
            repository.observarPublicacoes(),
            prazoSugeridoDao.observeAll(),
            observarClientesPorProcesso(),
        ) { publicacoes, prazosSugeridos, clientesPorProcesso ->
            val prazosPorPublicacao = prazosSugeridos.associateBy { it.publicacaoId }
            publicacoes
                .mapNotNull { publicacao ->
                    publicacao.prazo?.let { prazo ->
                        PrazoAgendaItem(
                            publicacao = publicacao,
                            prazo = prazo.aplicarConfirmacao(prazosPorPublicacao[publicacao.id]),
                            nomeCliente = publicacao.numeroProcesso
                                ?.let { clientesPorProcesso[it] }
                                ?.nome,
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

// A linha em prazos_sugeridos é a fonte de verdade quando existe: além da
// confirmação, sobrepõe os dados do prazo — protege um prazo cadastrado
// manualmente de ser recalculado por cima num re-sync do DJEN.
internal fun PublicacaoPrazo.aplicarConfirmacao(
    prazoSugerido: PrazoSugeridoEntity?,
): PublicacaoPrazo =
    if (prazoSugerido == null) {
        this
    } else {
        copy(
            quantidade = prazoSugerido.quantidade,
            unidade = prazoSugerido.unidade,
            diasUteis = prazoSugerido.diasUteis,
            textoOriginal = prazoSugerido.textoOriginal,
            dataLimiteEstimada = prazoSugerido.dataLimite ?: dataLimiteEstimada,
            isConfirmado = prazoSugerido.isConfirmado,
            idExternoCalendario = prazoSugerido.idExternoCalendario ?: idExternoCalendario,
            provedorCalendario = ProvedorCalendario.fromCodigo(prazoSugerido.provedorCalendario)?.codigo
                ?: provedorCalendario,
            isCumprido = prazoSugerido.isCumprido,
        )
    }
