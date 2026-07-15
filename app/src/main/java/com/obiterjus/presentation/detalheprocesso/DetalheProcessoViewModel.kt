package com.obiterjus.presentation.detalheprocesso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.domain.model.PrazoAgendaItem
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.TimelineProcessoItem
import com.obiterjus.domain.usecase.ExcluirProcessoUseCase
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
import kotlinx.coroutines.launch
import java.time.Instant
import com.obiterjus.presentation.participantes.resolverPartesProcesso
import com.obiterjus.presentation.participantes.formatarConfronto

@OptIn(ExperimentalCoroutinesApi::class)
class DetalheProcessoViewModel(
    private val observarProcessos: ObservarProcessos,
    private val observarPublicacoes: ObservarPublicacoes,
    private val observarAgendaPrazos: ObservarAgendaPrazos,
    private val observarTimelineProcesso: ObservarTimelineProcesso,
    private val excluirProcesso: ExcluirProcessoUseCase,
) : ViewModel() {
    private val numeroProcesso = MutableStateFlow("")
    private val abaSelecionada = MutableStateFlow(0)
    private val exclusao = MutableStateFlow(EstadoExclusao())

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
        exclusao,
    ) { numero, aba, dados, exclusao ->
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
        val partesResolvidas = dados.partesResolvidas

        EstadoDetalheProcesso(
            numeroProcesso = processoSeguro.numeroProcesso,
            tribunal = processoSeguro.tribunal.orEmpty(),
            orgaoJulgador = processoSeguro.orgaoJulgadorNome.orEmpty(),
            classeProcessual = processoSeguro.classeNome.orEmpty(),
            grau = processoSeguro.grau.orEmpty(),
            status = processoSeguro.syncStatus.name,
            partes = partesResolvidas.formatarConfronto(),
            poloAtivo = partesResolvidas.ativa?.nomes ?: emptyList(),
            poloPassivo = partesResolvidas.passiva?.nomes ?: emptyList(),
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
                poloAtivo = partesResolvidas.ativa?.nomes ?: emptyList(),
                poloPassivo = partesResolvidas.passiva?.nomes ?: emptyList(),
                fonte = processoSeguro.tribunal ?: "",
                ultimaAtualizacao = processoSeguro.atualizadoEm,
                dataDistribuicao = processoSeguro.dataDistribuicao,
                dataAjuizamento = processoSeguro.dataAjuizamento,
                assuntoPrincipal = processoSeguro.assuntos.firstOrNull(),
                nivelSigilo = processoSeguro.nivelSigilo,
                comarcaSecao = processoSeguro.comarcaSecao,
                juizo = processoSeguro.juizo,
                prioridadeTramitacao = processoSeguro.prioridadeTramitacao,
                gratuidadeJustica = processoSeguro.gratuidadeJustica,
                valorCausa = processoSeguro.valorCausa,
                faseProcessual = processoSeguro.faseProcessual,
                situacaoAtual = processoSeguro.situacaoAtual,
                tutelaAntecipadaLiminar = processoSeguro.tutelaAntecipadaLiminar,
                advogadosAtivo = processoSeguro.advogadosAtivo,
                advogadosPassivo = processoSeguro.advogadosPassivo,
                defensoriaPublica = processoSeguro.defensoriaPublica,
                ministerioPublico = processoSeguro.ministerioPublico,
                terceirosAuxiliares = processoSeguro.terceirosAuxiliares,
            ),
            ultimaAtualizacao = processoSeguro.atualizadoEm,
            excluindo = exclusao.excluindo,
            excluido = exclusao.excluido,
            erroExclusao = exclusao.erro,
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

    fun aoExcluir() {
        val numero = numeroProcesso.value
        if (numero.isBlank()) return
        exclusao.update { it.copy(excluindo = true, erro = false) }
        viewModelScope.launch {
            try {
                excluirProcesso(numero)
                exclusao.update { it.copy(excluindo = false, excluido = true) }
            } catch (_: Exception) {
                exclusao.update { it.copy(excluindo = false, erro = true) }
            }
        }
    }

    fun aoDescartarErroExclusao() {
        exclusao.update { it.copy(erro = false) }
    }
}

private data class EstadoExclusao(
    val excluindo: Boolean = false,
    val excluido: Boolean = false,
    val erro: Boolean = false,
)

data class EstadoDetalheProcesso(
    val numeroProcesso: String = "",
    val tribunal: String = "",
    val orgaoJulgador: String = "",
    val classeProcessual: String = "",
    val grau: String = "",
    val status: String = "",
    val partes: String? = null,
    val poloAtivo: List<String> = emptyList(),
    val poloPassivo: List<String> = emptyList(),
    val abas: List<AbaDetalhe> = emptyList(),
    val abaSelecionada: Int = 0,
    val timeline: List<TimelineProcessoItem> = emptyList(),
    val publicacoes: List<Publicacao> = emptyList(),
    val prazos: List<PrazoAgendaItem> = emptyList(),
    val informacoes: InformacoesProcesso? = null,
    val ultimaAtualizacao: Instant? = null,
    val excluindo: Boolean = false,
    val excluido: Boolean = false,
    val erroExclusao: Boolean = false,
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
    val poloAtivo: List<String>,
    val poloPassivo: List<String>,
    val fonte: String,
    val ultimaAtualizacao: Instant,
    // Campos expandidos — espelham os campos de EditarProcesso
    val dataDistribuicao: Instant? = null,
    val dataAjuizamento: Instant? = null,
    val assuntoPrincipal: String? = null,
    val nivelSigilo: Int? = null,
    val comarcaSecao: String? = null,
    val juizo: String? = null,
    val prioridadeTramitacao: String? = null,
    val gratuidadeJustica: String? = null,
    val valorCausa: Double? = null,
    val faseProcessual: String? = null,
    val situacaoAtual: String? = null,
    val tutelaAntecipadaLiminar: String? = null,
    val advogadosAtivo: String? = null,
    val advogadosPassivo: String? = null,
    val defensoriaPublica: String? = null,
    val ministerioPublico: String? = null,
    val terceirosAuxiliares: String? = null,
)

private data class DadosDetalhe(
    val processo: ProcessoMonitorado?,
    val timeline: List<TimelineProcessoItem>,
    val publicacoes: List<Publicacao>,
    val prazos: List<PrazoAgendaItem>,
)

private val DadosDetalhe.partesResolvidas
    get() = processo?.participantes.orEmpty().resolverPartesProcesso()
