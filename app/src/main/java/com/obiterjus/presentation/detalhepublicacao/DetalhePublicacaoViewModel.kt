package com.obiterjus.presentation.detalhepublicacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.core.parser.NumeroProcessoNormalizer
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoParticipante
import com.obiterjus.domain.usecase.ObservarProcessos
import com.obiterjus.domain.usecase.ObterPublicacaoPorId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import com.obiterjus.presentation.participantes.resolverPartesProcesso
import com.obiterjus.presentation.participantes.resolverPartesPublicacao
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DetalhePublicacaoViewModel(
    private val obterPublicacaoPorId: ObterPublicacaoPorId,
    private val observarProcessos: ObservarProcessos,
) : ViewModel() {

    private val publicacaoId = MutableStateFlow<Long?>(null)

    val estado: StateFlow<EstadoDetalhePublicacao> = publicacaoId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(EstadoDetalhePublicacao(estaCarregando = true))
            } else {
                combine(
                    obterPublicacaoPorId(id),
                    observarProcessos(),
                ) { publicacao, processos ->
                    if (publicacao == null) {
                        EstadoDetalhePublicacao(
                            estaCarregando = false,
                            naoEncontrada = publicacaoId.value != null,
                        )
                    } else {
                        mapParaEstado(publicacao, processos)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EstadoDetalhePublicacao(estaCarregando = true),
        )

    fun aoCarregar(id: Long) {
        publicacaoId.value = id
    }

    private fun mapParaEstado(
        publicacao: Publicacao,
        processos: List<ProcessoMonitorado>,
    ): EstadoDetalhePublicacao {
        val processoDataJud = NumeroProcessoNormalizer.normalize(publicacao.numeroProcesso)
            ?.let { numero ->
                processos.firstOrNull { it.numeroProcesso == numero }
            }

        val partesDataJud = processoDataJud?.participantes?.resolverPartesProcesso()
        val partesPublicacao = publicacao.participantes.resolverPartesPublicacao()

        val parteAtiva = partesDataJud?.ativa ?: partesPublicacao.ativa
        val partePassiva = partesDataJud?.passiva ?: partesPublicacao.passiva

        return EstadoDetalhePublicacao(
            estaCarregando = false,
            numeroProcesso = publicacao.numeroProcesso.orEmpty(),
            parteAtivaNome = parteAtiva?.nomes?.joinToString("\n"),
            parteAtivaTipo = parteAtiva?.tipo,
            partePassivaNome = partePassiva?.nomes?.joinToString("\n"),
            partePassivaTipo = partePassiva?.tipo,
            advogados = partesPublicacao.advogados,
            dataExpediente = publicacao.dataDisponibilizacao,
            nomeAto = publicacao.tipoComunicacao.orEmpty(),
            conteudoCompleto = publicacao.textoLimpo.orEmpty(),
        )
    }
}

data class EstadoDetalhePublicacao(
    val estaCarregando: Boolean = false,
    val naoEncontrada: Boolean = false,
    val numeroProcesso: String = "",
    val parteAtivaNome: String? = null,
    val parteAtivaTipo: String? = null,
    val partePassivaNome: String? = null,
    val partePassivaTipo: String? = null,
    val advogados: List<String> = emptyList(),
    val dataExpediente: LocalDate? = null,
    val nomeAto: String = "",
    val conteudoCompleto: String = "",
)
