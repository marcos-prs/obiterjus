package com.obiterjus.presentation.publicacoes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.core.parser.CnjDateParser
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.usecase.ObservarPublicacoes
import com.obiterjus.domain.usecase.ObterCertidaoDjen
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

    val estado: StateFlow<EstadoPublicacoes> =
        combine(
            observarPublicacoes(),
            filtros,
            publicacaoSelecionadaId,
            certidaoState,
        ) { publicacoes, filtrosAtuais, selecionadaId, certidao ->
            val filtradas = publicacoes
                .filter { publicacao -> publicacao.atende(filtrosAtuais) }
            EstadoPublicacoes(
                publicacoes = filtradas,
                totalPersistidas = publicacoes.size,
                filtros = filtrosAtuais,
                publicacaoSelecionada = publicacoes.firstOrNull { it.id == selecionadaId },
                certidao = certidao,
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

    fun aoAlterarFiltroDataInicio(valor: String) {
        filtros.update { it.copy(dataInicio = valor) }
    }

    fun aoAlterarFiltroDataFim(valor: String) {
        filtros.update { it.copy(dataFim = valor) }
    }

    fun aoAlternarSomenteSigilosas() {
        filtros.update { it.copy(somenteSigilosas = !it.somenteSigilosas) }
    }

    fun aoLimparFiltros() {
        filtros.value = FiltrosPublicacoes()
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
        val atendeTexto = textoFiltro.isBlank() ||
            listOfNotNull(
                numeroProcesso,
                tribunal,
                tipoComunicacao,
                nomeOrgao,
                textoLimpo,
            ).any { valor -> valor.contains(textoFiltro, ignoreCase = true) }

        val atendeSigilo = !filtros.somenteSigilosas || isSigiloso
        val atendeTribunal = atendeCampoEstruturado(tribunal, filtros.tribunal)
        val atendeTipo = atendeCampoEstruturado(tipoComunicacao, filtros.tipoComunicacao)
        val atendePeriodo = atendePeriodo(filtros)
        return atendeTexto && atendeSigilo && atendeTribunal && atendeTipo && atendePeriodo
    }

    private fun atendeCampoEstruturado(valor: String?, filtro: String): Boolean {
        val termo = filtro.trim()
        return termo.isBlank() || valor?.contains(termo, ignoreCase = true) == true
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
    val totalPersistidas: Int = 0,
    val filtros: FiltrosPublicacoes = FiltrosPublicacoes(),
    val publicacaoSelecionada: Publicacao? = null,
    val certidao: CertidaoUiState = CertidaoUiState(),
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
    val dataInicio: String = "",
    val dataFim: String = "",
    val somenteSigilosas: Boolean = false,
) {
    val possuiFiltrosAtivos: Boolean
        get() = texto.isNotBlank() ||
            tribunal.isNotBlank() ||
            tipoComunicacao.isNotBlank() ||
            dataInicio.isNotBlank() ||
            dataFim.isNotBlank() ||
            somenteSigilosas
}
