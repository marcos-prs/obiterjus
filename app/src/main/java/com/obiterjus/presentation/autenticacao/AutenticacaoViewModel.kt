package com.obiterjus.presentation.autenticacao

import com.obiterjus.ui.theme.TipoTema
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.R
import com.obiterjus.data.auth.CredencialGoogleProvider
import com.obiterjus.data.auth.ResultadoCredencialGoogle
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.data.settings.SessaoLocalRepository
import com.obiterjus.domain.model.AuthUser
import com.obiterjus.domain.model.OabCadastro
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.CadastroOabRepository
import com.obiterjus.domain.repository.SincronizacaoRepository
import com.obiterjus.domain.usecase.AplicarPreferenciaSincronizacaoUC
import com.obiterjus.domain.usecase.MonitorarCnjUseCase
import com.obiterjus.domain.model.MonitorarDjenModo
import com.obiterjus.domain.model.MonitorarDjenParams
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ModoAutenticacao {
    ENTRAR,
    CADASTRAR,
}

enum class EtapaCadastro {
    CONTA,
    OAB,
    VERIFICACAO,
    PREFERENCIAS,
    RESUMO,
}

enum class TipoInscricaoCadastro {
    ADVOGADO,
    ESTAGIARIO,
    SOCIO,
}

enum class StatusVerificacao {
    PENDENTE,
    CARREGANDO,
    SUCESSO,
    ERRO,
    OPCIONAL,
}

data class ItemVerificacaoCadastro(
    val titulo: String,
    val detalhe: String,
    val status: StatusVerificacao = StatusVerificacao.PENDENTE,
)

data class EstadoAutenticacao(
    val modo: ModoAutenticacao = ModoAutenticacao.ENTRAR,
    val etapaCadastro: EtapaCadastro = EtapaCadastro.CONTA,
    val nomeCompleto: String = "",
    val email: String = "",
    val senha: String = "",
    val confirmarSenha: String = "",
    val aceitarTermos: Boolean = false,
    val aceitarPrivacidade: Boolean = false,
    val uf: String = "",
    val numeroOab: String = "",
    val tipoInscricao: TipoInscricaoCadastro = TipoInscricaoCadastro.ADVOGADO,
    val nomeEscritorio: String = "",
    val areasAtuacao: Set<String> = emptySet(),
    val janelaBuscaDias: Int = 30,
    val sincronizacaoAutomatica: Boolean = true,
    val notificarPublicacoes: Boolean = true,
    val notificarPrazosUrgentes: Boolean = true,
    val notificarMovimentacoes: Boolean = false,
    val temaEscuro: Boolean = false,
    val verificacaoDjen: ItemVerificacaoCadastro = ItemVerificacaoCadastro(
        titulo = "DJEN",
        detalhe = "",
    ),
    val verificacaoOab: ItemVerificacaoCadastro = ItemVerificacaoCadastro(
        titulo = "OAB",
        detalhe = "",
    ),
    val verificacaoBusca: ItemVerificacaoCadastro = ItemVerificacaoCadastro(
        titulo = "Busca",
        detalhe = "",
    ),
    val verificacaoDataJud: ItemVerificacaoCadastro = ItemVerificacaoCadastro(
        titulo = "DataJud",
        detalhe = "",
    ),
    val verificacaoIndexacao: ItemVerificacaoCadastro = ItemVerificacaoCadastro(
        titulo = "Indexação",
        detalhe = "",
    ),
    val carregando: Boolean = false,
    val mensagemErro: String? = null,
    val mensagemSucesso: String? = null,
    val autenticado: Boolean = false,
    val emailUsuarioAtual: String? = null,
    val resumoBuscaPublicacoes: Int = 0,
    /**
     * A etapa Conta foi cumprida por uma credencial Google: a conta já existe no
     * Firebase e não há senha neste cadastro. O resto do assistente segue igual.
     */
    val contaGoogle: Boolean = false,
) {
    val senhaConfere: Boolean
        get() = senha.isNotBlank() && senha == confirmarSenha

    val senhaValida: Boolean
        get() = senha.length >= 8 &&
            senha.any(Char::isUpperCase) &&
            senha.any(Char::isDigit)

    val aceitaPoliticas: Boolean
        get() = aceitarTermos && aceitarPrivacidade

    val contaValida: Boolean
        get() = nomeCompleto.isNotBlank() &&
            email.isNotBlank() &&
            aceitaPoliticas &&
            // Cadastro por Google não tem senha para validar.
            (contaGoogle || (senhaValida && senhaConfere))

    val oabValida: Boolean
        get() = uf.length == 2 && numeroOab.isNotBlank()
}

class AutenticacaoViewModel internal constructor(
    private val authRepository: AuthRepository,
    private val repositorioCadastroOab: CadastroOabRepository,
    private val repositorioSincronizacao: SincronizacaoRepository,
    private val monitorarCnjUseCase: MonitorarCnjUseCase,
    private val perfilPreferencesRepository: PerfilPreferencesRepository,
    private val aplicarPreferenciaSincronizacao: AplicarPreferenciaSincronizacaoUC,
    private val sessaoLocal: SessaoLocalRepository,
    private val credencialGoogle: CredencialGoogleProvider,
    private val textos: TextosAutenticacao,
) : ViewModel() {
    constructor(
        context: Context,
        authRepository: AuthRepository,
        repositorioCadastroOab: CadastroOabRepository,
        repositorioSincronizacao: SincronizacaoRepository,
        monitorarCnjUseCase: MonitorarCnjUseCase,
        perfilPreferencesRepository: PerfilPreferencesRepository,
        aplicarPreferenciaSincronizacao: AplicarPreferenciaSincronizacaoUC,
        sessaoLocal: SessaoLocalRepository,
        credencialGoogle: CredencialGoogleProvider,
    ) : this(
        authRepository = authRepository,
        repositorioCadastroOab = repositorioCadastroOab,
        repositorioSincronizacao = repositorioSincronizacao,
        monitorarCnjUseCase = monitorarCnjUseCase,
        perfilPreferencesRepository = perfilPreferencesRepository,
        aplicarPreferenciaSincronizacao = aplicarPreferenciaSincronizacao,
        sessaoLocal = sessaoLocal,
        credencialGoogle = credencialGoogle,
        textos = ContextTextosAutenticacao(context),
    )

    private val _estado = MutableStateFlow(EstadoAutenticacao())
    val estado: StateFlow<EstadoAutenticacao> = combine(
        _estado,
        authRepository.currentUser,
    ) { estadoAtual, usuario ->
        estadoAtual.copy(
            autenticado = usuario.naoAnonimo(),
            emailUsuarioAtual = usuario?.email,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoAutenticacao(),
    )
    fun aoSelecionarModo(modo: ModoAutenticacao) {
        _estado.update { current ->
            current.copy(
                modo = modo,
                etapaCadastro = if (modo == ModoAutenticacao.CADASTRAR) EtapaCadastro.CONTA else current.etapaCadastro,
                mensagemErro = null,
                mensagemSucesso = null,
                carregando = false,
            )
        }
    }

    fun aoSelecionarEtapa(index: Int) {
        val etapas = EtapaCadastro.entries
        if (index in etapas.indices) {
            _estado.update { it.copy(etapaCadastro = etapas[index]) }
        }
    }

    fun aoAlterarNome(valor: String) = atualizar { it.copy(nomeCompleto = valor) }
    fun aoAlterarEmail(valor: String) = atualizar { it.copy(email = valor) }
    fun aoAlterarSenha(valor: String) = atualizar { it.copy(senha = valor) }
    fun aoAlterarConfirmarSenha(valor: String) = atualizar { it.copy(confirmarSenha = valor) }
    fun aoAlternarTermos(ativo: Boolean) = atualizar { it.copy(aceitarTermos = ativo) }
    fun aoAlternarPrivacidade(ativo: Boolean) = atualizar { it.copy(aceitarPrivacidade = ativo) }
    fun aoAlterarUf(valor: String) = atualizar { it.copy(uf = valor.filter(Char::isLetter).take(2).uppercase()) }
    fun aoAlterarNumeroOab(valor: String) = atualizar { it.copy(numeroOab = valor.filter(Char::isDigit)) }
    fun aoAlterarNomeEscritorio(valor: String) = atualizar { it.copy(nomeEscritorio = valor) }
    fun aoAlterarTipoInscricao(tipo: TipoInscricaoCadastro) = atualizar { it.copy(tipoInscricao = tipo) }
    fun aoAlternarAreaAtuacao(area: String) = atualizar {
        val areas = it.areasAtuacao.toMutableSet()
        if (!areas.add(area)) {
            areas.remove(area)
        }
        it.copy(areasAtuacao = areas)
    }
    fun aoAlterarJanelaBusca(dias: Int) = atualizar {
        it.copy(janelaBuscaDias = dias)
    }
    fun aoAlternarSincronizacaoAutomatica(ativo: Boolean) = atualizar { it.copy(sincronizacaoAutomatica = ativo) }
    fun aoAlternarNotificarPublicacoes(ativo: Boolean) = atualizar { it.copy(notificarPublicacoes = ativo) }
    fun aoAlternarNotificarPrazos(ativo: Boolean) = atualizar { it.copy(notificarPrazosUrgentes = ativo) }
    fun aoAlternarNotificarMovimentacoes(ativo: Boolean) = atualizar { it.copy(notificarMovimentacoes = ativo) }
    fun aoAlternarTemaEscuro(ativo: Boolean) = atualizar { it.copy(temaEscuro = ativo) }

    fun aoEntrar() {
        val estadoAtual = _estado.value
        if (estadoAtual.carregando) return

        val email = estadoAtual.email.trim()
        val senha = estadoAtual.senha
        if (email.isBlank() || senha.isBlank()) {
            _estado.update { it.copy(mensagemErro = textos.get(R.string.autenticacao_error_required_fields)) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, mensagemErro = null, mensagemSucesso = null) }
            authRepository.signInWithEmail(email, senha).fold(
                onSuccess = { authUser -> concluirLogin(authUser) },
                onFailure = {
                    _estado.update {
                        it.copy(
                            carregando = false,
                            mensagemErro = textos.get(R.string.autenticacao_error_login_email),
                        )
                    }
                },
            )
        }
    }

    /**
     * Acesso com Google. Serve de entrada e de cadastro: na primeira vez o
     * Firebase cria a conta a partir da credencial, sem passar pelo assistente
     * de e-mail/senha — a OAB e as preferências seguem editáveis no Perfil.
     *
     * [contextoDaTela] tem de ser o contexto da Activity: o seletor de contas do
     * Credential Manager não abre a partir do contexto de aplicação.
     */
    fun aoEntrarComGoogle(contextoDaTela: Context?) {
        if (_estado.value.carregando) return

        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, mensagemErro = null, mensagemSucesso = null) }
            val (authUser, _) = autenticarComGoogle(contextoDaTela) ?: return@launch
            concluirLogin(authUser)
        }
    }

    /**
     * Mesma credencial Google, outro destino: aqui ela cumpre a etapa Conta do
     * assistente e o cadastro continua nas etapas de OAB, verificação e
     * preferências, que é o que faz o monitoramento DJEN funcionar depois.
     *
     * Por isso não marca [EstadoAutenticacao.mensagemSucesso]: ela é o gatilho de
     * saída da tela, e sair aqui abandonaria o cadastro pela metade.
     *
     * Exige os aceites antes de pedir a credencial — a conta no Firebase nasce
     * desta chamada, e não se cria conta antes de aceitar os termos.
     */
    fun aoCadastrarComGoogle(contextoDaTela: Context?) {
        val estadoAtual = _estado.value
        if (estadoAtual.carregando) return
        if (!estadoAtual.aceitaPoliticas) {
            _estado.update { it.copy(mensagemErro = textos.get(R.string.autenticacao_error_politicas)) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, mensagemErro = null, mensagemSucesso = null) }
            val (authUser, credencial) = autenticarComGoogle(contextoDaTela) ?: return@launch

            // A credencial também entra em conta que já existe. Traz o que houver
            // na nuvem antes de adiantar os campos: sem isso o assistente seguiria
            // em branco e o `enviarPerfil` do fim sobrescreveria o perfil bom.
            val restauracao = restaurarDaNuvem(authUser.uid)
            val cadastro = runCatching { repositorioCadastroOab.cadastro.first() }.getOrNull()
            val preferencias = runCatching { perfilPreferencesRepository.preferencias.first() }.getOrNull()

            _estado.update { atual ->
                atual.copy(
                    carregando = false,
                    contaGoogle = true,
                    senha = "",
                    confirmarSenha = "",
                    nomeCompleto = credencial.nomeExibicao
                        ?: cadastro?.nomeAdvogado?.takeIf { it.isNotBlank() }
                        ?: atual.nomeCompleto,
                    email = credencial.email ?: authUser.email ?: atual.email,
                    uf = cadastro?.uf?.takeIf { it.isNotBlank() } ?: atual.uf,
                    numeroOab = cadastro?.numero?.takeIf { it.isNotBlank() } ?: atual.numeroOab,
                    tipoInscricao = cadastro?.tipoInscricao?.let(::tipoInscricaoDe) ?: atual.tipoInscricao,
                    nomeEscritorio = cadastro?.nomeEscritorio?.takeIf { it.isNotBlank() } ?: atual.nomeEscritorio,
                    areasAtuacao = cadastro?.areasAtuacao?.takeIf { it.isNotEmpty() }?.toSet() ?: atual.areasAtuacao,
                    janelaBuscaDias = preferencias?.intervaloBuscaDias ?: atual.janelaBuscaDias,
                    sincronizacaoAutomatica = preferencias?.sincronizacaoAutomatica ?: atual.sincronizacaoAutomatica,
                    notificarPublicacoes = preferencias?.notificarPublicacoes ?: atual.notificarPublicacoes,
                    notificarPrazosUrgentes = preferencias?.notificarPrazosUrgentes ?: atual.notificarPrazosUrgentes,
                    notificarMovimentacoes = preferencias?.notificarMovimentacoes ?: atual.notificarMovimentacoes,
                    temaEscuro = preferencias?.let { it.tema == TipoTema.ESCURO } ?: atual.temaEscuro,
                    etapaCadastro = EtapaCadastro.OAB,
                    mensagemErro = if (restauracao.isFailure) {
                        textos.get(R.string.autenticacao_error_restaurar)
                    } else {
                        null
                    },
                )
            }
        }
    }

    /**
     * Pede a credencial ao sistema e a troca por sessão no Firebase. Em qualquer
     * desfecho que não seja sucesso já deixa o estado resolvido — mensagem de
     * erro, ou apenas o fim do carregamento quando o advogado desistiu — e
     * devolve null para quem chamou apenas encerrar.
     */
    private suspend fun autenticarComGoogle(
        contextoDaTela: Context?,
    ): Pair<AuthUser, ResultadoCredencialGoogle.Sucesso>? =
        when (val resultado = credencialGoogle.obterIdToken(contextoDaTela)) {
            // Fechar o seletor é desistência, não falha: nada de mensagem.
            ResultadoCredencialGoogle.Cancelado -> {
                _estado.update { it.copy(carregando = false) }
                null
            }

            ResultadoCredencialGoogle.SemConta -> {
                _estado.update {
                    it.copy(
                        carregando = false,
                        mensagemErro = textos.get(R.string.autenticacao_error_google_sem_conta),
                    )
                }
                null
            }

            is ResultadoCredencialGoogle.Falha -> {
                _estado.update {
                    it.copy(
                        carregando = false,
                        mensagemErro = textos.get(R.string.autenticacao_error_login_google),
                    )
                }
                null
            }

            is ResultadoCredencialGoogle.Sucesso -> {
                val authUser = authRepository.linkWithGoogle(resultado.idToken).getOrNull()
                if (authUser == null) {
                    _estado.update {
                        it.copy(
                            carregando = false,
                            mensagemErro = textos.get(R.string.autenticacao_error_login_google),
                        )
                    }
                    null
                } else {
                    authUser to resultado
                }
            }
        }

    private fun tipoInscricaoDe(valor: String): TipoInscricaoCadastro? =
        runCatching { TipoInscricaoCadastro.valueOf(valor) }.getOrNull()

    /**
     * Fecha o login: deixa o aparelho pronto para o uid e traz o que a conta tem
     * na nuvem.
     *
     * A restauração chamava o Firestore fora de runCatching: sem rede, a exceção
     * escapava do launch e derrubava o app logo depois de um login bem-sucedido.
     */
    private suspend fun concluirLogin(authUser: AuthUser) {
        val restauracao = restaurarDaNuvem(authUser.uid)
        _estado.update {
            it.copy(
                carregando = false,
                senha = "",
                confirmarSenha = "",
                mensagemSucesso = textos.get(R.string.autenticacao_sucesso_login),
                mensagemErro = if (restauracao.isFailure) {
                    textos.get(R.string.autenticacao_error_restaurar)
                } else {
                    null
                },
            )
        }
    }

    fun aoEnviarRedefinicaoSenha() {
        val estadoAtual = _estado.value
        if (estadoAtual.carregando) return

        val email = estadoAtual.email.trim()
        if (email.isBlank()) {
            _estado.update { it.copy(mensagemErro = textos.get(R.string.autenticacao_error_email_required)) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, mensagemErro = null, mensagemSucesso = null) }
            authRepository.sendPasswordResetEmail(email).fold(
                onSuccess = {
                    _estado.update {
                        it.copy(
                            carregando = false,
                            mensagemSucesso = textos.get(R.string.autenticacao_sucesso_redefinicao_senha),
                        )
                    }
                },
                onFailure = {
                    _estado.update {
                        it.copy(
                            carregando = false,
                            mensagemErro = textos.get(R.string.autenticacao_error_redefinicao_senha),
                        )
                    }
                },
            )
        }
    }

    fun aoIrParaCadastro() {
        aoSelecionarModo(ModoAutenticacao.CADASTRAR)
    }

    fun aoVoltarEtapaOuModo() {
        _estado.update { current ->
            when {
                current.modo == ModoAutenticacao.CADASTRAR && current.etapaCadastro != EtapaCadastro.CONTA ->
                    current.copy(etapaCadastro = etapaAnterior(current.etapaCadastro), mensagemErro = null)
                current.modo == ModoAutenticacao.CADASTRAR ->
                    current.copy(modo = ModoAutenticacao.ENTRAR, mensagemErro = null, mensagemSucesso = null)
                else -> current.copy(mensagemErro = null, mensagemSucesso = null)
            }
        }
    }

    fun aoAvancarCadastro() {
        val estadoAtual = _estado.value
        when (estadoAtual.etapaCadastro) {
            EtapaCadastro.CONTA -> if (estadoAtual.contaValida) {
                _estado.update { it.copy(etapaCadastro = EtapaCadastro.OAB, mensagemErro = null) }
            } else {
                _estado.update { it.copy(mensagemErro = textos.get(R.string.autenticacao_error_conta_invalida)) }
            }

            EtapaCadastro.OAB -> if (estadoAtual.oabValida) {
                viewModelScope.launch {
                    repositorioCadastroOab.salvarCadastro(
                        numero = estadoAtual.numeroOab,
                        uf = estadoAtual.uf,
                        nomeAdvogado = estadoAtual.nomeCompleto,
                        tipoInscricao = estadoAtual.tipoInscricao.name,
                        nomeEscritorio = estadoAtual.nomeEscritorio,
                        areasAtuacao = estadoAtual.areasAtuacao.toList(),
                    )
                    _estado.update { it.copy(etapaCadastro = EtapaCadastro.VERIFICACAO, mensagemErro = null) }
                    executarVerificacaoAutomatica()
                }
            } else {
                _estado.update { it.copy(mensagemErro = textos.get(R.string.autenticacao_error_oab_invalida)) }
            }

            EtapaCadastro.VERIFICACAO -> {
                _estado.update { it.copy(etapaCadastro = EtapaCadastro.PREFERENCIAS, mensagemErro = null) }
            }

            EtapaCadastro.PREFERENCIAS -> {
                viewModelScope.launch {
                    perfilPreferencesRepository.saveIntervaloBuscaDias(estadoAtual.janelaBuscaDias)
                    aplicarPreferenciaSincronizacao(estadoAtual.sincronizacaoAutomatica)
                    perfilPreferencesRepository.saveNotificarPublicacoes(estadoAtual.notificarPublicacoes)
                    perfilPreferencesRepository.saveNotificarPrazosUrgentes(estadoAtual.notificarPrazosUrgentes)
                    perfilPreferencesRepository.saveNotificarMovimentacoes(estadoAtual.notificarMovimentacoes)
                    perfilPreferencesRepository.saveTema(if (estadoAtual.temaEscuro) TipoTema.ESCURO else TipoTema.CLARO)
                    _estado.update { it.copy(etapaCadastro = EtapaCadastro.RESUMO, mensagemErro = null) }
                }
            }

            EtapaCadastro.RESUMO -> finalizarCadastro()
        }
    }

    fun aoContinuarSemValidacao() {
        _estado.update { it.copy(etapaCadastro = EtapaCadastro.PREFERENCIAS, mensagemErro = null) }
    }

    fun aoReiniciarValidacao() {
        _estado.update {
            it.copy(
                etapaCadastro = EtapaCadastro.OAB,
                verificacaoDjen = it.verificacaoDjen.copy(status = StatusVerificacao.PENDENTE),
                verificacaoOab = it.verificacaoOab.copy(status = StatusVerificacao.PENDENTE),
                verificacaoBusca = it.verificacaoBusca.copy(status = StatusVerificacao.PENDENTE),
                verificacaoDataJud = it.verificacaoDataJud.copy(status = StatusVerificacao.PENDENTE),
                verificacaoIndexacao = it.verificacaoIndexacao.copy(status = StatusVerificacao.PENDENTE),
                mensagemErro = null,
            )
        }
    }

    fun aoConsumirMensagemErro() {
        _estado.update { it.copy(mensagemErro = null) }
    }

    fun aoConsumirMensagemSucesso() {
        _estado.update { it.copy(mensagemSucesso = null) }
    }

    private fun finalizarCadastro() {
        val estadoAtual = _estado.value
        if (estadoAtual.carregando) return

        if (estadoAtual.contaGoogle) {
            // A conta nasceu da credencial na etapa Conta; aqui só falta publicar
            // o perfil que o assistente acabou de coletar. Chamar signUp de novo
            // tentaria criar uma segunda conta, agora com senha.
            viewModelScope.launch {
                _estado.update { it.copy(carregando = true, mensagemErro = null, mensagemSucesso = null) }
                val uid = authRepository.currentUser.firstOrNull()?.uid
                if (uid == null) {
                    _estado.update {
                        it.copy(
                            carregando = false,
                            mensagemErro = textos.get(R.string.autenticacao_error_signup),
                        )
                    }
                    return@launch
                }
                runCatching { repositorioSincronizacao.enviarPerfil(uid) }
                _estado.update {
                    it.copy(
                        carregando = false,
                        mensagemSucesso = textos.get(R.string.autenticacao_sucesso_cadastro),
                    )
                }
            }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, mensagemErro = null, mensagemSucesso = null) }
            authRepository.signUpWithEmail(estadoAtual.email.trim(), estadoAtual.senha).fold(
                onSuccess = { authUser ->
                    runCatching {
                        sessaoLocal.prepararPara(authUser.uid)
                        repositorioSincronizacao.enviarPerfil(authUser.uid)
                    }
                    _estado.update {
                        it.copy(
                            carregando = false,
                            senha = "",
                            confirmarSenha = "",
                            mensagemSucesso = textos.get(R.string.autenticacao_sucesso_cadastro),
                        )
                    }
                },
                onFailure = {
                    _estado.update {
                        it.copy(
                            carregando = false,
                            mensagemErro = textos.get(R.string.autenticacao_error_signup),
                        )
                    }
                },
            )
        }
    }

    /**
     * Deixa o aparelho pronto para o uid e traz o que a conta tem na nuvem.
     *
     * A restauração chamava o Firestore fora de runCatching: sem rede, a exceção
     * escapava do launch e derrubava o app logo depois de um login bem-sucedido.
     */
    private suspend fun restaurarDaNuvem(uid: String): Result<Unit> = runCatching {
        sessaoLocal.prepararPara(uid)
        repositorioSincronizacao.restaurarPerfil(uid)
        repositorioSincronizacao.restaurarTudo(uid)
    }.map { }

    private fun executarVerificacaoAutomatica() {
        val estadoAtual = _estado.value
        if (!estadoAtual.oabValida) return

        viewModelScope.launch {
            _estado.update {
                it.copy(
                    verificacaoDjen = it.verificacaoDjen.copy(status = StatusVerificacao.CARREGANDO, detalhe = ""),
                    verificacaoOab = it.verificacaoOab.copy(status = StatusVerificacao.CARREGANDO, detalhe = ""),
                    verificacaoBusca = it.verificacaoBusca.copy(status = StatusVerificacao.CARREGANDO, detalhe = ""),
                    verificacaoDataJud = it.verificacaoDataJud.copy(status = StatusVerificacao.PENDENTE),
                    verificacaoIndexacao = it.verificacaoIndexacao.copy(status = StatusVerificacao.PENDENTE),
                )
            }

            val resultado = runCatching {
                withContext(Dispatchers.IO) {
                    val hoje = LocalDate.now()
                    monitorarCnjUseCase(
                        MonitorarDjenParams(
                            numeroOab = estadoAtual.numeroOab,
                            ufOab = estadoAtual.uf,
                            dataInicio = hoje.minusDays(estadoAtual.janelaBuscaDias.toLong()),
                            dataFim = hoje,
                            modo = MonitorarDjenModo.MANUAL,
                        ),
                    )
                }
            }

            resultado.fold(
                onSuccess = { resumo ->
                    val encontrou = resumo.djen.novas > 0 || resumo.djen.falhas.isEmpty()
                    _estado.update {
                        it.copy(
                            verificacaoDjen = it.verificacaoDjen.copy(
                                status = StatusVerificacao.SUCESSO,
                                detalhe = textos.get(R.string.autenticacao_verificacao_djen_ok),
                            ),
                            verificacaoOab = it.verificacaoOab.copy(
                                status = if (encontrou) StatusVerificacao.SUCESSO else StatusVerificacao.OPCIONAL,
                                detalhe = if (encontrou) {
                                    textos.get(R.string.autenticacao_verificacao_oab_ok)
                                } else {
                                    textos.get(R.string.autenticacao_verificacao_oab_nao_encontrada)
                                },
                            ),
                            verificacaoBusca = it.verificacaoBusca.copy(
                                status = StatusVerificacao.SUCESSO,
                                detalhe = textos.get(R.string.autenticacao_verificacao_busca_ok, resumo.djen.novas),
                            ),
                            verificacaoDataJud = it.verificacaoDataJud.copy(
                                status = StatusVerificacao.OPCIONAL,
                                detalhe = textos.get(R.string.autenticacao_verificacao_datajud_ok),
                            ),
                            verificacaoIndexacao = it.verificacaoIndexacao.copy(
                                status = StatusVerificacao.OPCIONAL,
                                detalhe = textos.get(R.string.autenticacao_verificacao_indexacao_ok),
                            ),
                            resumoBuscaPublicacoes = resumo.djen.novas,
                        )
                    }
                },
                onFailure = {
                    _estado.update {
                        it.copy(
                            verificacaoDjen = it.verificacaoDjen.copy(
                                status = StatusVerificacao.ERRO,
                                detalhe = textos.get(R.string.autenticacao_verificacao_djen_erro),
                            ),
                            verificacaoOab = it.verificacaoOab.copy(
                                status = StatusVerificacao.ERRO,
                                detalhe = textos.get(R.string.autenticacao_verificacao_oab_nao_encontrada),
                            ),
                            verificacaoBusca = it.verificacaoBusca.copy(
                                status = StatusVerificacao.OPCIONAL,
                                detalhe = textos.get(R.string.autenticacao_verificacao_busca_indisponivel),
                            ),
                            verificacaoDataJud = it.verificacaoDataJud.copy(
                                status = StatusVerificacao.PENDENTE,
                                detalhe = textos.get(R.string.autenticacao_verificacao_datajud),
                            ),
                            verificacaoIndexacao = it.verificacaoIndexacao.copy(
                                status = StatusVerificacao.PENDENTE,
                                detalhe = textos.get(R.string.autenticacao_verificacao_indexacao),
                            ),
                            mensagemErro = textos.get(R.string.autenticacao_verificacao_erro),
                        )
                    }
                },
            )
        }
    }

    private fun atualizar(bloco: (EstadoAutenticacao) -> EstadoAutenticacao) {
        _estado.update(bloco)
    }

    private fun etapaAnterior(etapa: EtapaCadastro): EtapaCadastro =
        when (etapa) {
            EtapaCadastro.CONTA -> EtapaCadastro.CONTA
            EtapaCadastro.OAB -> EtapaCadastro.CONTA
            EtapaCadastro.VERIFICACAO -> EtapaCadastro.OAB
            EtapaCadastro.PREFERENCIAS -> EtapaCadastro.VERIFICACAO
            EtapaCadastro.RESUMO -> EtapaCadastro.PREFERENCIAS
        }
}

private fun AuthUser?.naoAnonimo(): Boolean = this != null && !isAnonymous

internal interface TextosAutenticacao {
    fun get(@StringRes resId: Int): String
    fun get(@StringRes resId: Int, vararg args: Any): String
}

private class ContextTextosAutenticacao(
    private val context: Context,
) : TextosAutenticacao {
    override fun get(resId: Int): String = context.getString(resId)

    override fun get(resId: Int, vararg args: Any): String = context.getString(resId, *args)
}
