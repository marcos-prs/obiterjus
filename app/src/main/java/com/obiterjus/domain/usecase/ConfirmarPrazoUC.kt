package com.obiterjus.domain.usecase

import com.obiterjus.data.agenda.local.PrazoSugeridoDao
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.repository.CalendarSyncRepository

class ConfirmarPrazoUC(
    private val prazoSugeridoDao: PrazoSugeridoDao,
    private val calendarSyncRepository: CalendarSyncRepository
) {
    suspend fun invoke(
        publicacaoId: Long,
        prazo: PublicacaoPrazo,
        title: String,
        description: String,
        provedor: String
    ): Result<Unit> {
        return try {
            // 1. Marcar como confirmado no banco local
            val entity = prazoSugeridoDao.getByPublicacaoId(publicacaoId)
            if (entity != null) {
                prazoSugeridoDao.update(
                    entity.copy(
                        isConfirmado = true,
                        provedorCalendario = provedor
                    )
                )
            } else {
                return Result.failure(Exception("Prazo sugerido não encontrado para a publicação $publicacaoId"))
            }

            // 2. Tenta sincronizar imediatamente
            val syncResult = calendarSyncRepository.syncPrazo(prazo, title, description, provedor)
            if (syncResult.isSuccess) {
                // Se sucesso, guarda o ID externo
                val externalId = syncResult.getOrNull()
                prazoSugeridoDao.update(
                    entity.copy(
                        isConfirmado = true,
                        provedorCalendario = provedor,
                        idExternoCalendario = externalId
                    )
                )
            }
            // Se falhar a sincronização síncrona, a flag isConfirmado já está true, 
            // então o CalendarSyncWorker pegará isso depois.

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
