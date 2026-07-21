package com.obiterjus.domain.usecase

import com.obiterjus.data.agenda.local.PrazoSugeridoDao
import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.domain.model.PublicacaoPrazo

/**
 * Marca (ou desmarca) um prazo como cumprido pelo usuário. Assim como na
 * confirmação, a linha em prazos_sugeridos é criada aqui na primeira vez, caso o
 * prazo ainda viva apenas nos campos prazo* de PublicacaoEntity. Cumprir é
 * independente de ter criado evento no calendário: um prazo pode ser cumprido
 * tendo ou não sido confirmado.
 */
class MarcarPrazoCumpridoUC(
    private val prazoSugeridoDao: PrazoSugeridoDao,
) {
    suspend fun invoke(
        publicacaoId: Long,
        prazo: PublicacaoPrazo,
        cumprido: Boolean = true,
    ): Result<Unit> {
        return try {
            val entity = prazoSugeridoDao.getByPublicacaoId(publicacaoId)
                ?: PrazoSugeridoEntity(
                    publicacaoId = publicacaoId,
                    quantidade = prazo.quantidade,
                    unidade = prazo.unidade,
                    diasUteis = prazo.diasUteis,
                    textoOriginal = prazo.textoOriginal,
                    dataLimite = prazo.dataLimiteEstimada,
                    isConfirmado = prazo.isConfirmado,
                    idExternoCalendario = prazo.idExternoCalendario,
                    provedorCalendario = prazo.provedorCalendario,
                ).let { novo -> novo.copy(id = prazoSugeridoDao.insert(novo)) }

            prazoSugeridoDao.update(entity.copy(isCumprido = cumprido))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
