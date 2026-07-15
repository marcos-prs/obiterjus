package com.obiterjus.data.djen.mapper

import com.obiterjus.core.parser.CnjDateParser
import com.obiterjus.core.parser.DjenParticipantesExtractor
import com.obiterjus.core.parser.DjenTextCleaner
import com.obiterjus.core.parser.NumeroProcessoNormalizer
import com.obiterjus.core.parser.SigiloDetector
import com.obiterjus.data.djen.remote.dto.DjenComunicacaoDto
import com.obiterjus.data.processo.ProcessoDadosResolver
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.data.publicacao.local.toParticipantesJsonOrNull
import com.obiterjus.domain.model.PublicacaoParticipante
import java.time.Instant

class DjenMapper {
    companion object {
        private const val DJEN_SOURCE = "DJEN"
    }

    // O prazo NÃO é calculado aqui: nesta etapa o tribunal ainda não foi
    // normalizado e a chamada à API CalendárioForense o exige. O cálculo
    // acontece uma única vez, no PublicacaoPrazoMapper, sobre a entidade
    // completa.
    suspend fun toPublicacaoEntity(
        dto: DjenComunicacaoDto,
        capturedAt: Instant,
        updatedAt: Instant,
    ): PublicacaoEntity {
        val cleanedText = DjenTextCleaner.clean(dto.texto)
        val dataDisponibilizacao = CnjDateParser.parseLocalDate(dto.dataDisponibilizacaoIso)

        val numeroProcessoNormalizado = NumeroProcessoNormalizer.normalize(dto.numeroProcesso)
            ?: NumeroProcessoNormalizer.extractAll(cleanedText.clean).firstOrNull()

        return PublicacaoEntity(
            id = dto.id,
            hash = dto.hash?.trim()?.takeIf { it.isNotEmpty() },
            numeroProcesso = numeroProcessoNormalizado,
            participantesJson = extrairParticipantes(dto, cleanedText.clean)
                .toParticipantesJsonOrNull(),
            prazoQuantidade = null,
            prazoUnidade = null,
            prazoDiasUteis = false,
            prazoTexto = null,
            prazoDataLimite = null,
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

    // Prioriza os campos estruturados que a API já entrega (destinatários e
    // advogados com OAB) e usa a extração por regex do texto apenas para
    // complementar o que não veio estruturado.
    private fun extrairParticipantes(
        dto: DjenComunicacaoDto,
        textoLimpo: String?,
    ): List<PublicacaoParticipante> {
        val estruturados = buildList {
            dto.destinatarios.forEach { destinatario ->
                val nome = destinatario.nome?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
                val tipo = when (ProcessoDadosResolver.normalizarPolo(destinatario.polo)) {
                    "ATIVO" -> "Polo Ativo"
                    "PASSIVO" -> "Polo Passivo"
                    else -> "Parte"
                }
                add(PublicacaoParticipante(tipo = tipo, nome = nome))
            }
            dto.destinatarioAdvogados.forEach { destinatario ->
                val advogado = destinatario.advogado ?: return@forEach
                val nome = advogado.nome?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
                val documento = advogado.numeroOab?.trim()?.takeIf { it.isNotEmpty() }?.let { numero ->
                    val uf = advogado.ufOab?.trim()?.takeIf { it.isNotEmpty() }
                    if (uf != null) "OAB/$uf $numero" else "OAB $numero"
                }
                add(PublicacaoParticipante(tipo = "Advogado", nome = nome, documento = documento))
            }
        }
        val extraidosDoTexto = DjenParticipantesExtractor.extract(textoLimpo)

        return (estruturados + extraidosDoTexto).distinctBy { participante ->
            val categoria = if (participante.tipo.equals("Advogado", ignoreCase = true)) "ADVOGADO" else "PARTE"
            "$categoria|${ProcessoDadosResolver.normalizarNome(participante.nome)}"
        }
    }
}
