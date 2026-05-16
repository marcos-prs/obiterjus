package com.obiterjus.presentation.autenticacao

import com.obiterjus.ui.theme.TipoTema
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.R
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.domain.model.AuthUser
import com.obiterjus.domain.model.OabCadastro
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.CadastroOabRepository
import com.obiterjus.domain.repository.SincronizacaoRepository
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
            senhaValida &&
            senhaConfere &&
            aceitaPoliticas

    val oabValida: Boolean
        get() = uf.length == 2 && numeroOab.isNotBlank()
}

class AutenticacaoViewModel internal constructor(
    private val authRepository: AuthRepository,
    private val repositorioCadastroOab: CadastroOabRepository,
    private val repositorioSincronizacao: SincronizacaoRepository,
    private val monitorarCnjUseCase: MonitorarCnjUseCase,
    private val perfilPreferencesRepository: PerfilPreferencesRepository,
    private val textos: TextosAutenticacao,
) : ViewModel() {
    constructor(
        context: Context,
        authRepository: AuthRepository,
        repositorioCadastroOab: CadastroOabRepository,
        repositorioSincronizacao: SincronizacaoRepository,
        monitorarCnjUseCase: MonitorarCnjUseCase,
        perfilPreferencesRepository: PerfilPreferencesRepository,
    ) : this(
        authRepository = authRepository,
        repositorioCadastroOab = repositorioCadastroOab,
        repositorioSincronizacao = repositorioSincronizacao,
        monitorarCnjUseCase = monitorarCnjUseCase,
        perfilPreferencesRepository = perfilPreferencesRepository,
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
                onSuccess = { authUser ->
                    repositorioSincronizacao.restaurarPerfil(authUser.uid)
                    _estado.update {
                        it.copy(
                            carregando = false,
                            mensagemSucesso = textos.get(R.string.autenticacao_sucesso_login),
                        )
                    }
                },
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
                    perfilPreferencesRepository.saveSincronizacaoAutomatica(estadoAtual.sincronizacaoAutomatica)
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

        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, mensagemErro = null, mensagemSucesso = null) }
            authRepository.signUpWithEmail(estadoAtual.email.trim(), estadoAtual.senha).fold(
                onSuccess = { authUser ->
                    repositorioSincronizacao.enviarPerfil(authUser.uid)
                    _estado.update {
                        it.copy(
                            carregando = false,
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
