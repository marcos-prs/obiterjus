package com.obiterjus.data.auditoria.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {
    @Insert
    suspend fun insert(log: SyncLogEntity)

    /** Retorna os últimos [limite] registros, do mais recente ao mais antigo. */
    @Query("SELECT * FROM sync_logs ORDER BY executadoEm DESC LIMIT :limite")
    fun observeRecentes(limite: Int = 100): Flow<List<SyncLogEntity>>

    @Query("DELETE FROM sync_logs WHERE id NOT IN (SELECT id FROM sync_logs ORDER BY executadoEm DESC LIMIT :manter)")
    suspend fun descartarAntigos(manter: Int = 200)
}
