package com.obiterjus.presentation.editarcliente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.model.EnderecoCliente
import com.obiterjus.domain.model.RepresentanteLegal
import com.obiterjus.domain.model.TipoPessoa
import com.obiterjus.domain.repository.ClientesRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditarClienteViewModel(
    private val repositorio: ClientesRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoEditarCliente())
    val estado: StateFlow<EstadoEditarCliente> = _estado

    private var criadoEmOriginal: Instant? = null

    fun aoCarregar(clienteId: String) {
        // Id em branco marca cadastro avulso: geramos um id novo e não há nada a
        // buscar. O mesmo formato de UUID usado ao promover uma parte a cliente
        // (ver SincronizarClientesDoProcesso).
        if (clienteId.isBlank()) {
            criadoEmOriginal = null
            _estado.update { EstadoEditarCliente(clienteId = UUID.randomUUID().toString()) }
            return
        }
        _estado.update { EstadoEditarCliente(clienteId = clienteId) }
        viewModelScope.launch {
            val cliente = repositorio.buscarPorId(clienteId) ?: return@launch
            criadoEmOriginal = cliente.criadoEm
            _estado.update { cliente.paraEstado() }
        }
    }

    fun aoAlterarTipoPessoa(valor: TipoPessoa) = _estado.update { it.copy(tipoPessoa = valor) }
    fun aoAlterarNome(valor: String) = _estado.update { it.copy(nome = valor, erroNomeVazio = false) }
    fun aoAlterarDocumento(valor: String) = _estado.update { it.copy(documento = valor) }
    fun aoAlterarNacionalidade(valor: String) = _estado.update { it.copy(nacionalidade = valor) }
    fun aoAlterarEstadoCivil(valor: String) = _estado.update { it.copy(estadoCivil = valor) }
    fun aoAlterarProfissao(valor: String) = _estado.update { it.copy(profissao = valor) }
    fun aoAlterarCep(valor: String) = _estado.update { it.copy(cep = valor) }
    fun aoAlterarLogradouro(valor: String) = _estado.update { it.copy(logradouro = valor) }
    fun aoAlterarNumero(valor: String) = _estado.update { it.copy(numero = valor) }
    fun aoAlterarComplemento(valor: String) = _estado.update { it.copy(complemento = valor) }
    fun aoAlterarBairro(valor: String) = _estado.update { it.copy(bairro = valor) }
    fun aoAlterarMunicipio(valor: String) = _estado.update { it.copy(municipio = valor) }
    fun aoAlterarUf(valor: String) = _estado.update { it.copy(uf = valor) }
    fun aoAlterarTelefone(valor: String) = _estado.update { it.copy(telefone = valor) }
    fun aoAlterarEmail(valor: String) = _estado.update { it.copy(email = valor) }
    fun aoAlterarObservacoes(valor: String) = _estado.update { it.copy(observacoes = valor) }

    fun aoAlterarRepNome(valor: String) = _estado.update { it.copy(repNome = valor) }
    fun aoAlterarRepDocumento(valor: String) = _estado.update { it.copy(repDocumento = valor) }
    fun aoAlterarRepNacionalidade(valor: String) = _estado.update { it.copy(repNacionalidade = valor) }
    fun aoAlterarRepEstadoCivil(valor: String) = _estado.update { it.copy(repEstadoCivil = valor) }
    fun aoAlterarRepProfissao(valor: String) = _estado.update { it.copy(repProfissao = valor) }
    fun aoAlterarRepCargo(valor: String) = _estado.update { it.copy(repCargo = valor) }

    fun aoDescartarSalvo() = _estado.update { it.copy(salvo = false) }

    fun aoSalvar() {
        val atual = _estado.value
        if (atual.nome.isBlank()) {
            _estado.update { it.copy(erroNomeVazio = true) }
            return
        }
        _estado.update { it.copy(salvando = true) }
        viewModelScope.launch {
            val instante = Instant.now()
            repositorio.salvar(
                Cliente(
                    id = atual.clienteId,
                    tipoPessoa = atual.tipoPessoa,
                    nome = atual.nome.trim(),
                    documento = atual.documento.ouNulo(),
                    nacionalidade = atual.nacionalidade.ouNulo(),
                    estadoCivil = atual.estadoCivil.ouNulo(),
                    profissao = atual.profissao.ouNulo(),
                    endereco = EnderecoCliente(
                        cep = atual.cep.ouNulo(),
                        logradouro = atual.logradouro.ouNulo(),
                        numero = atual.numero.ouNulo(),
                        complemento = atual.complemento.ouNulo(),
                        bairro = atual.bairro.ouNulo(),
                        municipio = atual.municipio.ouNulo(),
                        uf = atual.uf.ouNulo(),
                    ),
                    telefone = atual.telefone.ouNulo(),
                    email = atual.email.ouNulo(),
                    observacoes = atual.observacoes.ouNulo(),
                    representante = atual.representanteOuNulo(),
                    criadoEm = criadoEmOriginal ?: instante,
                    atualizadoEm = instante,
                ),
            )
            _estado.update { it.copy(salvando = false, salvo = true) }
        }
    }
}

data class EstadoEditarCliente(
    val clienteId: String = "",
    val carregado: Boolean = false,
    val tipoPessoa: TipoPessoa = TipoPessoa.FISICA,
    val nome: String = "",
    val documento: String = "",
    val nacionalidade: String = "",
    val estadoCivil: String = "",
    val profissao: String = "",
    val cep: String = "",
    val logradouro: String = "",
    val numero: String = "",
    val complemento: String = "",
    val bairro: String = "",
    val municipio: String = "",
    val uf: String = "",
    val telefone: String = "",
    val email: String = "",
    val observacoes: String = "",
    val repNome: String = "",
    val repDocumento: String = "",
    val repNacionalidade: String = "",
    val repEstadoCivil: String = "",
    val repProfissao: String = "",
    val repCargo: String = "",
    val salvando: Boolean = false,
    val salvo: Boolean = false,
    val erroNomeVazio: Boolean = false,
) {
    /**
     * Representante só existe em pessoa jurídica, e só quando algum campo foi
     * preenchido — senão gravaria um bloco vazio que a ficha exibiria como
     * seção sem conteúdo.
     */
    fun representanteOuNulo(): RepresentanteLegal? {
        if (tipoPessoa != TipoPessoa.JURIDICA) return null
        val campos = listOf(repNome, repDocumento, repNacionalidade, repEstadoCivil, repProfissao, repCargo)
        if (campos.all { it.isBlank() }) return null
        return RepresentanteLegal(
            nome = repNome.ouNulo(),
            documento = repDocumento.ouNulo(),
            nacionalidade = repNacionalidade.ouNulo(),
            estadoCivil = repEstadoCivil.ouNulo(),
            profissao = repProfissao.ouNulo(),
            cargo = repCargo.ouNulo(),
        )
    }
}

private fun String.ouNulo(): String? = trim().takeIf { it.isNotEmpty() }

private fun Cliente.paraEstado(): EstadoEditarCliente = EstadoEditarCliente(
    clienteId = id,
    carregado = true,
    tipoPessoa = tipoPessoa,
    nome = nome,
    documento = documento.orEmpty(),
    nacionalidade = nacionalidade.orEmpty(),
    estadoCivil = estadoCivil.orEmpty(),
    profissao = profissao.orEmpty(),
    cep = endereco.cep.orEmpty(),
    logradouro = endereco.logradouro.orEmpty(),
    numero = endereco.numero.orEmpty(),
    complemento = endereco.complemento.orEmpty(),
    bairro = endereco.bairro.orEmpty(),
    municipio = endereco.municipio.orEmpty(),
    uf = endereco.uf.orEmpty(),
    telefone = telefone.orEmpty(),
    email = email.orEmpty(),
    observacoes = observacoes.orEmpty(),
    repNome = representante?.nome.orEmpty(),
    repDocumento = representante?.documento.orEmpty(),
    repNacionalidade = representante?.nacionalidade.orEmpty(),
    repEstadoCivil = representante?.estadoCivil.orEmpty(),
    repProfissao = representante?.profissao.orEmpty(),
    repCargo = representante?.cargo.orEmpty(),
)
