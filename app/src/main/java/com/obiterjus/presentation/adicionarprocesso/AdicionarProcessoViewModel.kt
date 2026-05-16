package com.obiterjus.presentation.adicionarprocesso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.core.parser.NumeroProcessoNormalizer
import com.obiterjus.domain.model.ProcessoDataJudSyncStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.domain.usecase.AdicionarProcessoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdicionarProcessoViewModel(
    private val adicionarProcesso: AdicionarProcessoUseCase,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoAdicionarProcesso())
    val estado: StateFlow<EstadoAdicionarProcesso> = _estado

    fun aoAlterarNumero(valor: String) {
        _estado.update { it.copy(numeroInput = valor, status = StatusAdicao.IDLE) }
    }

    fun aoBuscar() {
        val numero = NumeroProcessoNormalizer.normalize(_estado.value.numeroInput)
        if (numero == null) {
            _estado.update { it.copy(status = StatusAdicao.NUMERO_INVALIDO) }
            return
        }
        _estado.update { it.copy(status = StatusAdicao.BUSCANDO) }
        viewModelScope.launch {
            try {
                val resumo = adicionarProcesso(numero)
                val resultado = resumo.resultados.firstOrNull()
                val status = when (resultado?.status) {
                    ProcessoDataJudSyncStatus.FOUND -> StatusAdicao.SUCESSO
                    ProcessoDataJudSyncStatus.PENDING -> StatusAdicao.SUCESSO_PENDENTE
                    ProcessoDataJudSyncStatus.NOT_FOUND -> StatusAdicao.NAO_ENCONTRADO
                    ProcessoDataJudSyncStatus.FAILED -> StatusAdicao.ERRO_API
                    null -> StatusAdicao.FALHA
                }
                _estado.update {
                    it.copy(
                        status = status,
                        mensagemErro = resultado?.mensagem,
                        processoSyncResumo = resumo,
                    )
                }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(
                        status = StatusAdicao.FALHA,
                        mensagemErro = e.message,
                    )
                }
            }
        }
    }
}

data class EstadoAdicionarProcesso(
    val numeroInput: String = "",
    val status: StatusAdicao = StatusAdicao.IDLE,
    val mensagemErro: String? = null,
    val processoSyncResumo: SincronizarProcessosDataJudResumo? = null,
)

enum class StatusAdicao {
    IDLE,
    NUMERO_INVALIDO,
    BUSCANDO,
    SUCESSO,
    SUCESSO_PENDENTE,
    NAO_ENCONTRADO,
    ERRO_API,
    FALHA,
}
