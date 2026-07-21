package com.obiterjus.domain.repository

import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.model.VinculoClienteProcesso
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Carteira de clientes em memória. Reproduz as duas regras do banco de que a
 * lógica depende: busca por documento compara só os dígitos, e busca por nome
 * ignora caixa.
 */
class FakeClientesRepository : ClientesRepository {
    private val clientesFlow = MutableStateFlow<Map<String, Cliente>>(emptyMap())
    private val vinculosFlow = MutableStateFlow<List<VinculoClienteProcesso>>(emptyList())

    val clientes: Map<String, Cliente> get() = clientesFlow.value
    val vinculos: List<VinculoClienteProcesso> get() = vinculosFlow.value

    fun semear(vararg cliente: Cliente) {
        clientesFlow.value = clientesFlow.value + cliente.associateBy { it.id }
    }

    fun semearVinculo(vinculo: VinculoClienteProcesso) {
        vinculosFlow.value = vinculosFlow.value + vinculo
    }

    override suspend fun salvar(cliente: Cliente) {
        clientesFlow.value = clientesFlow.value + (cliente.id to cliente)
    }

    override suspend fun vincular(vinculo: VinculoClienteProcesso) {
        vinculosFlow.value = vinculosFlow.value.filterNot {
            it.clienteId == vinculo.clienteId && it.numeroProcesso == vinculo.numeroProcesso
        } + vinculo
    }

    override suspend fun desvincular(clienteId: String, numeroProcesso: String) {
        vinculosFlow.value = vinculosFlow.value.filterNot {
            it.clienteId == clienteId && it.numeroProcesso == numeroProcesso
        }
    }

    override suspend fun obterVinculosDoProcesso(numeroProcesso: String): List<VinculoClienteProcesso> =
        vinculosFlow.value.filter { it.numeroProcesso == numeroProcesso }

    override suspend fun buscarPorDocumento(documento: String): Cliente? {
        val digitos = documento.filter(Char::isDigit)
        return clientesFlow.value.values.firstOrNull {
            it.documento?.filter(Char::isDigit) == digitos
        }
    }

    override suspend fun buscarPorNome(nome: String): List<Cliente> =
        clientesFlow.value.values.filter { it.nome.equals(nome, ignoreCase = true) }

    override suspend fun buscarPorId(id: String): Cliente? = clientesFlow.value[id]

    override fun observarClientes(): Flow<List<Cliente>> =
        clientesFlow.map { it.values.toList() }

    override fun observarCliente(id: String): Flow<Cliente?> = clientesFlow.map { it[id] }

    override fun observarVinculos(): Flow<List<VinculoClienteProcesso>> = vinculosFlow

    override fun observarProcessosDoCliente(clienteId: String): Flow<List<String>> =
        vinculosFlow.map { lista ->
            lista.filter { it.clienteId == clienteId }.map { it.numeroProcesso }
        }

    override fun observarClientesDoProcesso(numeroProcesso: String): Flow<List<Cliente>> =
        vinculosFlow.map { lista ->
            lista.filter { it.numeroProcesso == numeroProcesso }
                .mapNotNull { clientesFlow.value[it.clienteId] }
        }

    override suspend fun excluir(clienteId: String) {
        clientesFlow.value = clientesFlow.value - clienteId
    }
}
