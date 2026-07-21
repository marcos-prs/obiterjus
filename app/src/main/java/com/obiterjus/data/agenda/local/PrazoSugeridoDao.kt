package com.obiterjus.data.agenda.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PrazoSugeridoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prazoSugerido: PrazoSugeridoEntity): Long

    @Update
    suspend fun update(prazoSugerido: PrazoSugeridoEntity)

    @Query("SELECT * FROM prazos_sugeridos WHERE publicacaoId = :publicacaoId")
    suspend fun getByPublicacaoId(publicacaoId: Long): PrazoSugeridoEntity?

    @Query("SELECT * FROM prazos_sugeridos WHERE publicacaoId = :publicacaoId")
    fun observeByPublicacaoId(publicacaoId: Long): Flow<PrazoSugeridoEntity?>

    @Query(
        """
        SELECT * FROM prazos_sugeridos
        WHERE isConfirmado = 1
          AND idExternoCalendario IS NULL
          AND provedorCalendario IN (:provedores)
        """
    )
    suspend fun getPrazosParaSincronizar(provedores: List<String>): List<PrazoSugeridoEntity>

    @Query("SELECT * FROM prazos_sugeridos")
    fun observeAll(): Flow<List<PrazoSugeridoEntity>>
}
