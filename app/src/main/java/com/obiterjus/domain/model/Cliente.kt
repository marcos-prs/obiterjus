package com.obiterjus.domain.model

import java.time.Instant

enum class TipoPessoa { FISICA, JURIDICA }

/**
 * Cliente que o advogado representa. Diferente do participante, existe fora de
 * qualquer processo: a qualificação é preenchida uma vez e vale em todos.
 */
data class Cliente(
    val id: String,
    val tipoPessoa: TipoPessoa,
    val nome: String,
    val documento: String? = null,
    val nacionalidade: String? = null,
    val estadoCivil: String? = null,
    val profissao: String? = null,
    val endereco: EnderecoCliente = EnderecoCliente(),
    val telefone: String? = null,
    val email: String? = null,
    val observacoes: String? = null,
    val representante: RepresentanteLegal? = null,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
)

data class EnderecoCliente(
    val cep: String? = null,
    val logradouro: String? = null,
    val numero: String? = null,
    val complemento: String? = null,
    val bairro: String? = null,
    val municipio: String? = null,
    val uf: String? = null,
)

data class RepresentanteLegal(
    val nome: String? = null,
    val documento: String? = null,
    val nacionalidade: String? = null,
    val estadoCivil: String? = null,
    val profissao: String? = null,
    val cargo: String? = null,
)

/** Vínculo de um cliente a um processo, com a parte que o originou. */
data class VinculoClienteProcesso(
    val clienteId: String,
    val numeroProcesso: String,
    val participanteIdLocal: String? = null,
)

/**
 * Cliente exibido no contexto de um processo. [clienteId] nulo significa que o
 * nome veio da parte marcada, sem cadastro correspondente — não há ficha para
 * abrir.
 */
data class ClienteDoProcesso(
    val nome: String,
    val clienteId: String? = null,
)

/** Cliente já acompanhado dos processos em que atua — o que a lista exibe. */
data class ClienteComProcessos(
    val cliente: Cliente,
    val numerosProcesso: List<String> = emptyList(),
)
