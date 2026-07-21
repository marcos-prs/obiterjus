package com.obiterjus.domain.usecase

import com.obiterjus.data.agenda.local.PrazoSugeridoDao
import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.model.ConfirmacaoPrazoResultado
import com.obiterjus.domain.model.ProvedorCalendario
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
        provedor: ProvedorCalendario,
    ): Result<ConfirmacaoPrazoResultado> {
        return try {
            // Prazos automáticos vivem nos campos prazo* de PublicacaoEntity;
            // a linha em prazos_sugeridos nasce aqui, na primeira confirmação.
            val entity = prazoSugeridoDao.getByPublicacaoId(publicacaoId)
                ?: PrazoSugeridoEntity(
                    publicacaoId = publicacaoId,
                    quantidade = prazo.quantidade,
                    unidade = prazo.unidade,
                    diasUteis = prazo.diasUteis,
                    textoOriginal = prazo.textoOriginal,
                    dataLimite = prazo.dataLimiteEstimada,
                ).let { novo -> novo.copy(id = prazoSugeridoDao.insert(novo)) }

            val entityConfirmada = entity.copy(
                isConfirmado = true,
                provedorCalendario = provedor.codigo,
            )
            prazoSugeridoDao.update(entityConfirmada)

            if (provedor == ProvedorCalendario.LOCAL) {
                return Result.success(ConfirmacaoPrazoResultado.ConfirmadoLocalmente)
            }

            val syncResult = calendarSyncRepository.syncPrazo(
                prazo = prazo,
                title = title,
                description = description,
                provedor = provedor,
            )
            return if (syncResult.isSuccess) {
                val externalId = syncResult.getOrNull()
                    ?: return Result.success(ConfirmacaoPrazoResultado.SincronizacaoPendente(provedor))
                prazoSugeridoDao.update(
                    entityConfirmada.copy(idExternoCalendario = externalId),
                )
                Result.success(
                    ConfirmacaoPrazoResultado.EventoCriado(
                        provedor = provedor,
                        idExterno = externalId,
                    ),
                )
            } else {
                Result.success(ConfirmacaoPrazoResultado.SincronizacaoPendente(provedor))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
