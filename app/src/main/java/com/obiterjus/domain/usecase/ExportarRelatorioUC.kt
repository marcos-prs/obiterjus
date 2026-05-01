package com.obiterjus.domain.usecase

import com.obiterjus.core.time.FormatadorData
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.repository.RepositorioPublicacoes
import kotlinx.coroutines.flow.first

/**
 * Gera um relatório de texto formatado das publicações do advogado para
 * compartilhamento via Intent Android (text/plain).
 *
 * O conteúdo pode ser colado num e-mail, WhatsApp ou salvo como .txt.
 * Versões futuras podem adicionar saída em PDF ou CSV injetando
 * implementações diferentes via interface.
 */
class ExportarRelatorioUC(
    private val repositorioPublicacoes: RepositorioPublicacoes,
) {
    /**
     * Retorna o texto do relatório pronto para compartilhamento, ou null se não
     * houver publicações registradas.
     */
    suspend operator fun invoke(): String? {
        val publicacoes = repositorioPublicacoes.observarPublicacoes().first()
        if (publicacoes.isEmpty()) return null
        return gerarTexto(publicacoes)
    }

    private fun gerarTexto(publicacoes: List<Publicacao>): String = buildString {
        appendLine("=== RELATÓRIO ObiterJus ===")
        appendLine("Gerado em: ${FormatadorData.formatarDataHora(java.time.Instant.now())}")
        appendLine("Total de publicações: ${publicacoes.size}")
        appendLine()

        val comPrazo = publicacoes.filter { it.prazo != null }
        if (comPrazo.isNotEmpty()) {
            appendLine("--- PUBLICAÇÕES COM PRAZO (${comPrazo.size}) ---")
            comPrazo.sortedWith(
                compareBy(nullsLast()) { pub -> pub.prazo?.dataLimiteEstimada },
            ).forEach { pub -> appendLinhaPublicacao(pub) }
            appendLine()
        }

        val semPrazo = publicacoes.filter { it.prazo == null }
        if (semPrazo.isNotEmpty()) {
            appendLine("--- DEMAIS PUBLICAÇÕES (${semPrazo.size}) ---")
            semPrazo.forEach { pub -> appendLinhaPublicacao(pub) }
        }
    }

    private fun StringBuilder.appendLinhaPublicacao(pub: Publicacao) {
        appendLine()
        appendLine("Processo: ${pub.numeroProcesso ?: "-"}")
        appendLine("Tribunal: ${pub.tribunal ?: "-"}")
        appendLine("Disponibilizado: ${pub.dataDisponibilizacao?.let(FormatadorData::formatarData) ?: "-"}")

        pub.prazo?.let { prazo ->
            appendLine("Prazo: ${prazo.formatarResumo()}")
            prazo.dataLimiteEstimada?.let { data ->
                appendLine("Data-limite estimada: ${FormatadorData.formatarData(data)}")
            }
        }

        if (pub.isSigiloso) {
            appendLine("[SIGILOSO]")
        } else {
            val texto = pub.textoLimpo?.take(MAX_TEXTO_CHARS)?.trim()
            if (!texto.isNullOrBlank()) {
                appendLine("Texto:")
                appendLine(texto)
                if (pub.textoLimpo.length > MAX_TEXTO_CHARS) appendLine("...")
            }
        }
        appendLine("---")
    }

    private fun PublicacaoPrazo.formatarResumo(): String =
        buildString {
            append(quantidade)
            append(' ')
            append(unidade)
            if (diasUteis) append(" úteis")
        }

    private companion object {
        const val MAX_TEXTO_CHARS = 400
    }
}
