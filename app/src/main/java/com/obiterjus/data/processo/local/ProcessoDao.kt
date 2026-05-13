package com.obiterjus.data.processo.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessoDao {
    @Upsert
    suspend fun upsert(processo: ProcessoEntity)

    @Upsert
    suspend fun upsertAll(processos: List<ProcessoEntity>)

    @Query(
        """
        SELECT * FROM processos
        ORDER BY atualizadoEm DESC, numeroProcesso ASC
        """,
    )
    fun observeAll(): Flow<List<ProcessoEntity>>

    @Query("SELECT * FROM processos WHERE numeroProcesso = :numeroProcesso")
    fun observeByNumero(numeroProcesso: String): Flow<ProcessoEntity?>

    @Query("SELECT * FROM processos WHERE numeroProcesso = :numeroProcesso")
    suspend fun getByNumero(numeroProcesso: String): ProcessoEntity?

    @Query("SELECT * FROM processos WHERE numeroProcesso IN (:numerosProcesso)")
    suspend fun getByNumeros(numerosProcesso: List<String>): List<ProcessoEntity>

    @Query("DELETE FROM processos WHERE numeroProcesso = :numeroProcesso")
    suspend fun deleteByNumeroProcesso(numeroProcesso: String)
}
