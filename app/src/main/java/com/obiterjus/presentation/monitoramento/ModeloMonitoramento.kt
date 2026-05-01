package com.obiterjus.presentation.monitoramento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.core.parser.CnjDateParser
import com.obiterjus.domain.model.MonitorarCnjResumo
import com.obiterjus.domain.model.MonitorarDjenModo
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.SincronizacaoStatus
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.RepositorioCadastroOab
import com.obiterjus.domain.repository.RepositorioSincronizacao
import com.obiterjus.data.settings.SyncPreferencesRepository
import com.obiterjus.domain.usecase.ExportarRelatorioUC
import com.obiterjus.domain.usecase.MonitorarCnjUseCase
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MonitoramentoViewModel(
    private val monitorarCnjUseCase: MonitorarCnjUseCase,
    private val authRepository: AuthRepository,
    private val repositorioSincronizacao: RepositorioSincronizacao,
    private val repositorioCadastroOab: RepositorioCadastroOab,
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val exportarRelatorioUC: ExportarRelatorioUC,
) : ViewModel() {
    private val today = LocalDate.now()
    private val _uiState = MutableStateFlow(
        MonitoramentoUiState(
            dataInicio = FormatadorData.formatarData(today.minusDays(DEFAULT_LOOKBACK_DAYS)),
            dataFim = FormatadorData.formatarData(today),
        ),
    )
    val uiState: StateFlow<MonitoramentoUiState> = _uiState.asStateFlow()
    private var cadastroInicialCarregado = false

    private val _exportTextoPendente = MutableStateFlow<String?>(null)
    /** Texto de relatório pronto para compartilhamento. Consuma com [aoConsumirExporte]. */
    val exportTextoPendente: StateFlow<String?> = _exportTextoPendente.asStateFlow()

    init {
        viewModelScope.launch {
            repositorioCadastroOab.cadastro.collect { cadastro ->
                if (!cadastroInicialCarregado) {
                    _uiState.update { current ->
                        current.copy(
                            numeroOab = cadastro.numero.ifBlank { current.numeroOab },
                            ufOab = cadastro.uf.ifBlank { current.ufOab },
                            dataInicio = cadastro.dataInicio?.let(FormatadorData::formatarData)
                                ?: current.dataInicio,
                            dataFim = cadastro.dataFim?.let(FormatadorData::formatarData)
                                ?: current.dataFim,
                        )
                    }
                    cadastroInicialCarregado = true
                }
            }
        }

        viewModelScope.launch {
            repositorioCadastroOab.status.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
        
        viewModelScope.launch {
            syncPreferencesRepository.syncFrequencyHours.collect { freq ->
                _uiState.update { it.copy(syncFrequencyHours = freq) }
            }
        }
    }

    fun onNumeroOabChange(value: String) {
        _uiState.update { it.copy(numeroOab = value, error = null) }
        persistirCadastroAtual()
    }

    fun onUfOabChange(value: String) {
        _uiState.update {
            it.copy(
                ufOab = value.filter(Char::isLetter).take(2).uppercase(),
                error = null,
            )
        }
        persistirCadastroAtual()
    }

    fun onDataInicioChange(value: String) {
        _uiState.update { it.copy(dataInicio = value, error = null) }
        persistirCadastroAtual()
    }

    fun onDataFimChange(value: String) {
        _uiState.update { it.copy(dataFim = value, error = null) }
        persistirCadastroAtual()
    }

    fun onSyncFrequencyChange(hours: Int) {
        viewModelScope.launch {
            syncPreferencesRepository.saveSyncFrequencyHours(hours)
        }
    }

    fun sincronizar() {
        val state = _uiState.value
        val dataInicio = CnjDateParser.parseLocalDate(state.dataInicio)
        val dataFim = CnjDateParser.parseLocalDate(state.dataFim)

        if (dataInicio == null || dataFim == null) {
            _uiState.update {
                it.copy(error = MonitoramentoUiError.InvalidDate)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }

            val result = runCatching {
                withContext(Dispatchers.IO) {
                    monitorarCnjUseCase(
                        MonitorarDjenParams(
                            numeroOab = state.numeroOab,
                            ufOab = state.ufOab,
                            dataInicio = dataInicio,
                            dataFim = dataFim,
                            modo = MonitorarDjenModo.MANUAL,
                        ),
                    )
                }
            }

            _uiState.update { current ->
                result.fold(
                    onSuccess = { resumo ->
                        viewModelScope.launch {
                            if (resumo.djen.falhas.isEmpty()) {
                                repositorioCadastroOab.registrarSucesso(
                                    executadoEm = Instant.now(),
                                    novasPublicacoes = resumo.djen.novas,
                                )
                            } else {
                                repositorioCadastroOab.registrarFalha(
                                    executadoEm = Instant.now(),
                                    mensagem = resumo.djen.falhas.first(),
                                )
                            }
                        }
                        current.copy(
                            isLoading = false,
                            lastResumo = resumo,
                            error = null,
                        )
                    },
                    onFailure = {
                        viewModelScope.launch {
                            repositorioCadastroOab.registrarFalha(
                                executadoEm = Instant.now(),
                                mensagem = it.message ?: it::class.java.simpleName,
                            )
                        }
                        current.copy(
                            isLoading = false,
                            error = MonitoramentoUiError.SyncFailed,
                        )
                    },
                )
            }

            result.getOrNull()
                ?.takeIf { it.djen.falhas.isEmpty() }
                ?.let { sincronizarNuvemSePossivel() }
        }
    }

    private fun persistirCadastroAtual() {
        val state = _uiState.value
        viewModelScope.launch {
            repositorioCadastroOab.salvarCadastro(
                numero = state.numeroOab,
                uf = state.ufOab,
                dataInicio = CnjDateParser.parseLocalDate(state.dataInicio),
                dataFim = CnjDateParser.parseLocalDate(state.dataFim),
            )
        }
    }

    private suspend fun sincronizarNuvemSePossivel() {
        val usuario = authRepository.currentUser.firstOrNull()
            ?: authRepository.signInAnonymously().getOrNull()
        usuario?.uid?.let { uid ->
            runCatching {
                withContext(Dispatchers.IO) {
                    repositorioSincronizacao.enviarTudo(uid)
                }
            }
        }
    }

    fun exportar() {
        viewModelScope.launch {
            val texto = runCatching { exportarRelatorioUC() }.getOrNull()
            _exportTextoPendente.value = texto
        }
    }

    fun aoConsumirExporte() {
        _exportTextoPendente.value = null
    }

    companion object {
        private const val DEFAULT_LOOKBACK_DAYS = 15L
    }
}

data class MonitoramentoUiState(
    val numeroOab: String = "",
    val ufOab: String = "",
    val dataInicio: String = "",
    val dataFim: String = "",
    val isLoading: Boolean = false,
    val lastResumo: MonitorarCnjResumo? = null,
    val syncStatus: SincronizacaoStatus = SincronizacaoStatus(),
    val syncFrequencyHours: Int = 24,
    val error: MonitoramentoUiError? = null,
    val semPublicacoesParaExportar: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isLoading &&
            numeroOab.isNotBlank() &&
            ufOab.trim().length == 2 &&
            dataInicio.isNotBlank() &&
            dataFim.isNotBlank()
}

sealed interface MonitoramentoUiError {
    data object InvalidDate : MonitoramentoUiError
    data object SyncFailed : MonitoramentoUiError
}
