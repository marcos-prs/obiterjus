package com.obiterjus.domain.repository

import com.obiterjus.domain.model.EstruturaPeca
import com.obiterjus.domain.model.ExtracaoProgresso
import com.obiterjus.domain.model.MinutaPackage
import com.obiterjus.domain.model.MinutaSugestao
import com.obiterjus.domain.model.TipoMinuta
import java.io.InputStream
import kotlinx.coroutines.flow.Flow

/**
 * Orquestra o fluxo da API Obiter-Minuta e a persistência local das sugestões
 * geradas. A extração (`MinutaPackage`) é transitória — o `/result` expira na
 * primeira leitura; apenas a sugestão final é gravada.
 */
interface MinutaRepository {

    /**
     * Envia o PDF da peça e acompanha a extração até o fim, emitindo o
     * progresso do polling. O último valor emitido é sempre um progresso
     * concluído (ou o fluxo lança em caso de falha/timeout).
     */
    fun extrairPeca(
        pdf: InputStream,
        nomeArquivo: String,
        tipoDeclarado: String,
        numeroProcesso: String? = null,
        vara: String? = null,
    ): Flow<ExtracaoProgresso>

    /** Lê o resultado da extração de um job concluído (consome o `/result`). */
    suspend fun obterResultado(jobId: String): MinutaPackage

    /**
     * Gera o rascunho a partir do texto integral da peça de origem
     * ([textoOrigem]); a [estrutura] revisada acompanha como checklist. Persiste
     * a sugestão vinculada ao processo (quando informado).
     */
    suspend fun sugerirMinuta(
        tipoMinuta: TipoMinuta,
        tipoPecaOrigem: String,
        textoOrigem: String,
        estrutura: EstruturaPeca,
        numeroProcesso: String? = null,
        vara: String? = null,
        orientacoes: String? = null,
    ): MinutaSugestao

    /** Sugestões já geradas para um processo, mais recentes primeiro. */
    fun observarMinutasDoProcesso(numeroProcesso: String): Flow<List<MinutaSugestao>>

    suspend fun obterMinuta(sugestaoId: String): MinutaSugestao?

    suspend fun excluirMinuta(sugestaoId: String)
}
