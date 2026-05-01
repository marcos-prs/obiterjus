package com.obiterjus.data.datajud.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MovimentoDao {
    @Upsert
    abstract suspend fun upsertAll(movimentos: List<MovimentoEntity>)

    @Query("DELETE FROM movimentos WHERE numeroProcesso = :numeroProcesso")
    abstract suspend fun deleteByProcesso(numeroProcesso: String)

    @Query(
        """
        SELECT * FROM movimentos
        WHERE numeroProcesso = :numeroProcesso
        ORDER BY dataHora DESC, idLocal DESC
        """,
    )
    abstract fun observeByProcesso(numeroProcesso: String): Flow<List<MovimentoEntity>>

    @Query(
        """
        SELECT * FROM movimentos
        WHERE numeroProcesso = :numeroProcesso
        ORDER BY dataHora DESC, idLocal DESC
        """,
    )
    abstract suspend fun getByProcesso(numeroProcesso: String): List<MovimentoEntity>

    @Query(
        """
        SELECT * FROM movimentos
        WHERE idLocal IN (:ids)
        """,
    )
    abstract suspend fun getByIds(ids: List<String>): List<MovimentoEntity>

    @Transaction
    open suspend fun replaceForProcesso(
        numeroProcesso: String,
        movimentos: List<MovimentoEntity>,
    ) {
        deleteByProcesso(numeroProcesso)
        if (movimentos.isNotEmpty()) {
            upsertAll(movimentos)
        }
    }
}
