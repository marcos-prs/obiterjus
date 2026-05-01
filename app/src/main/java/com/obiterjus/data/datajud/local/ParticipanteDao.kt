package com.obiterjus.data.datajud.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipanteDao {
    @Upsert
    suspend fun upsertAll(participantes: List<ParticipanteEntity>)

    @Query("SELECT * FROM participantes WHERE numeroProcesso = :numeroProcesso")
    fun observeByNumeroProcesso(numeroProcesso: String): Flow<List<ParticipanteEntity>>

    @Query("SELECT * FROM participantes WHERE numeroProcesso = :numeroProcesso")
    suspend fun getByNumeroProcesso(numeroProcesso: String): List<ParticipanteEntity>

    @Query("SELECT * FROM participantes")
    fun observeAll(): Flow<List<ParticipanteEntity>>

    @Query("SELECT * FROM participantes")
    suspend fun getAll(): List<ParticipanteEntity>
    
    @Query("DELETE FROM participantes WHERE numeroProcesso = :numeroProcesso")
    suspend fun deleteByNumeroProcesso(numeroProcesso: String)
}
