package com.obiterjus.presentation.perfil
import com.obiterjus.ui.theme.TipoTema

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.R
import com.obiterjus.data.settings.PerfilPreferences
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.data.settings.SyncPreferencesRepository
import com.obiterjus.domain.model.AuthUser
import com.obiterjus.domain.model.OabCadastro
import com.obiterjus.domain.model.SincronizacaoStatus
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.RepositorioCadastroOab
import com.obiterjus.domain.repository.RepositorioSincronizacao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EstadoPerfil(
    val nomeUsuario: String? = null,
    val email: String? = null,
    val oab: String = "",
    val uf: String = "",
    val intervaloBuscaDias: Int = 0,
    val sincronizacaoAutomatica: Boolean = true,
    val frequenciaSincronizacao: String = "",
    val notificarPublicacoes: Boolean = true,
    val notificarPrazosUrgentes: Boolean = true,
    val notificarMovimentacoes: Boolean = true,
    val fontePrincipal: String = "",
    val enriquecimento: String = "",
    val statusSincronizacaoNuvem: String? = null,
    val autenticado: Boolean = false,
    val sincronizando: Boolean = false,
    val tema: TipoTema = TipoTema.SISTEMA,
)

class ModeloPerfil(
    private val context: Context,
    private val authRepository: AuthRepository,
    repositorioCadastroOab: RepositorioCadastroOab,
    private val repositorioSincronizacao: RepositorioSincronizacao,
    syncPreferencesRepository: SyncPreferencesRepository,
    private val perfilPreferencesRepository: PerfilPreferencesRepository,
) : ViewModel() {
    private val _sincronizando = MutableStateFlow(false)
    private val _mensagemSucesso = MutableStateFlow<String?>(null)
    private val _mensagemErro = MutableStateFlow<String?>(null)
    private val _statusNuvemOverride = MutableStateFlow<String?>(null)

    val mensagemSucesso: StateFlow<String?> = _mensagemSucesso.asStateFlow()
    val mensagemErro: StateFlow<String?> = _mensagemErro.asStateFlow()

    val estado: StateFlow<EstadoPerfil> = combine(
        authRepository.currentUser,
        repositorioCadastroOab.cadastro,
        repositorioCadastroOab.status,
        syncPreferencesRepository.syncFrequencyHours,
        perfilPreferencesRepository.preferencias,
        _sincronizando,
        _statusNuvemOverride,
    ) { values: Array<Any?> ->
        val usuario = values[0] as AuthUser?
        val cadastro = values[1] as OabCadastro
        val status = values[2] as SincronizacaoStatus
        val frequencia = values[3] as Int
        val preferencias = values[4] as PerfilPreferences
        val sincronizando = values[5] as Boolean
        val statusOverride = values[6] as String?
        val autenticado = usuario != null && !usuario.isAnonymous
        EstadoPerfil(
            nomeUsuario = cadastro.nomeAdvogado.ifBlank {
                nomeExibicao(usuario).orEmpty()
            },
            email = usuario?.email,
            autenticado = autenticado,
            oab = cadastro.numero,
            uf = cadastro.uf,
            intervaloBuscaDias = preferencias.intervaloBuscaDias,
            sincronizacaoAutomatica = preferencias.sincronizacaoAutomatica,
            frequenciaSincronizacao = rotuloFrequencia(frequencia),
            notificarPublicacoes = preferencias.notificarPublicacoes,
            notificarPrazosUrgentes = preferencias.notificarPrazosUrgentes,
            notificarMovimentacoes = preferencias.notificarMovimentacoes,
            fontePrincipal = context.getString(R.string.perfil_fonte_djen),
            enriquecimento = context.getString(R.string.perfil_enriquecimento_datajud),
            statusSincronizacaoNuvem = statusOverride ?: when {
                status.ultimoSucessoEm != null -> context.getString(R.string.perfil_status_nuvem_ok)
                status.ultimaFalha != null -> status.ultimaFalha
                autenticado -> context.getString(R.string.perfil_status_nuvem_ok)
                else -> context.getString(R.string.perfil_status_nuvem_anonimo)
            },
            sincronizando = sincronizando,
            tema = preferencias.tema,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoPerfil(),
    )

    fun aoAlternarSincronizacaoAutomatica(ativo: Boolean) {
        viewModelScope.launch {
            perfilPreferencesRepository.saveSincronizacaoAutomatica(ativo)
        }
    }

    fun aoAlternarNotificarPublicacoes(ativo: Boolean) {
        viewModelScope.launch {
            perfilPreferencesRepository.saveNotificarPublicacoes(ativo)
        }
    }

    fun aoAlternarNotificarPrazos(ativo: Boolean) {
        viewModelScope.launch {
            perfilPreferencesRepository.saveNotificarPrazosUrgentes(ativo)
        }
    }

    fun aoAlternarNotificarMovimentacoes(ativo: Boolean) {
        viewModelScope.launch {
            perfilPreferencesRepository.saveNotificarMovimentacoes(ativo)
        }
    }

    fun aoAlterarTema(tema: TipoTema) {
        viewModelScope.launch {
            perfilPreferencesRepository.saveTema(tema)
        }
    }

    fun aoForcarSincronizacao() {
        if (_sincronizando.value) return

        viewModelScope.launch {
            _sincronizando.value = true
            _mensagemErro.value = null
            _mensagemSucesso.value = null

            val usuario = authRepository.currentUser.firstOrNull()
                ?: authRepository.signInAnonymously().getOrNull()

            if (usuario == null) {
                _sincronizando.value = false
                _mensagemErro.value = context.getString(R.string.perfil_sync_falha)
                return@launch
            }

            val resultado = runCatching {
                withContext(Dispatchers.IO) {
                    repositorioSincronizacao.enviarTudo(usuario.uid)
                }
            }

            resultado.fold(
                onSuccess = { resumo ->
                    _statusNuvemOverride.value = context.getString(R.string.perfil_status_nuvem_ok)
                    _mensagemSucesso.value = context.getString(
                        R.string.perfil_sync_sucesso,
                        resumo.processos,
                        resumo.publicacoes,
                    )
                },
                onFailure = {
                    _mensagemErro.value = context.getString(R.string.perfil_sync_falha)
                },
            )

            _sincronizando.value = false
        }
    }

    fun aoLogout() {
        viewModelScope.launch {
            _mensagemErro.value = null
            _mensagemSucesso.value = null
            runCatching {
                authRepository.signOut()
            }.onSuccess {
                _statusNuvemOverride.value = null
                _mensagemSucesso.value = context.getString(R.string.perfil_logout_sucesso)
            }.onFailure {
                _mensagemErro.value = context.getString(R.string.perfil_logout_falha)
            }
        }
    }

    fun aoConsumirMensagemSucesso() {
        _mensagemSucesso.value = null
    }

    fun aoConsumirMensagemErro() {
        _mensagemErro.value = null
    }

    private fun nomeExibicao(usuario: AuthUser?): String? =
        usuario?.email?.substringBefore("@")?.replaceFirstChar(Char::uppercaseChar)

    private fun rotuloFrequencia(horas: Int): String =
        when (horas) {
            24 -> context.getString(R.string.perfil_frequencia_diaria)
            else -> context.getString(R.string.perfil_frequencia_horas, horas)
        }
}
