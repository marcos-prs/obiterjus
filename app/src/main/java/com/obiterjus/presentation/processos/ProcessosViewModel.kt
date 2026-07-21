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
class ProcessosViewModel(
    observarProcessos: ObservarProcessos,
    observarTimelineProcesso: ObservarTimelineProcesso,
) : ViewModel() {
    private val filtros = MutableStateFlow(FiltrosProcessos())
    private val tribunaisExpandidos = MutableStateFlow<Set<String>>(emptySet())
    private val numeroSelecionado = MutableStateFlow<String?>(null)
    private val timelineSelecionada = numeroSelecionado.flatMapLatest { numero ->
        if (numero == null) flowOf(emptyList()) else observarTimelineProcesso(numero)
    }

    val estado: StateFlow<EstadoProcessos> =
        combine(
            observarProcessos(),
            timelineSelecionada,
            filtros,
            numeroSelecionado,
            tribunaisExpandidos,
        ) { processos, timeline, filtrosAtuais, numeroAtual, tribunaisAtuais ->
            val tribunaisPorGenero = processos.tribunaisPorGenero()
            val filtrados = processos
                .filter { processo -> processo.atende(filtrosAtuais) }
                .ordenar(filtrosAtuais)
            val grupos = filtrados
                .groupBy { it.tribunal ?: TRIBUNAL_SEM_NOME }
                .entries
                .sortedByDescending { it.value.size }
                .map { (tribunal, processosTribunal) ->
                    GrupoTribunalProcesso(
                        tribunal = tribunal,
                        quantidade = processosTribunal.size,
                        expandido = tribunaisAtuais.isEmpty() || tribunaisAtuais.contains(tribunal),
                        comarcas = processosTribunal
                            .groupBy { it.orgaoJulgadorNome?.takeIf(String::isNotBlank) ?: COMARCA_SEM_NOME }
                            .entries
                            .map { (comarca, processosComarca) ->
                                GrupoComarcaProcesso(
                                    comarca = comarca,
                                    processos = processosComarca,
                                )
                            },
                    )
                }
            EstadoProcessos(
                processos = filtrados,
                totalPersistidos = processos.size,
                filtros = filtrosAtuais,
                processoSelecionado = processos.firstOrNull { it.numeroProcesso == numeroAtual },
                timelineSelecionada = timeline,
                gruposTribunais = grupos,
                tribunaisExpandidos = tribunaisAtuais,
                tribunaisPorGenero = tribunaisPorGenero,
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

    fun aoAlterarFiltroTribunal(valor: String) {
        filtros.update { it.copy(tribunal = valor) }
    }

    fun aoAlterarFiltroSyncStatus(valor: String) {
        filtros.update { it.copy(syncStatus = valor) }
    }

    fun aoAlterarStatus(valor: FiltroStatusProcesso) {
        filtros.update { it.copy(status = valor) }
    }

    fun aoLimparFiltros() {
        filtros.value = FiltrosProcessos()
    }

    fun aoAlternarTribunal(tribunal: String) {
        tribunaisExpandidos.update { atual ->
            if (atual.contains(tribunal)) atual - tribunal else atual + tribunal
        }
    }

    fun aoSelecionarProcesso(numeroProcesso: String) {
        numeroSelecionado.value = numeroProcesso
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
            ).any { valor -> valor.contains(textoFiltro, ignoreCase = true) } ||
            participantes.any { p ->
                p.nome?.contains(textoFiltro, ignoreCase = true) == true
            }

        val atendeParticipante = filtros.participante.isBlank() ||
            participantes.any { p ->
                p.nome?.contains(filtros.participante.trim(), ignoreCase = true) == true
            }

        val atendeSyncStatus = filtros.syncStatus.isBlank() ||
            syncStatus.name.equals(filtros.syncStatus.trim(), ignoreCase = true)
        val atendeStatus = when (filtros.status) {
            FiltroStatusProcesso.GERAL -> true
            FiltroStatusProcesso.ATIVOS -> syncStatus != com.obiterjus.domain.model.ProcessoSyncStatus.STALE
            FiltroStatusProcesso.ARQUIVADOS -> syncStatus == com.obiterjus.domain.model.ProcessoSyncStatus.STALE
        }
        val atendeTribunal = filtros.tribunal.isBlank() ||
            tribunal?.contains(filtros.tribunal.trim(), ignoreCase = true) == true

        return atendeTexto && atendeParticipante && atendeSyncStatus && atendeStatus && atendeTribunal
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
    val gruposTribunais: List<GrupoTribunalProcesso> = emptyList(),
    val tribunaisExpandidos: Set<String> = emptySet(),
    val tribunaisPorGenero: Map<com.obiterjus.domain.model.GeneroTribunal, List<String>> = emptyMap(),
)

data class FiltrosProcessos(
    val texto: String = "",
    val participante: String = "",
    val syncStatus: String = "",
    val ordenacao: OrdenacaoProcessos = OrdenacaoProcessos.MAIS_RECENTES,
    val status: FiltroStatusProcesso = FiltroStatusProcesso.GERAL,
    val tribunal: String = "",
) {
    val possuiFiltrosAtivos: Boolean
        get() = texto.isNotBlank() ||
            participante.isNotBlank() ||
            syncStatus.isNotBlank() ||
            ordenacao != OrdenacaoProcessos.MAIS_RECENTES ||
            status != FiltroStatusProcesso.GERAL ||
            tribunal.isNotBlank()
}

enum class OrdenacaoProcessos {
    MAIS_RECENTES,
    MAIS_ANTIGOS,
    TRIBUNAL,
    NUMERO,
}

enum class FiltroStatusProcesso {
    GERAL,
    ATIVOS,
    ARQUIVADOS,
}

data class GrupoTribunalProcesso(
    val tribunal: String,
    val quantidade: Int,
    val expandido: Boolean,
    val comarcas: List<GrupoComarcaProcesso>,
)

data class GrupoComarcaProcesso(
    val comarca: String,
    val processos: List<ProcessoMonitorado>,
)

private const val TRIBUNAL_SEM_NOME = ""
private const val COMARCA_SEM_NOME = ""

private fun List<ProcessoMonitorado>.tribunaisPorGenero(): Map<com.obiterjus.domain.model.GeneroTribunal, List<String>> =
    mapNotNull { processo -> processo.tribunal?.trim()?.takeIf { it.isNotBlank() } }
        .distinctBy { tribunal -> tribunal.uppercase() }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
        .groupBy { tribunal -> com.obiterjus.domain.model.GeneroTribunal.classificar(tribunal) }
