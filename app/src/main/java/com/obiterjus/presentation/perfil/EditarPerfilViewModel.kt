package com.obiterjus.presentation.perfil

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.R
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.CadastroOabRepository
import com.obiterjus.domain.repository.SincronizacaoRepository
import com.obiterjus.presentation.autenticacao.TipoInscricaoCadastro
import com.obiterjus.ui.theme.TipoTema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EtapaEditarPerfil {
    CONTA,
    OAB,
    PREFERENCIAS,
    SEGURANCA,
    RESUMO,
}

data class EstadoEditarPerfil(
    val nomeCompleto: String = "",
    val email: String = "",
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

    // Segurança
    val senhaAtual: String = "",
    val novaSenha: String = "",
    val confirmarNovaSenha: String = "",

    val carregando: Boolean = false,
    val mensagemErro: String? = null,
    val mensagemSucesso: String? = null,
) {
    val novaSenhaValida: Boolean
        get() = novaSenha.length >= 8 &&
            novaSenha.any(Char::isUpperCase) &&
            novaSenha.any(Char::isDigit)

    val novaSenhaConfere: Boolean
        get() = novaSenha.isNotBlank() && novaSenha == confirmarNovaSenha
}

class EditarPerfilViewModel(
    private val authRepository: AuthRepository,
    private val repositorioCadastroOab: CadastroOabRepository,
    private val repositorioSincronizacao: SincronizacaoRepository,
    private val perfilPreferencesRepository: PerfilPreferencesRepository,
    context: Context,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoEditarPerfil())
    val estado: StateFlow<EstadoEditarPerfil> = _estado.asStateFlow()

    private val resources = context.resources

    init {
        reidratarEstado()
    }

    private fun reidratarEstado() {
        viewModelScope.launch {
            _estado.update { it.copy(carregando = true) }

            // Primeiro tenta restaurar do Firestore para ter os dados mais recentes
            authRepository.currentUser.first()?.let { user ->
                repositorioSincronizacao.restaurarPerfil(user.uid)
            }

            // Agora lê dos repositórios locais (que foram atualizados pelo restaurarPerfil ou já tinham dados)
            val cadastro = repositorioCadastroOab.cadastro.first()
            val perfilPrefs = perfilPreferencesRepository.preferencias.first()
            val user = authRepository.currentUser.first()

            _estado.update {
                it.copy(
                    nomeCompleto = cadastro.nomeAdvogado,
                    email = user?.email ?: "",
                    uf = cadastro.uf,
                    numeroOab = cadastro.numero,
                    tipoInscricao = try {
                        TipoInscricaoCadastro.valueOf(cadastro.tipoInscricao)
                    } catch (e: Exception) {
                        TipoInscricaoCadastro.ADVOGADO
                    },
                    nomeEscritorio = cadastro.nomeEscritorio,
                    areasAtuacao = cadastro.areasAtuacao.toSet(),
                    janelaBuscaDias = perfilPrefs.intervaloBuscaDias,
                    sincronizacaoAutomatica = perfilPrefs.sincronizacaoAutomatica,
                    notificarPublicacoes = perfilPrefs.notificarPublicacoes,
                    notificarPrazosUrgentes = perfilPrefs.notificarPrazosUrgentes,
                    notificarMovimentacoes = perfilPrefs.notificarMovimentacoes,
                    temaEscuro = perfilPrefs.tema == TipoTema.ESCURO,
                    carregando = false
                )
            }
        }
    }

    // Alterações Individuais (Salvamento Incremental)

    fun aoAlterarNome(valor: String) {
        _estado.update { it.copy(nomeCompleto = valor) }
        viewModelScope.launch {
            repositorioCadastroOab.salvarCadastro(
                numero = _estado.value.numeroOab,
                uf = _estado.value.uf,
                nomeAdvogado = valor
            )
            sincronizarComNuvem()
        }
    }

    fun aoAlterarUf(valor: String) {
        val uf = valor.filter(Char::isLetter).take(2).uppercase()
        _estado.update { it.copy(uf = uf) }
        viewModelScope.launch {
            repositorioCadastroOab.salvarCadastro(
                numero = _estado.value.numeroOab,
                uf = uf
            )
            sincronizarComNuvem()
        }
    }

    fun aoAlterarNumeroOab(valor: String) {
        val numero = valor.filter(Char::isDigit)
        _estado.update { it.copy(numeroOab = numero) }
        viewModelScope.launch {
            repositorioCadastroOab.salvarCadastro(
                numero = numero,
                uf = _estado.value.uf
            )
            sincronizarComNuvem()
        }
    }

    fun aoAlterarTipoInscricao(tipo: TipoInscricaoCadastro) {
        _estado.update { it.copy(tipoInscricao = tipo) }
        viewModelScope.launch {
            repositorioCadastroOab.salvarCadastro(
                numero = _estado.value.numeroOab,
                uf = _estado.value.uf,
                tipoInscricao = tipo.name
            )
            sincronizarComNuvem()
        }
    }

    fun aoAlterarNomeEscritorio(valor: String) {
        _estado.update { it.copy(nomeEscritorio = valor) }
        viewModelScope.launch {
            repositorioCadastroOab.salvarCadastro(
                numero = _estado.value.numeroOab,
                uf = _estado.value.uf,
                nomeEscritorio = valor
            )
            sincronizarComNuvem()
        }
    }

    fun aoAlternarAreaAtuacao(area: String) {
        _estado.update { current ->
            val novas = current.areasAtuacao.toMutableSet()
            if (!novas.add(area)) novas.remove(area)
            current.copy(areasAtuacao = novas)
        }
        viewModelScope.launch {
            repositorioCadastroOab.salvarCadastro(
                numero = _estado.value.numeroOab,
                uf = _estado.value.uf,
                areasAtuacao = _estado.value.areasAtuacao.toList()
            )
            sincronizarComNuvem()
        }
    }

    fun aoAlterarJanelaBusca(dias: Int) {
        _estado.update { it.copy(janelaBuscaDias = dias) }
        viewModelScope.launch {
            perfilPreferencesRepository.saveIntervaloBuscaDias(dias)
            sincronizarComNuvem()
        }
    }


    fun aoAlternarNotificarPublicacoes(ativo: Boolean) {
        _estado.update { it.copy(notificarPublicacoes = ativo) }
        viewModelScope.launch {
            perfilPreferencesRepository.saveNotificarPublicacoes(ativo)
            sincronizarComNuvem()
        }
    }

    fun aoAlternarNotificarPrazos(ativo: Boolean) {
        _estado.update { it.copy(notificarPrazosUrgentes = ativo) }
        viewModelScope.launch {
            perfilPreferencesRepository.saveNotificarPrazosUrgentes(ativo)
            sincronizarComNuvem()
        }
    }

    fun aoAlternarNotificarMovimentacoes(ativo: Boolean) {
        _estado.update { it.copy(notificarMovimentacoes = ativo) }
        viewModelScope.launch {
            perfilPreferencesRepository.saveNotificarMovimentacoes(ativo)
            sincronizarComNuvem()
        }
    }

    fun aoAlternarTemaEscuro(ativo: Boolean) {
        _estado.update { it.copy(temaEscuro = ativo) }
        viewModelScope.launch {
            perfilPreferencesRepository.saveTema(if (ativo) TipoTema.ESCURO else TipoTema.CLARO)
            sincronizarComNuvem()
        }
    }

    // Segurança (Troca de Senha)

    fun aoAlterarSenhaAtual(valor: String) = _estado.update { it.copy(senhaAtual = valor) }
    fun aoAlterarNovaSenha(valor: String) = _estado.update { it.copy(novaSenha = valor) }
    fun aoAlterarConfirmarNovaSenha(valor: String) = _estado.update { it.copy(confirmarNovaSenha = valor) }

    fun aoTrocarSenha() {
        val estadoAtual = _estado.value
        if (estadoAtual.carregando) return

        if (estadoAtual.senhaAtual.isBlank()) {
            _estado.update { it.copy(mensagemErro = resources.getString(R.string.autenticacao_error_required_fields)) }
            return
        }

        if (!estadoAtual.novaSenhaValida) {
            _estado.update { it.copy(mensagemErro = resources.getString(R.string.autenticacao_error_conta_invalida)) }
            return
        }

        if (!estadoAtual.novaSenhaConfere) {
            _estado.update { it.copy(mensagemErro = resources.getString(R.string.autenticacao_error_confirm_password)) }
            return
        }

        viewModelScope.launch {
            _estado.update { it.copy(carregando = true, mensagemErro = null, mensagemSucesso = null) }
            authRepository.updatePassword(estadoAtual.senhaAtual, estadoAtual.novaSenha).fold(
                onSuccess = {
                    _estado.update {
                        it.copy(
                            carregando = false,
                            senhaAtual = "",
                            novaSenha = "",
                            confirmarNovaSenha = "",
                            mensagemSucesso = resources.getString(R.string.perfil_sucesso_troca_senha)
                        )
                    }
                },
                onFailure = { erro ->
                    _estado.update {
                        it.copy(
                            carregando = false,
                            mensagemErro = resources.getString(R.string.perfil_erro_troca_senha_atual_incorreta)
                        )
                    }
                }
            )
        }
    }

    fun aoConsumirMensagens() {
        _estado.update { it.copy(mensagemErro = null, mensagemSucesso = null) }
    }

    private suspend fun sincronizarComNuvem() {
        authRepository.currentUser.first()?.let { user ->
            repositorioSincronizacao.enviarPerfil(user.uid)
        }
    }
}
