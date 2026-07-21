package com.obiterjus.presentation.editarprocesso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.model.NaturezaProcesso
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.repository.ClientesRepository
import com.obiterjus.domain.repository.ProcessosRepository
import com.obiterjus.domain.usecase.BuscarClientesSemelhantes
import com.obiterjus.domain.usecase.DecisaoCliente
import com.obiterjus.domain.usecase.ExcluirProcessoUseCase
import com.obiterjus.data.viacep.ViaCepApi
import com.obiterjus.domain.usecase.RessincronizarProcessoUseCase
import com.obiterjus.domain.usecase.SincronizarClientesDoProcesso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class EditarProcessoViewModel(
    private val repositorio: ProcessosRepository,
    private val excluirProcesso: ExcluirProcessoUseCase,
    private val ressincronizarProcesso: RessincronizarProcessoUseCase,
    private val viaCepApi: ViaCepApi,
    private val repositorioClientes: ClientesRepository,
    private val buscarClientesSemelhantes: BuscarClientesSemelhantes,
    private val sincronizarClientesDoProcesso: SincronizarClientesDoProcesso,
) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoEditarProcesso())
    val estado: StateFlow<EstadoEditarProcesso> = _estado

    fun aoCarregar(numeroProcesso: String) {
        _estado.update { it.copy(carregado = false, numeroProcesso = numeroProcesso) }
        viewModelScope.launch {
            val processo = repositorio.obterProcesso(numeroProcesso)
            // Partes já vinculadas entram com a decisão pronta, senão salvar de
            // novo criaria um cliente duplicado para quem já está na carteira.
            val decisoesExistentes = repositorioClientes.obterVinculosDoProcesso(numeroProcesso)
                .mapNotNull { vinculo ->
                    vinculo.participanteIdLocal?.let { it to DecisaoCliente.Vincular(vinculo.clienteId) }
                }
                .toMap()
            if (processo != null) {
                _estado.update {
                    it.copy(
                        carregado = true,
                        decisoesCliente = decisoesExistentes,
                        tribunal = processo.tribunal.orEmpty(),
                        grau = processo.grau.orEmpty(),
                        classeNome = processo.classeNome.orEmpty(),
                        orgaoJulgadorNome = processo.orgaoJulgadorNome.orEmpty(),
                        participantes = processo.participantes,
                        processoOriginal = processo,
                        dataDistribuicao = processo.dataDistribuicao,
                        comarcaSecao = processo.comarcaSecao.orEmpty(),
                        juizo = processo.juizo.orEmpty(),
                        prioridadeTramitacao = processo.prioridadeTramitacao.orEmpty(),
                        gratuidadeJustica = processo.gratuidadeJustica.orEmpty(),
                        valorCausa = processo.valorCausa?.toString().orEmpty(),
                        faseProcessual = processo.faseProcessual.orEmpty(),
                        situacaoAtual = processo.situacaoAtual.orEmpty(),
                        tutelaAntecipadaLiminar = processo.tutelaAntecipadaLiminar.orEmpty(),
                        advogadosAtivo = processo.advogadosAtivo.orEmpty(),
                        advogadosPassivo = processo.advogadosPassivo.orEmpty(),
                        defensoriaPublica = processo.defensoriaPublica.orEmpty(),
                        ministerioPublico = processo.ministerioPublico.orEmpty(),
                        terceirosAuxiliares = processo.terceirosAuxiliares.orEmpty(),
                        assuntoPrincipal = processo.assuntos.firstOrNull().orEmpty(),
                        segredoJustica = if ((processo.nivelSigilo ?: 0) > 0) "Sim" else "Não",
                        natureza = when (processo.natureza) {
                            NaturezaProcesso.CIVEL -> OPCAO_NATUREZA_CIVEL
                            NaturezaProcesso.PENAL -> OPCAO_NATUREZA_PENAL
                            null -> ""
                        },
                    )
                }
            }
        }
    }

    fun aoAlterarTribunal(valor: String) { _estado.update { it.copy(tribunal = valor) } }
    fun aoAlterarGrau(valor: String) { _estado.update { it.copy(grau = valor) } }
    fun aoAlterarClasse(valor: String) { _estado.update { it.copy(classeNome = valor) } }
    fun aoAlterarOrgao(valor: String) { _estado.update { it.copy(orgaoJulgadorNome = valor) } }
    fun aoAlterarDataDistribuicao(valor: Instant?) { _estado.update { it.copy(dataDistribuicao = valor) } }
    fun aoAlterarComarca(valor: String) { _estado.update { it.copy(comarcaSecao = valor) } }
    fun aoAlterarJuizo(valor: String) { _estado.update { it.copy(juizo = valor) } }
    fun aoAlterarPrioridade(valor: String) { _estado.update { it.copy(prioridadeTramitacao = valor) } }
    fun aoAlterarGratuidade(valor: String) { _estado.update { it.copy(gratuidadeJustica = valor) } }
    fun aoAlterarValorCausa(valor: String) { _estado.update { it.copy(valorCausa = valor) } }
    fun aoAlterarFase(valor: String) { _estado.update { it.copy(faseProcessual = valor) } }
    fun aoAlterarSituacao(valor: String) { _estado.update { it.copy(situacaoAtual = valor) } }
    fun aoAlterarTutela(valor: String) { _estado.update { it.copy(tutelaAntecipadaLiminar = valor) } }
    fun aoAlterarAdvogadosAtivo(valor: String) { _estado.update { it.copy(advogadosAtivo = valor) } }
    fun aoAlterarAdvogadosPassivo(valor: String) { _estado.update { it.copy(advogadosPassivo = valor) } }
    fun aoAlterarDefensoria(valor: String) { _estado.update { it.copy(defensoriaPublica = valor) } }
    fun aoAlterarMP(valor: String) { _estado.update { it.copy(ministerioPublico = valor) } }
    fun aoAlterarTerceiros(valor: String) { _estado.update { it.copy(terceirosAuxiliares = valor) } }
    fun aoAlterarAssunto(valor: String) { _estado.update { it.copy(assuntoPrincipal = valor) } }
    fun aoAlterarSegredo(valor: String) { _estado.update { it.copy(segredoJustica = valor) } }
    fun aoAlterarNatureza(valor: String) { _estado.update { it.copy(natureza = valor) } }

    /**
     * Marcar a caixa "é cliente" abre a escolha entre vincular a alguém que já
     * está na carteira ou criar um cadastro novo. Nada é gravado aqui: a
     * decisão fica em estado e só é aplicada no [aoSalvar], como todo o resto
     * desta tela.
     */
    fun aoMarcarComoCliente(participante: ParticipanteProcesso) {
        aoAlterarParticipante(participante.idLocal, participante.copy(ehCliente = true))
        viewModelScope.launch {
            val candidatos = buscarClientesSemelhantes(participante)
            _estado.update {
                it.copy(
                    sugestaoCliente = SugestaoCliente(
                        idLocalParticipante = participante.idLocal,
                        nomeParticipante = participante.nome.orEmpty(),
                        candidatos = candidatos,
                    ),
                )
            }
        }
    }

    fun aoDesmarcarComoCliente(participante: ParticipanteProcesso) {
        aoAlterarParticipante(participante.idLocal, participante.copy(ehCliente = false))
        _estado.update { it.copy(decisoesCliente = it.decisoesCliente - participante.idLocal) }
    }

    fun aoVincularClienteExistente(clienteId: String) {
        registrarDecisao(DecisaoCliente.Vincular(clienteId))
    }

    fun aoCriarClienteNovo() {
        registrarDecisao(DecisaoCliente.CriarNovo)
    }

    /**
     * Fechar sem escolher desfaz a marcação: manter a caixa marcada sem decisão
     * levaria o [SincronizarClientesDoProcesso] a criar um cliente que o
     * usuário não pediu.
     */
    fun aoDispensarSugestaoCliente() {
        val sugestao = _estado.value.sugestaoCliente ?: return
        _estado.update { atual ->
            val lista = atual.participantes.toMutableList()
            val index = lista.indexOfFirst { it.idLocal == sugestao.idLocalParticipante }
            if (index != -1) {
                lista[index] = lista[index].copy(ehCliente = false)
            }
            atual.copy(participantes = lista, sugestaoCliente = null)
        }
    }

    private fun registrarDecisao(decisao: DecisaoCliente) {
        val sugestao = _estado.value.sugestaoCliente ?: return
        _estado.update {
            it.copy(
                decisoesCliente = it.decisoesCliente + (sugestao.idLocalParticipante to decisao),
                sugestaoCliente = null,
            )
        }
    }

    fun aoAlterarParticipante(idLocal: String, participante: ParticipanteProcesso) {
        _estado.update { atual ->
            val lista = atual.participantes.toMutableList()
            val index = lista.indexOfFirst { it.idLocal == idLocal }
            if (index != -1) {
                lista[index] = participante
            }
            atual.copy(participantes = lista)
        }
    }

    fun aoAdicionarParticipante() {
        _estado.update { atual ->
            val novo = ParticipanteProcesso(
                idLocal = UUID.randomUUID().toString(),
                numeroProcesso = atual.numeroProcesso,
                polo = null,
                nome = "",
                tipoPessoa = null,
                tipoParticipacao = null,
            )
            atual.copy(participantes = atual.participantes + novo)
        }
    }

    fun aoRemoverParticipante(idLocal: String) {
        _estado.update { atual ->
            atual.copy(participantes = atual.participantes.filter { it.idLocal != idLocal })
        }
    }

    fun aoMoverParticipante(idLocal: String, paraCima: Boolean) {
        _estado.update { atual ->
            val lista = atual.participantes.toMutableList()
            val idx = lista.indexOfFirst { it.idLocal == idLocal }
            val destino = if (paraCima) idx - 1 else idx + 1
            if (idx == -1 || destino !in lista.indices) return@update atual
            val tmp = lista[idx]; lista[idx] = lista[destino]; lista[destino] = tmp
            atual.copy(participantes = lista)
        }
    }

    fun aoBuscarCep(idLocal: String, cep: String) {
        if (cep.length != 8) return
        _estado.update { it.copy(cepBuscandoIdLocal = idLocal, cepErroIdLocal = null) }
        viewModelScope.launch {
            try {
                val response = viaCepApi.buscarCep(cep)
                val dto = response.body()
                if (response.isSuccessful && dto != null && !dto.erro) {
                    _estado.update { atual ->
                        val lista = atual.participantes.toMutableList()
                        val idx = lista.indexOfFirst { it.idLocal == idLocal }
                        if (idx != -1) {
                            lista[idx] = lista[idx].copy(logradouro = dto.logradouro.orEmpty())
                        }
                        atual.copy(participantes = lista, cepBuscandoIdLocal = null, cepErroIdLocal = null)
                    }
                } else {
                    _estado.update { it.copy(cepBuscandoIdLocal = null, cepErroIdLocal = idLocal) }
                }
            } catch (_: Exception) {
                _estado.update { it.copy(cepBuscandoIdLocal = null, cepErroIdLocal = idLocal) }
            }
        }
    }

    fun aoDescartarErroValidacao() {
        _estado.update { it.copy(erroNomeParticipanteVazio = false) }
    }

    fun aoDescartarSalvo() { _estado.update { it.copy(salvo = false) } }
    fun aoDescartarErroExclusao() { _estado.update { it.copy(erroExclusao = false) } }
    fun aoDescartarRessincronizado() { _estado.update { it.copy(ressincronizado = false) } }
    fun aoDescartarErroRessincronizacao() { _estado.update { it.copy(erroRessincronizacao = false) } }

    fun aoSalvar() {
        val atual = _estado.value
        val original = atual.processoOriginal ?: return
        if (atual.participantes.any { it.nome.isNullOrBlank() }) {
            _estado.update { it.copy(erroNomeParticipanteVazio = true) }
            return
        }
        _estado.update { it.copy(salvando = true) }
        viewModelScope.launch {
            repositorio.salvarProcesso(
                original.copy(
                    tribunal = atual.tribunal.ifBlank { null },
                    grau = atual.grau.ifBlank { null },
                    classeNome = atual.classeNome.ifBlank { null },
                    orgaoJulgadorNome = atual.orgaoJulgadorNome.ifBlank { null },
                    participantes = atual.participantes,
                    dataDistribuicao = atual.dataDistribuicao,
                    comarcaSecao = atual.comarcaSecao.ifBlank { null },
                    juizo = atual.juizo.ifBlank { null },
                    prioridadeTramitacao = atual.prioridadeTramitacao.ifBlank { null },
                    gratuidadeJustica = atual.gratuidadeJustica.ifBlank { null },
                    valorCausa = atual.valorCausa.toDoubleOrNull(),
                    faseProcessual = atual.faseProcessual.ifBlank { null },
                    situacaoAtual = atual.situacaoAtual.ifBlank { null },
                    tutelaAntecipadaLiminar = atual.tutelaAntecipadaLiminar.ifBlank { null },
                    advogadosAtivo = atual.advogadosAtivo.ifBlank { null },
                    advogadosPassivo = atual.advogadosPassivo.ifBlank { null },
                    defensoriaPublica = atual.defensoriaPublica.ifBlank { null },
                    ministerioPublico = atual.ministerioPublico.ifBlank { null },
                    terceirosAuxiliares = atual.terceirosAuxiliares.ifBlank { null },
                    assuntos = listOf(atual.assuntoPrincipal).filter { it.isNotBlank() },
                    nivelSigilo = if (atual.segredoJustica == "Sim") 1 else 0,
                    natureza = when (atual.natureza) {
                        OPCAO_NATUREZA_CIVEL -> NaturezaProcesso.CIVEL
                        OPCAO_NATUREZA_PENAL -> NaturezaProcesso.PENAL
                        else -> original.natureza
                    },
                ),
            )
            // Depois do processo: o vínculo tem chave estrangeira para ele.
            sincronizarClientesDoProcesso(
                numeroProcesso = atual.numeroProcesso,
                participantes = atual.participantes,
                decisoes = atual.decisoesCliente,
            )
            _estado.update { it.copy(salvando = false, salvo = true) }
        }
    }

    fun aoExcluir() {
        val numero = _estado.value.numeroProcesso
        _estado.update { it.copy(excluindo = true) }
        viewModelScope.launch {
            try {
                excluirProcesso(numero)
                _estado.update { it.copy(excluindo = false, excluido = true) }
            } catch (_: Exception) {
                _estado.update { it.copy(excluindo = false, erroExclusao = true) }
            }
        }
    }

    fun aoRessincronizar() {
        val numero = _estado.value.numeroProcesso
        _estado.update { it.copy(ressincronizando = true) }
        viewModelScope.launch {
            try {
                ressincronizarProcesso(numero)
                _estado.update { it.copy(ressincronizando = false, ressincronizado = true) }
                aoCarregar(numero)
            } catch (_: Exception) {
                _estado.update { it.copy(ressincronizando = false, erroRessincronizacao = true) }
            }
        }
    }
}

data class EstadoEditarProcesso(
    val carregado: Boolean = false,
    val numeroProcesso: String = "",
    val tribunal: String = "",
    val grau: String = "",
    val classeNome: String = "",
    val orgaoJulgadorNome: String = "",
    val participantes: List<ParticipanteProcesso> = emptyList(),
    val processoOriginal: ProcessoMonitorado? = null,
    val salvando: Boolean = false,
    val salvo: Boolean = false,
    val excluindo: Boolean = false,
    val excluido: Boolean = false,
    val erroExclusao: Boolean = false,
    val ressincronizando: Boolean = false,
    val ressincronizado: Boolean = false,
    val erroRessincronizacao: Boolean = false,
    val erroNomeParticipanteVazio: Boolean = false,
    val cepBuscandoIdLocal: String? = null,
    val cepErroIdLocal: String? = null,
    /** Decisão de vínculo por participante, aplicada ao salvar. */
    val decisoesCliente: Map<String, DecisaoCliente> = emptyMap(),
    val sugestaoCliente: SugestaoCliente? = null,
    // Campos expandidos
    val dataDistribuicao: Instant? = null,
    val comarcaSecao: String = "",
    val juizo: String = "",
    val prioridadeTramitacao: String = "",
    val gratuidadeJustica: String = "",
    val valorCausa: String = "",
    val faseProcessual: String = "",
    val situacaoAtual: String = "",
    val tutelaAntecipadaLiminar: String = "",
    val advogadosAtivo: String = "",
    val advogadosPassivo: String = "",
    val defensoriaPublica: String = "",
    val ministerioPublico: String = "",
    val terceirosAuxiliares: String = "",
    val assuntoPrincipal: String = "",
    val segredoJustica: String = "Não",
    val natureza: String = "",
)

/** Escolha pendente ao marcar uma parte como cliente. */
data class SugestaoCliente(
    val idLocalParticipante: String,
    val nomeParticipante: String,
    val candidatos: List<Cliente>,
)

const val OPCAO_NATUREZA_CIVEL = "Cível"
const val OPCAO_NATUREZA_PENAL = "Penal"
