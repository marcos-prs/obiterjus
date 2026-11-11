package com.obiterjus.presentation.perfil

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.R
import com.obiterjus.core.config.AppConfig
import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.data.djen.DjenSyncExecutor
import com.obiterjus.data.push.PushRegistroRepository
import com.obiterjus.data.settings.PerfilPreferences
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.data.settings.SessaoLocalRepository
import com.obiterjus.domain.model.AuthUser
import com.obiterjus.domain.model.OabCadastro
import com.obiterjus.domain.model.MonitorarDjenModo
import com.obiterjus.domain.model.SincronizacaoStatus
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.CadastroOabRepository
import com.obiterjus.domain.repository.SincronizacaoRepository
import com.obiterjus.domain.usecase.AplicarPreferenciaSincronizacaoUC
import com.obiterjus.ui.theme.TipoTema
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EstadoPerfil(
    val nomeUsuario: String? = null,
    val email: String? = null,
    val oab: String = "",
    val uf: String = "",
    val tipoInscricao: String = "",
    val intervaloBuscaDias: Int = 0,
    val sincronizacaoAutomatica: Boolean = true,
    val notificarPublicacoes: Boolean = true,
    val notificarPrazosUrgentes: Boolean = true,
    val notificarMovimentacoes: Boolean = true,
    val fontePrincipal: String = "",
    val enriquecimento: String = "",
    val statusSincronizacaoNuvem: String? = null,
    val autenticado: Boolean = false,
    val sincronizando: Boolean = false,
    val tema: TipoTema = TipoTema.SISTEMA,
    val apenasPorNome: Boolean = false,
    val sincronizacaoNuvemAutorizada: Boolean = false,
    val pushPreChecagemAutorizada: Boolean = false,
    /**
     * Só então o toggle da pré-checagem aparece. Fica atrás de flag porque
     * pedir essa autorização antes de a política de privacidade descrever o que
     * o servidor passa a guardar seria pedir consentimento para algo não
     * contado.
     */
    val pushPreChecagemDisponivel: Boolean = false,
)

class PerfilViewModel(
    private val context: Context,
    private val authRepository: AuthRepository,
    repositorioCadastroOab: CadastroOabRepository,
    private val djenSyncExecutor: DjenSyncExecutor,
    private val perfilPreferencesRepository: PerfilPreferencesRepository,
    private val aplicarPreferenciaSincronizacao: AplicarPreferenciaSincronizacaoUC,
    private val pushRegistro: PushRegistroRepository,
    private val appConfigRepository: AppConfigRepository,
    private val repositorioSincronizacao: SincronizacaoRepository,
    private val sessaoLocal: SessaoLocalRepository,
) : ViewModel() {
    private val _sincronizando = MutableStateFlow(false)
    private val _mensagemSucesso = MutableStateFlow<String?>(null)
    private val _mensagemErro = MutableStateFlow<String?>(null)
    private val _statusNuvemOverride = MutableStateFlow<String?>(null)

    private val _mostrarMenuConta = MutableStateFlow(false)
    val mostrarMenuConta: StateFlow<Boolean> = _mostrarMenuConta.asStateFlow()

    val mensagemSucesso: StateFlow<String?> = _mensagemSucesso.asStateFlow()
    val mensagemErro: StateFlow<String?> = _mensagemErro.asStateFlow()

    val estado: StateFlow<EstadoPerfil> = combine(
        authRepository.currentUser,
        repositorioCadastroOab.cadastro,
        repositorioCadastroOab.status,
        perfilPreferencesRepository.preferencias,
        _sincronizando,
        _statusNuvemOverride,
        appConfigRepository.config,
    ) { values: Array<Any?> ->
        val usuario = values[0] as AuthUser?
        val cadastro = values[1] as OabCadastro
        val status = values[2] as SincronizacaoStatus
        val preferencias = values[3] as PerfilPreferences
        val sincronizando = values[4] as Boolean
        val statusOverride = values[5] as String?
        val appConfig = values[6] as AppConfig
        val autenticado = usuario != null && !usuario.isAnonymous
        EstadoPerfil(
            nomeUsuario = cadastro.nomeAdvogado.ifBlank {
                nomeExibicao(usuario).orEmpty()
            },
            email = usuario?.email,
            autenticado = autenticado,
            oab = cadastro.numero,
            uf = cadastro.uf,
            tipoInscricao = cadastro.tipoInscricao,
            intervaloBuscaDias = preferencias.intervaloBuscaDias,
            sincronizacaoAutomatica = preferencias.sincronizacaoAutomatica,
            notificarPublicacoes = preferencias.notificarPublicacoes,
            notificarPrazosUrgentes = preferencias.notificarPrazosUrgentes,
            notificarMovimentacoes = preferencias.notificarMovimentacoes,
            fontePrincipal = context.getString(R.string.perfil_fonte_djen),
            enriquecimento = context.getString(R.string.perfil_enriquecimento_datajud),
            statusSincronizacaoNuvem = statusOverride ?: when {
                status.ultimaFalha != null -> status.ultimaFalha
                status.ultimoSucessoEm != null -> context.getString(R.string.perfil_status_nuvem_ok)
                autenticado -> context.getString(R.string.perfil_status_nuvem_ok)
                else -> context.getString(R.string.perfil_status_nuvem_anonimo)
            },
            sincronizando = sincronizando,
            tema = preferencias.tema,
            apenasPorNome = preferencias.apenasPorNome,
            sincronizacaoNuvemAutorizada = preferencias.sincronizacaoNuvemAutorizada,
            pushPreChecagemAutorizada = preferencias.pushPreChecagemAutorizada,
            pushPreChecagemDisponivel = appConfig.pushPreChecagemDisponivel,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoPerfil(),
    )

    fun aoAlternarSincronizacaoAutomatica(ativo: Boolean) {
        viewModelScope.launch {
            aplicarPreferenciaSincronizacao(ativo)
            // Desligar aqui também precisa avisar o servidor de push, senão ele
            // seguiria acordando um aparelho que pediu para parar. `sincronizar`
            // cuida dos dois sentidos: registra se ligado, cancela se desligado.
            pushRegistro.sincronizar()
            syncPerfilToCloud()
        }
    }

    /**
     * Autoriza o servidor a consultar o DJEN pela OAB para decidir se vale
     * acordar o aparelho. Sem isso o push chega cego, todo dia, no horário.
     */
    fun aoAlternarPushPreChecagem(ativo: Boolean) {
        viewModelScope.launch {
            perfilPreferencesRepository.savePushPreChecagemAutorizada(ativo)
            pushRegistro.sincronizar()
        }
    }

    fun aoAlternarNotificarPublicacoes(ativo: Boolean) {
        viewModelScope.launch {
            perfilPreferencesRepository.saveNotificarPublicacoes(ativo)
            syncPerfilToCloud()
        }
    }

    fun aoAlternarNotificarPrazos(ativo: Boolean) {
        viewModelScope.launch {
            perfilPreferencesRepository.saveNotificarPrazosUrgentes(ativo)
            syncPerfilToCloud()
        }
    }

    fun aoAlternarNotificarMovimentacoes(ativo: Boolean) {
        viewModelScope.launch {
            perfilPreferencesRepository.saveNotificarMovimentacoes(ativo)
            syncPerfilToCloud()
        }
    }

    fun aoAlterarTema(tema: TipoTema) {
        viewModelScope.launch {
            perfilPreferencesRepository.saveTema(tema)
            syncPerfilToCloud()
        }
    }

    fun aoAlternarApenasPorNome(ativo: Boolean) {
        viewModelScope.launch {
            perfilPreferencesRepository.saveApenasPorNome(ativo)
            syncPerfilToCloud()
        }
    }

    /**
     * O envio automático leva o inteiro teor das publicações para o Firestore,
     * então depende de escolha explícita — nasce desligado.
     */
    fun aoAlternarSincronizacaoNuvem(ativo: Boolean) {
        viewModelScope.launch {
            perfilPreferencesRepository.saveSincronizacaoNuvemAutorizada(ativo)
        }
    }

    fun aoForcarSincronizacao() {
        if (_sincronizando.value) return

        viewModelScope.launch {
            _sincronizando.value = true
            _mensagemErro.value = null
            _mensagemSucesso.value = null

            try {
                val resumo = withContext(Dispatchers.IO) {
                    djenSyncExecutor.executar(MonitorarDjenModo.MANUAL)
                }
                _mensagemSucesso.value = context.getString(
                    R.string.perfil_sync_sucesso,
                    resumo.totalProcessosSincronizados,
                    resumo.djen.novas,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _mensagemErro.value = context.getString(R.string.perfil_sync_falha)
            } finally {
                _sincronizando.value = false
            }
        }
    }

    /**
     * Sair da conta apaga o acervo do aparelho. Sem isso, o próximo advogado a
     * entrar enxergava os processos e clientes do anterior — e a primeira
     * sincronização os enviava para a conta dele na nuvem.
     */
    fun aoLogout() {
        viewModelScope.launch {
            _mensagemErro.value = null
            _mensagemSucesso.value = null
            runCatching {
                authRepository.signOut()
                sessaoLocal.limparTudo()
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

    fun aoAbrirMenu() {
        _mostrarMenuConta.value = true
    }

    fun aoFecharMenu() {
        _mostrarMenuConta.value = false
    }

    private fun nomeExibicao(usuario: AuthUser?): String? =
        usuario?.email?.substringBefore("@")?.replaceFirstChar(Char::uppercaseChar)

    private suspend fun syncPerfilToCloud() {
        val user = authRepository.currentUser.first() ?: return
        if (user.isAnonymous) return
        repositorioSincronizacao.enviarPerfil(user.uid)
    }

}
