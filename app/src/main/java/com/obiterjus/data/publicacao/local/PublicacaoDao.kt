package com.obiterjus.data.publicacao.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface PublicacaoDao {
    @Upsert
    suspend fun upsert(publicacao: PublicacaoEntity)

    @Upsert
    suspend fun upsertAll(publicacoes: List<PublicacaoEntity>)

    @Query("SELECT id FROM publicacoes WHERE id IN (:ids)")
    suspend fun getExistingIds(ids: List<Long>): List<Long>

    @Query("SELECT * FROM publicacoes WHERE hash IN (:hashes)")
    suspend fun getByHashes(hashes: List<String>): List<PublicacaoEntity>

    @Query("SELECT * FROM publicacoes WHERE id = :id")
    suspend fun getById(id: Long): PublicacaoEntity?

    @Query("SELECT * FROM publicacoes WHERE id = :id")
    fun observeById(id: Long): Flow<PublicacaoEntity?>

    @Query("SELECT * FROM publicacoes WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<PublicacaoEntity>

    @Query(
        """
        SELECT * FROM publicacoes
        WHERE (:numeroProcesso IS NULL OR numeroProcesso = :numeroProcesso)
          AND (:tribunal IS NULL OR tribunal = :tribunal)
          AND (:tipoComunicacao IS NULL OR tipoComunicacao = :tipoComunicacao)
          AND (:dataInicio IS NULL OR dataDisponibilizacao >= :dataInicio)
          AND (:dataFim IS NULL OR dataDisponibilizacao <= :dataFim)
          AND (:somenteSigilosas IS NULL OR isSigiloso = :somenteSigilosas)
        ORDER BY dataDisponibilizacao DESC, id DESC
        """,
    )
    fun observePublicacoes(
        numeroProcesso: String?,
        tribunal: String?,
        tipoComunicacao: String?,
        dataInicio: LocalDate?,
        dataFim: LocalDate?,
        somenteSigilosas: Boolean?,
    ): Flow<List<PublicacaoEntity>>

    @Query(
        """
        SELECT * FROM publicacoes
        WHERE numeroProcesso = :numeroProcesso
        ORDER BY dataDisponibilizacao DESC, id DESC
        """,
    )
    fun observePorProcesso(numeroProcesso: String): Flow<List<PublicacaoEntity>>

    @Query(
        """
        SELECT DISTINCT numeroProcesso FROM publicacoes
        WHERE numeroProcesso IS NOT NULL AND numeroProcesso != ''
        ORDER BY numeroProcesso ASC
        """,
    )
    suspend fun getNumerosProcessoDistintos(): List<String>

    @Query(
        """
        SELECT * FROM publicacoes
        WHERE dataDisponibilizacao = :data
        """,
    )
    suspend fun getByDataDisponibilizacao(data: LocalDate): List<PublicacaoEntity>

    @Query(
        """
        UPDATE publicacoes
        SET duplicataDe = :duplicataDe, totalDuplicatas = :totalDuplicatas
        WHERE id = :id
        """,
    )
    suspend fun atualizarStatusDuplicata(id: Long, duplicataDe: Long?, totalDuplicatas: Int)
}
