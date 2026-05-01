package com.obiterjus.domain.repository

import com.obiterjus.domain.model.PublicacaoPrazo

interface CalendarSyncRepository {
    suspend fun syncPrazo(
        prazo: PublicacaoPrazo,
        title: String,
        description: String,
        provedor: String // "GOOGLE" ou "OUTLOOK"
    ): Result<String> // Retorna o ID externo do evento

    suspend fun cancelPrazo(
        idExterno: String,
        provedor: String
    ): Result<Unit>
}
