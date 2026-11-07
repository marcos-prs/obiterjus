package com.obiterjus.data.minuta.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MinutaDao {

    @Upsert
    suspend fun upsert(minuta: MinutaEntity)

    @Query("SELECT * FROM minutas WHERE numeroProcesso = :numeroProcesso ORDER BY geradoEm DESC")
    fun observarPorProcesso(numeroProcesso: String): Flow<List<MinutaEntity>>

    @Query("SELECT * FROM minutas WHERE sugestaoId = :sugestaoId")
    suspend fun buscarPorId(sugestaoId: String): MinutaEntity?

    @Query("SELECT * FROM minutas")
    suspend fun listarTodas(): List<MinutaEntity>

    @Query("DELETE FROM minutas WHERE sugestaoId = :sugestaoId")
    suspend fun excluir(sugestaoId: String)
}
