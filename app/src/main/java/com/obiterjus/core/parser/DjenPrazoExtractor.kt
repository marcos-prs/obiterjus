package com.obiterjus.core.parser

import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.domain.model.ConfiancaCalculo
import com.obiterjus.domain.model.NaturezaProcesso
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.model.toConfianca
import java.time.LocalDate

class DjenPrazoExtractor(
    private val calculadoraPrazos: CalculadoraPrazos
) {
    private val prazoRegex = Regex(
        pattern = """(?i)\b(?:prazo\s+(?:de|para)|em|no\s+prazo\s+de)\s+(\d{1,3})(?:\s*\([^)]+\))?\s+(dias|dia|horas|hora|meses|m[eê]s)\s*(úteis|uteis|corridos?)?""",
    )

    suspend fun extract(
        texto: String?,
        dataDisponibilizacao: LocalDate?,
        tribunal: String? = null,
        natureza: NaturezaProcesso? = null,
    ): PublicacaoPrazo? {
        if (texto.isNullOrBlank()) return null
        val match = prazoRegex.find(texto) ?: return null
        val quantidade = match.groupValues[1].toIntOrNull() ?: return null
        val unidade = match.groupValues[2].lowercase().normalizeUnidade()

        val qualificador = match.groupValues.getOrNull(3)
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
        val diasUteis = when {
            qualificador == "úteis" || qualificador == "uteis" -> true
            qualificador != null -> false // "corrido(s)" explícito
            // Sem qualificador, o default segue a natureza do processo:
            // cível → dias úteis (CPC art. 219); penal → corridos (CPP 798).
            else -> natureza != NaturezaProcesso.PENAL
        }

        val resultado = dataDisponibilizacao?.let { data ->
            calculadoraPrazos.calcularDataLimite(
                dataBase = data,
                quantidade = quantidade,
                unidade = unidade,
                diasUteis = diasUteis,
                tribunal = tribunal,
                natureza = natureza,
            )
        }
        return PublicacaoPrazo(
            quantidade = quantidade,
            unidade = unidade,
            diasUteis = diasUteis,
            textoOriginal = match.value.trim(),
            dataLimiteEstimada = resultado?.data,
            confiancaCalculo = resultado?.toConfianca() ?: ConfiancaCalculo.PENDENTE,
        )
    }

    private fun String.normalizeUnidade(): String =
        when {
            startsWith("hora") -> "horas"
            startsWith("mês") || startsWith("mes") -> "meses"
            else -> "dias"
        }
}
