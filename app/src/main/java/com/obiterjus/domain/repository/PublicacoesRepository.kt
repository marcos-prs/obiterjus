package com.obiterjus.domain.repository

import com.obiterjus.domain.model.Publicacao
import kotlinx.coroutines.flow.Flow

interface PublicacoesRepository {
    fun observarPublicacoes(): Flow<List<Publicacao>>

    fun observarPublicacoesProcesso(numeroProcesso: String): Flow<List<Publicacao>>
    fun observarPublicacao(id: Long): Flow<Publicacao?>
}
