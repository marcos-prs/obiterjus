package com.obiterjus.domain.usecase

import com.obiterjus.data.agenda.local.PrazoSugeridoDao
import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.data.publicacao.local.PublicacaoDao
import com.obiterjus.domain.model.ConfiancaCalculo
import com.obiterjus.domain.model.ConfirmacaoPrazoResultado
import com.obiterjus.domain.model.ProvedorCalendario
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.repository.CalendarSyncRepository
import java.time.LocalDate

/**
 * Cadastra (ou substitui) o prazo de uma publicação a partir de dados
 * informados manualmente pelo usuário, com a data-limite já calculada pela
 * API CalendárioForense, e encadeia a confirmação/sincronização de agenda.
 *
 * Substitui tanto o prazo automático (campos prazo* de PublicacaoEntity)
 * quanto a linha de prazos_sugeridos (REPLACE pelo índice UNIQUE de
 * publicacaoId), cancelando o evento externo anterior quando existir.
 */
class CadastrarPrazoManualUC(
    private val publicacaoDao: PublicacaoDao,
    private val prazoSugeridoDao: PrazoSugeridoDao,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val confirmarPrazoUC: ConfirmarPrazoUC,
) {
    suspend fun invoke(
        publicacaoId: Long,
        quantidade: Int,
        diasUteis: Boolean,
        dataLimite: LocalDate,
        confianca: ConfiancaCalculo,
        title: String,
        description: String,
        provedor: ProvedorCalendario,
    ): Result<ConfirmacaoPrazoResultado> {
        return try {
            cancelarEventoAnterior(publicacaoId)

            publicacaoDao.atualizarPrazo(
                id = publicacaoId,
                quantidade = quantidade,
                unidade = UNIDADE_DIAS,
                diasUteis = diasUteis,
                texto = TEXTO_PRAZO_MANUAL,
                dataLimite = dataLimite,
                confianca = confianca.name,
            )

            prazoSugeridoDao.insert(
                PrazoSugeridoEntity(
                    publicacaoId = publicacaoId,
                    quantidade = quantidade,
                    unidade = UNIDADE_DIAS,
                    diasUteis = diasUteis,
                    textoOriginal = TEXTO_PRAZO_MANUAL,
                    dataLimite = dataLimite,
                ),
            )

            confirmarPrazoUC.invoke(
                publicacaoId = publicacaoId,
                prazo = PublicacaoPrazo(
                    quantidade = quantidade,
                    unidade = UNIDADE_DIAS,
                    diasUteis = diasUteis,
                    textoOriginal = TEXTO_PRAZO_MANUAL,
                    dataLimiteEstimada = dataLimite,
                    confiancaCalculo = confianca,
                ),
                title = title,
                description = description,
                provedor = provedor,
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Best-effort: uma falha aqui (rede, token expirado) não deve impedir o
     * cadastro do prazo correto — o pior caso é um evento órfão no calendário.
     */
    private suspend fun cancelarEventoAnterior(publicacaoId: Long) {
        val anterior = prazoSugeridoDao.getByPublicacaoId(publicacaoId) ?: return
        val idExterno = anterior.idExternoCalendario ?: return
        val provedor = ProvedorCalendario.fromCodigo(anterior.provedorCalendario) ?: return
        if (provedor == ProvedorCalendario.LOCAL) return
        runCatching { calendarSyncRepository.cancelPrazo(idExterno, provedor) }
    }

    companion object {
        const val TEXTO_PRAZO_MANUAL = "Prazo cadastrado manualmente"
        const val UNIDADE_DIAS = "dias"
    }
}
