package com.obiterjus.domain.repository

import com.obiterjus.domain.model.SincronizacaoNuvemResumo

interface SincronizacaoRepository {
    suspend fun enviarTudo(userId: String): SincronizacaoNuvemResumo

    suspend fun restaurarTudo(userId: String): SincronizacaoNuvemResumo

    suspend fun enviarPerfil(userId: String): Result<Unit>

    suspend fun restaurarPerfil(userId: String): Result<Unit>
}
