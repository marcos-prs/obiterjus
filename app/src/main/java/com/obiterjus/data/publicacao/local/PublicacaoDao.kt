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

    /**
     * Marca d'água enviada ao servidor de push: o maior `id` de comunicação do
     * DJEN já conhecido por este aparelho. O servidor compara com o que a
     * consulta dele devolve e só empurra quando houver `id` maior.
     *
     * É um inteiro, de propósito — o servidor não precisa guardar nada do
     * conteúdo para decidir se vale acordar o app.
     */
    @Query("SELECT MAX(id) FROM publicacoes")
    suspend fun maiorIdConhecido(): Long?

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

    /**
     * Publicações que ficaram sem data-limite — a API de cálculo estava fora do
     * ar no momento da ingestão, ou o tribunal ainda não estava cadastrado.
     *
     * Sem tribunal não há o que tentar: [com.obiterjus.core.time.CalculadoraPrazos]
     * devolve Pendente antes de tocar a rede. A janela por [dataMinima] impede que
     * publicações antigas sem prazo nenhum no texto sejam retentadas para sempre.
     */
    @Query(
        """
        SELECT * FROM publicacoes
        WHERE prazoDataLimite IS NULL
          AND ativo = 1
          AND textoLimpo IS NOT NULL AND trim(textoLimpo) != ''
          AND tribunal IS NOT NULL AND trim(tribunal) != ''
          AND dataDisponibilizacao IS NOT NULL
          AND dataDisponibilizacao >= :dataMinima
        ORDER BY dataDisponibilizacao DESC, id DESC
        LIMIT :limite
        """,
    )
    suspend fun getComPrazoPendente(dataMinima: LocalDate, limite: Int): List<PublicacaoEntity>

    @Query(
        """
        UPDATE publicacoes
        SET prazoQuantidade = :quantidade,
            prazoUnidade = :unidade,
            prazoDiasUteis = :diasUteis,
            prazoTexto = :texto,
            prazoDataLimite = :dataLimite,
            prazoConfianca = :confianca
        WHERE id = :id
        """,
    )
    suspend fun atualizarPrazo(
        id: Long,
        quantidade: Int,
        unidade: String,
        diasUteis: Boolean,
        texto: String,
        dataLimite: LocalDate,
        confianca: String,
    )
}
