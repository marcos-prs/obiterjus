package com.obiterjus.domain.repository

import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.model.ProvedorCalendario

interface CalendarSyncRepository {
    suspend fun syncPrazo(
        prazo: PublicacaoPrazo,
        title: String,
        description: String,
        provedor: ProvedorCalendario,
    ): Result<String>

    suspend fun cancelPrazo(
        idExterno: String,
        provedor: ProvedorCalendario,
    ): Result<Unit>
}
