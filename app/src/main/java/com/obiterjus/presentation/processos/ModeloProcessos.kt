package com.obiterjus.presentation.processos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.TimelineProcessoItem
import com.obiterjus.domain.usecase.ObservarProcessos
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

@OptIn(ExperimentalCoroutinesApi::class)
class ModeloProcessos(
    observarProcessos: ObservarProcessos,
    observarTimelineProcesso: ObservarTimelineProcesso,
) : ViewModel() {
    private val filtros = MutableStateFlow(FiltrosProcessos())
    private val numeroSelecionado = MutableStateFlow<String?>(null)
    private val timelineSelecionada = numeroSelecionado
        .flatMapLatest { numero ->
            if (numero == null) flowOf(emptyList()) else observarTimelineProcesso(numero)
        }

    private val participantesSelecionados = numeroSelecionado
        .flatMapLatest { numero ->
            if (numero == null) flowOf(emptyList()) else observarProcessos.repositorio.observarParticipantes(numero)
        }

    val estado: StateFlow<EstadoProcessos> =
        combine(
            observarProcessos(),
            timelineSelecionada,
            participantesSelecionados,
            filtros,
            numeroSelecionado,
        ) { processos, timeline, participantes, filtrosAtuais, numeroAtual ->
            val filtrados = processos
                .filter { processo -> processo.atende(filtrosAtuais) }
                .ordenar(filtrosAtuais)
            EstadoProcessos(
                processos = filtrados,
                totalPersistidos = processos.size,
                filtros = filtrosAtuais,
                processoSelecionado = processos.firstOrNull { it.numeroProcesso == numeroAtual }?.copy(participantes = participantes),
                timelineSelecionada = timeline,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EstadoProcessos(),
        )

    fun aoAlterarFiltroTexto(valor: String) {
        filtros.update { it.copy(texto = valor) }
    }

    fun aoAlterarFiltroParticipante(valor: String) {
        filtros.update { it.copy(participante = valor) }
    }

    fun aoAlterarFiltroSyncStatus(valor: String) {
        filtros.update { it.copy(syncStatus = valor) }
    }

    fun aoAlterarOrdenacao(valor: OrdenacaoProcessos) {
        filtros.update { it.copy(ordenacao = valor) }
    }

    fun aoLimparFiltros() {
        filtros.value = FiltrosProcessos()
    }

    fun aoSelecionarProcesso(numeroProcesso: String) {
        numeroSelecionado.value = numeroProcesso
    }

    fun aoFecharDetalhe() {
        numeroSelecionado.value = null
    }

    private fun ProcessoMonitorado.atende(filtros: FiltrosProcessos): Boolean {
        val textoFiltro = filtros.texto.trim()
        val atendeTexto = textoFiltro.isBlank() ||
            listOfNotNull(
                numeroProcesso,
                tribunal,
                grau,
                classeNome,
                orgaoJulgadorNome,
                syncStatus.name,
            ).any { valor -> valor.contains(textoFiltro, ignoreCase = true) }

        val atendeParticipante = filtros.participante.isBlank() ||
            participantes.any { p ->
                p.nome?.contains(filtros.participante.trim(), ignoreCase = true) == true
            }

        val atendeSyncStatus = filtros.syncStatus.isBlank() ||
            syncStatus.name.equals(filtros.syncStatus.trim(), ignoreCase = true)

        return atendeTexto && atendeParticipante && atendeSyncStatus
    }

    private fun List<ProcessoMonitorado>.ordenar(filtros: FiltrosProcessos): List<ProcessoMonitorado> =
        when (filtros.ordenacao) {
            OrdenacaoProcessos.MAIS_RECENTES -> sortedByDescending { it.atualizadoEm }
            OrdenacaoProcessos.MAIS_ANTIGOS -> sortedBy { it.atualizadoEm }
            OrdenacaoProcessos.TRIBUNAL -> sortedWith(compareBy(nullsLast()) { it.tribunal })
            OrdenacaoProcessos.NUMERO -> sortedBy { it.numeroProcesso }
        }
}

data class EstadoProcessos(
    val processos: List<ProcessoMonitorado> = emptyList(),
    val totalPersistidos: Int = 0,
    val filtros: FiltrosProcessos = FiltrosProcessos(),
    val processoSelecionado: ProcessoMonitorado? = null,
    val timelineSelecionada: List<TimelineProcessoItem> = emptyList(),
)

data class FiltrosProcessos(
    val texto: String = "",
    val participante: String = "",
    val syncStatus: String = "",
    val ordenacao: OrdenacaoProcessos = OrdenacaoProcessos.MAIS_RECENTES,
) {
    val possuiFiltrosAtivos: Boolean
        get() = texto.isNotBlank() ||
            participante.isNotBlank() ||
            syncStatus.isNotBlank() ||
            ordenacao != OrdenacaoProcessos.MAIS_RECENTES
}

enum class OrdenacaoProcessos {
    MAIS_RECENTES,
    MAIS_ANTIGOS,
    TRIBUNAL,
    NUMERO,
}
