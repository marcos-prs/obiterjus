package com.obiterjus.domain.repository

import com.obiterjus.domain.model.Publicacao
import kotlinx.coroutines.flow.Flow

interface RepositorioPublicacoes {
    fun observarPublicacoes(): Flow<List<Publicacao>>

    fun observarPublicacoesProcesso(numeroProcesso: String): Flow<List<Publicacao>>
}
