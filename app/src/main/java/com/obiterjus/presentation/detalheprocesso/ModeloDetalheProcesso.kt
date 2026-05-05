package com.obiterjus.presentation.detalheprocesso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.domain.model.PrazoAgendaItem
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.TimelineProcessoItem
import com.obiterjus.domain.usecase.ObservarAgendaPrazos
import com.obiterjus.domain.usecase.ObservarProcessos
import com.obiterjus.domain.usecase.ObservarPublicacoes
import com.obiterjus.domain.usecase.ObservarTimelineProcesso
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloDetalheProcesso(
    private val observarProcessos: ObservarProcessos,
    private val observarPublicacoes: ObservarPublicacoes,
    private val observarAgendaPrazos: ObservarAgendaPrazos,
    private val observarTimelineProcesso: ObservarTimelineProcesso,
) : ViewModel() {
    private val numeroProcesso = MutableStateFlow("")
    private val abaSelecionada = MutableStateFlow(0)

    private val processoAtual = combine(observarProcessos(), numeroProcesso) { processos, numero ->
        processos.firstOrNull { it.numeroProcesso == numero }
    }

    private val timelineAtual = numeroProcesso.flatMapLatest { numero ->
        if (numero.isBlank()) flowOf(emptyList()) else observarTimelineProcesso(numero)
    }

    private val publicacoesAtual = combine(observarPublicacoes(), numeroProcesso) { publicacoes, numero ->
        if (numero.isBlank()) emptyList() else publicacoes.filter { it.numeroProcesso == numero }
    }

    private val prazosAtual = combine(observarAgendaPrazos(), numeroProcesso) { prazos, numero ->
        if (numero.isBlank()) emptyList() else prazos.filter { it.publicacao.numeroProcesso == numero }
    }

    private val dadosAtual = combine(
        processoAtual,
        timelineAtual,
        publicacoesAtual,
        prazosAtual,
    ) { processo, timeline, publicacoes, prazos ->
        DadosDetalhe(
            processo = processo,
            timeline = timeline,
            publicacoes = publicacoes,
            prazos = prazos,
        )
    }

    val estado: StateFlow<EstadoDetalheProcesso> = combine(
        numeroProcesso,
        abaSelecionada,
        dadosAtual,
    ) { numero, aba, dados ->
        val processoSeguro = dados.processo ?: ProcessoMonitorado(
            numeroProcesso = numero,
            tribunal = null,
            grau = null,
            classeCodigo = null,
            classeNome = null,
            assuntos = emptyList(),
            orgaoJulgadorCodigo = null,
            orgaoJulgadorNome = null,
            nivelSigilo = null,
            dataAjuizamento = null,
            syncStatus = ProcessoSyncStatus.PENDING,
            capturadoEm = Instant.EPOCH,
            atualizadoEm = Instant.EPOCH,
        )

        EstadoDetalheProcesso(
            numeroProcesso = processoSeguro.numeroProcesso,
            tribunal = processoSeguro.tribunal.orEmpty(),
            orgaoJulgador = processoSeguro.orgaoJulgadorNome.orEmpty(),
            classeProcessual = processoSeguro.classeNome.orEmpty(),
            grau = processoSeguro.grau.orEmpty(),
            status = processoSeguro.syncStatus.name,
            partes = processoSeguro.participantes.takeIf { it.isNotEmpty() }
                ?.joinToString(separator = " · ") { participante ->
                    listOfNotNull(participante.polo, participante.nome).joinToString(": ")
                },
            abas = listOf(
                AbaDetalhe.TIMELINE,
                AbaDetalhe.PUBLICACOES,
                AbaDetalhe.PRAZOS,
                AbaDetalhe.INFORMACOES,
            ),
            abaSelecionada = aba,
            timeline = dados.timeline,
            publicacoes = dados.publicacoes,
            prazos = dados.prazos,
            informacoes = InformacoesProcesso(
                numeroProcesso = processoSeguro.numeroProcesso,
                tribunal = processoSeguro.tribunal,
                orgaoJulgador = processoSeguro.orgaoJulgadorNome,
                classeProcessual = processoSeguro.classeNome,
                grau = processoSeguro.grau,
                status = processoSeguro.syncStatus.name,
                partes = processoSeguro.participantes.mapNotNull { it.nome },
                fonte = processoSeguro.tribunal ?: "",
                ultimaAtualizacao = processoSeguro.atualizadoEm,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = EstadoDetalheProcesso(numeroProcesso = ""),
    )

    fun aoCarregar(numero: String) {
        numeroProcesso.value = numero
    }

    fun aoSelecionarAba(indice: Int) {
        abaSelecionada.update { indice }
    }
}

data class EstadoDetalheProcesso(
    val numeroProcesso: String = "",
    val tribunal: String = "",
    val orgaoJulgador: String = "",
    val classeProcessual: String = "",
    val grau: String = "",
    val status: String = "",
    val partes: String? = null,
    val abas: List<AbaDetalhe> = emptyList(),
    val abaSelecionada: Int = 0,
    val timeline: List<TimelineProcessoItem> = emptyList(),
    val publicacoes: List<Publicacao> = emptyList(),
    val prazos: List<PrazoAgendaItem> = emptyList(),
    val informacoes: InformacoesProcesso? = null,
)

enum class AbaDetalhe {
    TIMELINE,
    PUBLICACOES,
    PRAZOS,
    INFORMACOES,
}

data class InformacoesProcesso(
    val numeroProcesso: String,
    val tribunal: String?,
    val orgaoJulgador: String?,
    val classeProcessual: String?,
    val grau: String?,
    val status: String,
    val partes: List<String>,
    val fonte: String,
    val ultimaAtualizacao: Instant,
)

private data class DadosDetalhe(
    val processo: ProcessoMonitorado?,
    val timeline: List<TimelineProcessoItem>,
    val publicacoes: List<Publicacao>,
    val prazos: List<PrazoAgendaItem>,
)
