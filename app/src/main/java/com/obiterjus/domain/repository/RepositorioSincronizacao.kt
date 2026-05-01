package com.obiterjus.domain.repository

import com.obiterjus.domain.model.SincronizacaoNuvemResumo

interface RepositorioSincronizacao {
    suspend fun enviarTudo(userId: String): SincronizacaoNuvemResumo

    suspend fun restaurarTudo(userId: String): SincronizacaoNuvemResumo
}
