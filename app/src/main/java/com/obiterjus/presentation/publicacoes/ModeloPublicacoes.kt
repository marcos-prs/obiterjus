package com.obiterjus.presentation.publicacoes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.core.parser.CnjDateParser
import com.obiterjus.core.texto.correspondeAoTermoBusca
import com.obiterjus.domain.logic.NormalizadorPublicacoes
import com.obiterjus.domain.model.ConfiancaMatch
import com.obiterjus.domain.model.GeneroTribunal
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.TipoAto
import com.obiterjus.domain.model.tipoAto
import com.obiterjus.domain.usecase.ObservarPublicacoes
import com.obiterjus.domain.usecase.ObterCertidaoDjen
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class PublicacoesViewModel(
    observarPublicacoes: ObservarPublicacoes,
    private val obterCertidaoDjen: ObterCertidaoDjen,
) : ViewModel() {
    private val filtros = MutableStateFlow(FiltrosPublicacoes())
    private val publicacaoSelecionadaId = MutableStateFlow<Long?>(null)
    private val certidaoState = MutableStateFlow(CertidaoUiState())
    private val duplicatasCanonicaId = MutableStateFlow<Long?>(null)

    val estado: StateFlow<EstadoPublicacoes> =
        combine(
            observarPublicacoes(),
            filtros,
            publicacaoSelecionadaId,
            certidaoState,
            duplicatasCanonicaId,
        ) { publicacoes, filtrosAtuais, selecionadaId, certidao, canonicaId ->
            val tribunaisPorGenero = publicacoes.tribunaisPorGenero()
            val filtradas = publicacoes
                .filter { publicacao -> publicacao.atende(filtrosAtuais) }
            val deduplicadas = NormalizadorPublicacoes.deduplicar(filtradas)
            val agrupadas = NormalizadorPublicacoes.agruparPorData(deduplicadas)
            val metadados = NormalizadorPublicacoes.calcularMetadadosOrdem(deduplicadas)

            val canonicaDuplicatas = canonicaId
                ?.let { id -> publicacoes.firstOrNull { it.id == id } }
            val duplicatasSelecionadas = canonicaId
                ?.let { id ->
                    publicacoes.filter { it.duplicataDe == id }
                        .sortedWith(compareBy({ it.capturadoEm }, { it.id }))
                }
                .orEmpty()

            EstadoPublicacoes(
                publicacoes = deduplicadas,
                publicacoesAgrupadas = agrupadas,
                metadadosOrdem = metadados,
                tribunaisPorGenero = tribunaisPorGenero,
                totalPersistidas = publicacoes.size,
                filtros = filtrosAtuais,
                publicacaoSelecionada = publicacoes.firstOrNull { it.id == selecionadaId },
                certidao = certidao,
                canonicaDuplicatas = canonicaDuplicatas,
                duplicatasSelecionadas = duplicatasSelecionadas,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EstadoPublicacoes(),
        )

    fun aoAlterarFiltroTexto(valor: String) {
        filtros.update { it.copy(texto = valor) }
    }

    fun aoAlterarFiltroTribunal(valor: String) {
        filtros.update { it.copy(tribunal = valor) }
    }

    fun aoAlterarFiltroTipo(valor: String) {
        filtros.update { it.copy(tipoComunicacao = valor) }
    }

    fun aoAlterarFiltroTipoAto(tipo: TipoAto?) {
        filtros.update { it.copy(tipoAto = tipo) }
    }

    fun aoAlterarFiltroDataInicio(valor: String) {
        filtros.update { it.copy(dataInicio = valor) }
    }

    fun aoAlterarFiltroDataFim(valor: String) {
        filtros.update { it.copy(dataFim = valor) }
    }

    fun aoAlternarSomenteSigilosas() {
        filtros.update { it.copy(somenteSigilosas = !it.somenteSigilosas) }
    }

    fun aoAlterarFiltroConfianca(valor: ConfiancaMatch?) {
        filtros.update {
            // Confiança e Duplicatas são mutuamente exclusivas no mesmo chip row.
            it.copy(confiancaSelecionada = valor, mostrarApenasDuplicatas = false)
        }
    }

    fun aoAlternarFiltroDuplicatas() {
        filtros.update {
            val novoEstado = !it.mostrarApenasDuplicatas
            it.copy(
                mostrarApenasDuplicatas = novoEstado,
                confiancaSelecionada = if (novoEstado) null else it.confiancaSelecionada,
            )
        }
    }

    fun aoLimparFiltros() {
        filtros.value = FiltrosPublicacoes()
    }

    fun aoAbrirDuplicatas(canonicaId: Long) {
        duplicatasCanonicaId.value = canonicaId
    }

    fun aoFecharDuplicatas() {
        duplicatasCanonicaId.value = null
    }

    fun aoSelecionarPublicacao(id: Long) {
        publicacaoSelecionadaId.value = id
    }

    fun aoFecharDetalhe() {
        publicacaoSelecionadaId.value = null
    }

    fun aoAbrirCertidao(publicacao: Publicacao) {
        val hash = publicacao.hash?.trim()
        if (hash.isNullOrEmpty()) {
            certidaoState.value = CertidaoUiState(error = true)
            return
        }
        viewModelScope.launch {
            certidaoState.value = CertidaoUiState(isLoading = true)
            try {
                val uri = obterCertidaoDjen(hash)
                certidaoState.value = CertidaoUiState(uri = uri)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                certidaoState.value = CertidaoUiState(error = true)
            }
        }
    }

    fun aoConsumirCertidao() {
        certidaoState.value = CertidaoUiState()
    }

    fun aoFalharAbrirCertidao() {
        certidaoState.value = CertidaoUiState(error = true)
    }

    private fun Publicacao.atende(filtros: FiltrosPublicacoes): Boolean {
        val textoFiltro = filtros.texto.trim()
        val atendeTexto = when {
            textoFiltro.isBlank() -> true
            listOfNotNull(
                numeroProcesso,
                tribunal,
                tipoComunicacao,
                nomeOrgao,
                textoLimpo,
            ).correspondeAoTermoBusca(textoFiltro) -> true
            else -> participantes.any { participante ->
                participante.camposBusca().correspondeAoTermoBusca(textoFiltro)
            }
        }

        val atendeSigilo = !filtros.somenteSigilosas || isSigiloso
        val atendeTribunal = atendeTribunal(filtros.tribunal)
        val atendeTipo = atendeTipo(filtros.tipoComunicacao)
        val atendeTipoAto = filtros.tipoAto == null || tipoAto == filtros.tipoAto
        val atendePeriodo = atendePeriodo(filtros)
        val atendeConfianca = filtros.confiancaSelecionada == null ||
            confiancaMatch == filtros.confiancaSelecionada
        val atendeDuplicatas = if (filtros.mostrarApenasDuplicatas) {
            isDuplicata
        } else {
            !isDuplicata
        }
        return atendeTexto && atendeSigilo && atendeTribunal && atendeTipo && atendeTipoAto &&
            atendePeriodo && atendeConfianca && atendeDuplicatas
    }

    private fun Publicacao.atendeTribunal(filtro: String): Boolean {
        val termo = filtro.trim()
        return termo.isBlank() || tribunal?.contains(termo, ignoreCase = true) == true
    }

    private fun Publicacao.atendeTipo(filtro: String): Boolean {
        val termo = filtro.trim().lowercase()
        if (termo.isBlank()) return true

        return tipoComunicacao?.contains(termo, ignoreCase = true) == true ||
            tipoAto.name.equals(termo, ignoreCase = true)
    }

    private fun Publicacao.atendePeriodo(filtros: FiltrosPublicacoes): Boolean {
        val data = dataDisponibilizacao ?: return filtros.dataInicio.isBlank() && filtros.dataFim.isBlank()
        val dataInicio = CnjDateParser.parseLocalDate(filtros.dataInicio)
        val dataFim = CnjDateParser.parseLocalDate(filtros.dataFim)
        val depoisDoInicio = dataInicio?.let { !data.isBefore(it) } ?: true
        val antesDoFim = dataFim?.let { !data.isAfter(it) } ?: true
        return depoisDoInicio && antesDoFim
    }
}

data class EstadoPublicacoes(
    val publicacoes: List<Publicacao> = emptyList(),
    val publicacoesAgrupadas: Map<LocalDate, List<Publicacao>> = emptyMap(),
    val metadadosOrdem: Map<Long, Pair<Int, Int>> = emptyMap(),
    val tribunaisPorGenero: Map<GeneroTribunal, List<String>> = emptyMap(),
    val totalPersistidas: Int = 0,
    val filtros: FiltrosPublicacoes = FiltrosPublicacoes(),
    val publicacaoSelecionada: Publicacao? = null,
    val certidao: CertidaoUiState = CertidaoUiState(),
    val canonicaDuplicatas: Publicacao? = null,
    val duplicatasSelecionadas: List<Publicacao> = emptyList(),
)

data class CertidaoUiState(
    val isLoading: Boolean = false,
    val uri: Uri? = null,
    val error: Boolean = false,
)

data class FiltrosPublicacoes(
    val texto: String = "",
    val tribunal: String = "",
    val tipoComunicacao: String = "",
    val tipoAto: TipoAto? = null,
    val dataInicio: String = "",
    val dataFim: String = "",
    val somenteSigilosas: Boolean = false,
    val confiancaSelecionada: ConfiancaMatch? = null,
    val mostrarApenasDuplicatas: Boolean = false,
) {
    val possuiFiltrosAtivos: Boolean
        get() = texto.isNotBlank() ||
            tribunal.isNotBlank() ||
            tipoComunicacao.isNotBlank() ||
            tipoAto != null ||
            dataInicio.isNotBlank() ||
            dataFim.isNotBlank() ||
            somenteSigilosas ||
            confiancaSelecionada != null ||
            mostrarApenasDuplicatas
}

private fun List<Publicacao>.tribunaisPorGenero(): Map<GeneroTribunal, List<String>> =
    mapNotNull { publicacao -> publicacao.tribunal?.trim()?.takeIf { it.isNotBlank() } }
        .distinctBy { tribunal -> tribunal.uppercase() }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
        .groupBy { tribunal -> GeneroTribunal.classificar(tribunal) }

private fun com.obiterjus.domain.model.PublicacaoParticipante.camposBusca(): List<String?> {
    val combinado = listOfNotNull(nome, documento).joinToString(" ").trim()
    return listOfNotNull(
        nome,
        documento,
        combinado.takeIf { it.isNotBlank() },
    )
}
