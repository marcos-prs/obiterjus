package com.obiterjus.data.cliente.local

import com.obiterjus.data.cliente.normalizarDocumento
import com.obiterjus.data.cliente.toDomain
import com.obiterjus.data.cliente.toEntity
import com.obiterjus.data.processo.ProcessoDadosResolver
import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.model.VinculoClienteProcesso
import com.obiterjus.domain.repository.ClientesRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalClienteRepository(
    private val clienteDao: ClienteDao,
) : ClientesRepository {

    override fun observarClientes(): Flow<List<Cliente>> =
        clienteDao.observarTodos().map { clientes -> clientes.map { it.toDomain() } }

    override fun observarCliente(id: String): Flow<Cliente?> =
        clienteDao.observarPorId(id).map { it?.toDomain() }

    override suspend fun buscarPorId(id: String): Cliente? =
        clienteDao.buscarPorId(id)?.toDomain()

    override fun observarVinculos(): Flow<List<VinculoClienteProcesso>> =
        clienteDao.observarVinculos().map { vinculos ->
            vinculos.map {
                VinculoClienteProcesso(
                    clienteId = it.clienteId,
                    numeroProcesso = it.numeroProcesso,
                    participanteIdLocal = it.participanteIdLocal,
                )
            }
        }

    override suspend fun obterVinculosDoProcesso(numeroProcesso: String): List<VinculoClienteProcesso> =
        clienteDao.obterVinculosDoProcesso(numeroProcesso).map {
            VinculoClienteProcesso(
                clienteId = it.clienteId,
                numeroProcesso = it.numeroProcesso,
                participanteIdLocal = it.participanteIdLocal,
            )
        }

    override fun observarProcessosDoCliente(clienteId: String): Flow<List<String>> =
        clienteDao.observarProcessosDoCliente(clienteId)

    override fun observarClientesDoProcesso(numeroProcesso: String): Flow<List<Cliente>> =
        clienteDao.observarClientesDoProcesso(numeroProcesso)
            .map { clientes -> clientes.map { it.toDomain() } }

    override suspend fun salvar(cliente: Cliente) {
        clienteDao.upsert(cliente.toEntity())
    }

    override suspend fun excluir(clienteId: String) {
        clienteDao.excluir(clienteId)
    }

    override suspend fun vincular(vinculo: VinculoClienteProcesso) {
        clienteDao.vincular(
            ClienteProcessoEntity(
                clienteId = vinculo.clienteId,
                numeroProcesso = vinculo.numeroProcesso,
                participanteIdLocal = vinculo.participanteIdLocal,
                vinculadoEm = Instant.now(),
            ),
        )
    }

    override suspend fun desvincular(clienteId: String, numeroProcesso: String) {
        clienteDao.desvincular(clienteId, numeroProcesso)
    }

    override suspend fun buscarPorDocumento(documento: String): Cliente? =
        normalizarDocumento(documento)
            ?.let { clienteDao.buscarPorDocumento(it) }
            ?.toDomain()

    override suspend fun buscarPorNome(nome: String): List<Cliente> =
        clienteDao.buscarPorNome(ProcessoDadosResolver.normalizarNome(nome))
            .map { it.toDomain() }
}
