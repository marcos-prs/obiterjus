package com.obiterjus.presentation.autenticacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.domain.model.AuthUser
import com.obiterjus.domain.model.SincronizacaoNuvemResumo
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.RepositorioSincronizacao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModeloAutenticacao(
    private val authRepository: AuthRepository,
    private val repositorioSincronizacao: RepositorioSincronizacao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoAutenticacao())
    val estado: StateFlow<EstadoAutenticacao> = _estado.asStateFlow()

    val currentUser: StateFlow<AuthUser?> = authRepository.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    init {
        viewModelScope.launch {
            ensureAuthenticated()
        }
        viewModelScope.launch {
            authRepository.currentUser.collect { usuario ->
                _estado.update { it.copy(usuario = usuario) }
            }
        }
    }

    private suspend fun ensureAuthenticated() {
        if (authRepository.currentUser.firstOrNull() == null) {
            authRepository.signInAnonymously()
        }
    }

    fun signOut() {
        executar(ErroAutenticacao.Sair) {
            authRepository.signOut()
            ensureAuthenticated()
            Result.success(Unit)
        }
    }

    fun signInEmail(email: String, pass: String) {
        executarComCredenciais(email, pass, ErroAutenticacao.EntrarEmail, restaurarNuvem = true) { emailTratado, senha ->
            authRepository.signInWithEmail(emailTratado, senha)
        }
    }

    fun signUpEmail(email: String, pass: String) {
        executarComCredenciais(email, pass, ErroAutenticacao.CriarConta, restaurarNuvem = true) { emailTratado, senha ->
            authRepository.signUpWithEmail(emailTratado, senha)
        }
    }

    fun signInGoogle(idToken: String) {
        executar(ErroAutenticacao.EntrarGoogle, restaurarNuvem = true) {
            authRepository.linkWithGoogle(idToken)
        }
    }

    fun limparErro() {
        _estado.update { it.copy(erro = null) }
    }

    private fun executarComCredenciais(
        email: String,
        senha: String,
        erroPadrao: ErroAutenticacao,
        restaurarNuvem: Boolean,
        acao: suspend (String, String) -> Result<AuthUser>,
    ) {
        val emailTratado = email.trim()
        if (emailTratado.isBlank() || senha.isBlank()) {
            _estado.update { it.copy(erro = ErroAutenticacao.CamposObrigatorios) }
            return
        }
        executar(erroPadrao, restaurarNuvem) {
            acao(emailTratado, senha)
        }
    }

    private fun executar(
        erroPadrao: ErroAutenticacao,
        restaurarNuvem: Boolean = false,
        acao: suspend () -> Result<*>,
    ) {
        viewModelScope.launch {
            _estado.update {
                it.copy(
                    isLoading = true,
                    erro = null,
                    sincronizacao = EstadoSincronizacaoNuvem.Ociosa,
                )
            }
            val resultado = acao()
            val usuarioResultado = resultado.getOrNull() as? AuthUser
            val resumoRestauracao = if (resultado.isSuccess && restaurarNuvem) {
                restaurarDadosDaNuvem(usuarioResultado)
            } else {
                null
            }
            _estado.update { estadoAtual ->
                estadoAtual.copy(
                    isLoading = false,
                    erro = if (resultado.isSuccess) null else erroPadrao,
                    sincronizacao = resumoRestauracao ?: estadoAtual.sincronizacao,
                )
            }
        }
    }

    private suspend fun restaurarDadosDaNuvem(usuarioAutenticado: AuthUser?): EstadoSincronizacaoNuvem {
        val usuario = usuarioAutenticado ?: authRepository.currentUser.firstOrNull()
            ?: return EstadoSincronizacaoNuvem.Falha
        if (usuario.isAnonymous) {
            return EstadoSincronizacaoNuvem.Ociosa
        }
        _estado.update { it.copy(sincronizacao = EstadoSincronizacaoNuvem.Sincronizando) }
        return runCatching {
            withContext(ioDispatcher) {
                repositorioSincronizacao.restaurarTudo(usuario.uid)
            }
        }.fold(
            onSuccess = EstadoSincronizacaoNuvem::Concluida,
            onFailure = { EstadoSincronizacaoNuvem.Falha },
        )
    }
}

data class EstadoAutenticacao(
    val usuario: AuthUser? = null,
    val isLoading: Boolean = false,
    val erro: ErroAutenticacao? = null,
    val sincronizacao: EstadoSincronizacaoNuvem = EstadoSincronizacaoNuvem.Ociosa,
)

sealed interface ErroAutenticacao {
    data object CamposObrigatorios : ErroAutenticacao
    data object EntrarEmail : ErroAutenticacao
    data object CriarConta : ErroAutenticacao
    data object EntrarGoogle : ErroAutenticacao
    data object Sair : ErroAutenticacao
}

sealed interface EstadoSincronizacaoNuvem {
    data object Ociosa : EstadoSincronizacaoNuvem
    data object Sincronizando : EstadoSincronizacaoNuvem
    data class Concluida(val resumo: SincronizacaoNuvemResumo) : EstadoSincronizacaoNuvem
    data object Falha : EstadoSincronizacaoNuvem
}
