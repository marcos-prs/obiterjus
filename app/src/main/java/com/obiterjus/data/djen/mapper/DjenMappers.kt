package com.obiterjus.data.djen.mapper

import com.obiterjus.core.parser.CnjDateParser
import com.obiterjus.core.parser.DjenParticipantesExtractor
import com.obiterjus.domain.usecase.CalcularPrazoRegraUC
import com.obiterjus.core.parser.DjenTextCleaner
import com.obiterjus.core.parser.NumeroProcessoNormalizer
import com.obiterjus.core.parser.SigiloDetector
import com.obiterjus.data.djen.remote.dto.DjenComunicacaoDto
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.data.publicacao.local.toParticipantesJsonOrNull
import java.time.Instant

class DjenMapper(
    private val calcularPrazoRegraUC: CalcularPrazoRegraUC
) {
    companion object {
        private const val DJEN_SOURCE = "DJEN"
    }

    suspend fun toPublicacaoEntity(
        dto: DjenComunicacaoDto,
        capturedAt: Instant,
        updatedAt: Instant,
    ): PublicacaoEntity {
        val cleanedText = DjenTextCleaner.clean(dto.texto)
        val dataDisponibilizacao = CnjDateParser.parseLocalDate(dto.dataDisponibilizacaoIso)
        
        val prazo = calcularPrazoRegraUC.invoke(
            texto = cleanedText.clean,
            tipoComunicacao = dto.tipoComunicacao,
            dataDisponibilizacao = dataDisponibilizacao
        )

        val numeroProcessoNormalizado = NumeroProcessoNormalizer.normalize(dto.numeroProcesso)
            ?: NumeroProcessoNormalizer.extractAll(cleanedText.clean).firstOrNull()

        return PublicacaoEntity(
            id = dto.id,
            hash = dto.hash?.trim()?.takeIf { it.isNotEmpty() },
            numeroProcesso = numeroProcessoNormalizado,
            participantesJson = DjenParticipantesExtractor.extract(cleanedText.clean)
                .toParticipantesJsonOrNull(),
            prazoQuantidade = prazo?.quantidade,
            prazoUnidade = prazo?.unidade,
            prazoDiasUteis = prazo?.diasUteis ?: false,
            prazoTexto = prazo?.textoOriginal,
            prazoDataLimite = prazo?.dataLimiteEstimada,
            dataDisponibilizacao = dataDisponibilizacao,
            tribunal = dto.siglaTribunal?.trim()?.takeIf { it.isNotEmpty() },
            tipoComunicacao = dto.tipoComunicacao?.trim()?.takeIf { it.isNotEmpty() },
            nomeOrgao = dto.nomeOrgao?.trim()?.takeIf { it.isNotEmpty() },
            idOrgao = dto.idOrgao,
            textoRaw = cleanedText.raw,
            textoLimpo = cleanedText.clean,
            textoPossuiHtml = cleanedText.hasHtml,
            textoPossuiErroTemplate = cleanedText.hasTemplateError,
            isSigiloso = SigiloDetector.isSigiloso(cleanedText.clean),
            ativo = dto.ativo ?: true,
            fonte = DJEN_SOURCE,
            capturadoEm = capturedAt,
            atualizadoEm = updatedAt,
        )
    }
}
