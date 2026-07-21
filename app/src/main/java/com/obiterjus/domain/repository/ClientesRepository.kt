package com.obiterjus.domain.repository

import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.model.VinculoClienteProcesso
import kotlinx.coroutines.flow.Flow

interface ClientesRepository {
    fun observarClientes(): Flow<List<Cliente>>

    fun observarCliente(id: String): Flow<Cliente?>

    suspend fun buscarPorId(id: String): Cliente?

    fun observarVinculos(): Flow<List<VinculoClienteProcesso>>

    suspend fun obterVinculosDoProcesso(numeroProcesso: String): List<VinculoClienteProcesso>

    fun observarProcessosDoCliente(clienteId: String): Flow<List<String>>

    fun observarClientesDoProcesso(numeroProcesso: String): Flow<List<Cliente>>

    suspend fun salvar(cliente: Cliente)

    suspend fun excluir(clienteId: String)

    suspend fun vincular(vinculo: VinculoClienteProcesso)

    suspend fun desvincular(clienteId: String, numeroProcesso: String)

    /** Casamento exato por CPF/CNPJ — sinal forte de que já é o mesmo cliente. */
    suspend fun buscarPorDocumento(documento: String): Cliente?

    /** Casamento por nome, para quando a parte veio sem documento. */
    suspend fun buscarPorNome(nome: String): List<Cliente>
}
