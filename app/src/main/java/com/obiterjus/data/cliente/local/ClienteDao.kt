package com.obiterjus.data.cliente.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {

    @Upsert
    suspend fun upsert(cliente: ClienteEntity)

    @Query("SELECT * FROM clientes ORDER BY nomeNormalizado")
    fun observarTodos(): Flow<List<ClienteEntity>>

    @Query("SELECT * FROM clientes WHERE id = :id")
    fun observarPorId(id: String): Flow<ClienteEntity?>

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun buscarPorId(id: String): ClienteEntity?

    /** Casamento exato de CPF/CNPJ — o sinal forte de que já é o mesmo cliente. */
    @Query("SELECT * FROM clientes WHERE documentoNormalizado = :documentoNormalizado")
    suspend fun buscarPorDocumento(documentoNormalizado: String): ClienteEntity?

    /** Casamento por nome, usado quando a parte veio sem documento. */
    @Query("SELECT * FROM clientes WHERE nomeNormalizado = :nomeNormalizado")
    suspend fun buscarPorNome(nomeNormalizado: String): List<ClienteEntity>

    @Query("DELETE FROM clientes WHERE id = :id")
    suspend fun excluir(id: String)

    // --- Vínculos com processos ---

    @Upsert
    suspend fun vincular(vinculo: ClienteProcessoEntity)

    @Query("DELETE FROM clientes_processos WHERE clienteId = :clienteId AND numeroProcesso = :numeroProcesso")
    suspend fun desvincular(clienteId: String, numeroProcesso: String)

    @Query("SELECT * FROM clientes_processos")
    fun observarVinculos(): Flow<List<ClienteProcessoEntity>>

    @Query("SELECT * FROM clientes_processos WHERE numeroProcesso = :numeroProcesso")
    suspend fun obterVinculosDoProcesso(numeroProcesso: String): List<ClienteProcessoEntity>

    @Query("SELECT numeroProcesso FROM clientes_processos WHERE clienteId = :clienteId")
    fun observarProcessosDoCliente(clienteId: String): Flow<List<String>>

    @Query(
        """
        SELECT c.* FROM clientes c
        INNER JOIN clientes_processos cp ON cp.clienteId = c.id
        WHERE cp.numeroProcesso = :numeroProcesso
        ORDER BY c.nomeNormalizado
        """,
    )
    fun observarClientesDoProcesso(numeroProcesso: String): Flow<List<ClienteEntity>>
}
