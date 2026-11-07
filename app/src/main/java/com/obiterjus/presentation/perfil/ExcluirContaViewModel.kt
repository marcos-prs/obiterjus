package com.obiterjus.presentation.perfil

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.R
import com.obiterjus.core.worker.DjenSyncScheduler
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.usecase.ExcluirContaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoExcluirConta(
    val email: String = "",
    val senha: String = "",
    val pedeSenha: Boolean = true,
    val cienteDoEfeito: Boolean = false,
    val confirmando: Boolean = false,
    val excluindo: Boolean = false,
    val excluida: Boolean = false,
    val mensagemErro: String? = null,
) {
    val podeExcluir: Boolean
        get() = cienteDoEfeito && !excluindo && (!pedeSenha || senha.isNotBlank())
}

class ExcluirContaViewModel(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val excluirConta: ExcluirContaUseCase,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoExcluirConta())
    val estado: StateFlow<EstadoExcluirConta> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val usuario = authRepository.currentUser.first()
            _estado.update { estado ->
                estado.copy(
                    email = usuario?.email.orEmpty(),
                    pedeSenha = authRepository.temProvedorSenha(),
                )
            }
        }
    }

    fun aoAlterarSenha(valor: String) {
        _estado.update { it.copy(senha = valor, mensagemErro = null) }
    }

    fun aoAlternarCiencia(valor: Boolean) {
        _estado.update { it.copy(cienteDoEfeito = valor) }
    }

    fun aoPedirConfirmacao() {
        _estado.update { it.copy(confirmando = true) }
    }

    fun aoDesistir() {
        _estado.update { it.copy(confirmando = false) }
    }

    fun aoConfirmarExclusao() {
        val estadoAtual = _estado.value
        if (!estadoAtual.podeExcluir) return

        viewModelScope.launch {
            _estado.update { it.copy(confirmando = false, excluindo = true, mensagemErro = null) }

            when (excluirConta(estadoAtual.senha)) {
                ExcluirContaUseCase.Resultado.Concluida -> {
                    // A sync das 7h sobreviveria à conta: o WorkManager não sabe
                    // que não há mais sessão e tentaria buscar publicações.
                    DjenSyncScheduler.cancel(context)
                    _estado.update {
                        it.copy(senha = "", excluindo = false, excluida = true)
                    }
                }

                ExcluirContaUseCase.Resultado.SemSessao -> falhar(R.string.excluir_conta_erro_sem_sessao)

                ExcluirContaUseCase.Resultado.SenhaInvalida -> falhar(R.string.excluir_conta_erro_senha)

                is ExcluirContaUseCase.Resultado.FalhaNaNuvem -> falhar(R.string.excluir_conta_erro_nuvem)

                is ExcluirContaUseCase.Resultado.FalhaNaConta -> falhar(R.string.excluir_conta_erro_conta)
            }
        }
    }

    fun aoConsumirErro() {
        _estado.update { it.copy(mensagemErro = null) }
    }

    private fun falhar(@StringRes mensagem: Int) {
        _estado.update {
            it.copy(excluindo = false, mensagemErro = context.getString(mensagem))
        }
    }
}
