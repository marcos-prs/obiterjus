package com.obiterjus.domain.usecase

import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.domain.model.ConfiancaCalculo
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.model.toConfianca
import java.time.LocalDate

/**
 * Analisa o texto de uma [Publicacao] e extrai prazos usando expressões regulares.
 * Calcula a [PublicacaoPrazo.dataLimiteEstimada] com o [CalculadoraPrazos].
 *
 * Regras de extração (por ordem de prioridade):
 * 1. Prazo em horas  → "prazo de X hora(s)"
 * 2. Prazo em dias   → "prazo de X dia(s)" ou "prazo de X dias úteis"
 * 3. Prazo em meses  → "prazo de X mês/meses"
 *
 * A data-base usada para calcular a data-limite é [Publicacao.dataDisponibilizacao].
 * Se ela não estiver disponível, retorna a publicação sem data-limite.
 */
class ClassificarPublicacaoUC(
    private val calculadoraPrazos: CalculadoraPrazos,
) {

    suspend operator fun invoke(publicacao: Publicacao): Publicacao {
        val texto = publicacao.textoLimpo
            ?.takeIf { it.isNotBlank() }
            ?: return publicacao

        // Se já tem prazo extraído anteriormente, não sobrescreve
        if (publicacao.prazo != null && publicacao.prazo.dataLimiteEstimada != null) {
            return publicacao
        }

        val prazo = extrairPrazo(texto) ?: return publicacao

        val dataBase = publicacao.dataDisponibilizacao
        val resultado = dataBase?.let {
            calculadoraPrazos.calcularDataLimite(
                dataBase = it,
                quantidade = prazo.quantidade,
                unidade = prazo.unidade,
                diasUteis = prazo.diasUteis,
                tribunal = publicacao.tribunal,
            )
        }

        return publicacao.copy(
            prazo = prazo.copy(
                dataLimiteEstimada = resultado?.data,
                confiancaCalculo = resultado?.toConfianca() ?: ConfiancaCalculo.ESTIMADO,
            ),
        )
    }

    private fun extrairPrazo(texto: String): PublicacaoPrazo? {
        // Horas: "prazo de X hora(s)"
        REGEX_HORAS.find(texto)?.let { match ->
            val quantidade = match.groupValues[1].toIntOrNull() ?: return@let null
            return PublicacaoPrazo(
                quantidade = quantidade,
                unidade = "horas",
                diasUteis = true,
                textoOriginal = match.value,
            )
        }

        // Dias úteis: "prazo de X dias úteis"
        REGEX_DIAS_UTEIS.find(texto)?.let { match ->
            val quantidade = match.groupValues[1].toIntOrNull() ?: return@let null
            return PublicacaoPrazo(
                quantidade = quantidade,
                unidade = "dias",
                diasUteis = true,
                textoOriginal = match.value,
            )
        }

        // Dias corridos: "prazo de X dia(s)"
        REGEX_DIAS.find(texto)?.let { match ->
            val quantidade = match.groupValues[1].toIntOrNull() ?: return@let null
            return PublicacaoPrazo(
                quantidade = quantidade,
                unidade = "dias",
                diasUteis = false,
                textoOriginal = match.value,
            )
        }

        // Meses: "prazo de X mês/meses"
        REGEX_MESES.find(texto)?.let { match ->
            val quantidade = match.groupValues[1].toIntOrNull() ?: return@let null
            return PublicacaoPrazo(
                quantidade = quantidade,
                unidade = "meses",
                diasUteis = false,
                textoOriginal = match.value,
            )
        }

        return null
    }

    private companion object {
        // Prazo em horas
        val REGEX_HORAS = Regex(
            pattern = """prazo\s+de\s+(\d+)\s+hora[s]?""",
            option = RegexOption.IGNORE_CASE,
        )

        // Dias úteis (deve ser testado antes dos dias corridos)
        val REGEX_DIAS_UTEIS = Regex(
            pattern = """prazo\s+de\s+(\d+)\s+dias?\s+[úu]teis?""",
            option = RegexOption.IGNORE_CASE,
        )

        // Dias corridos
        val REGEX_DIAS = Regex(
            pattern = """prazo\s+de\s+(\d+)\s+dias?""",
            option = RegexOption.IGNORE_CASE,
        )

        // Meses
        val REGEX_MESES = Regex(
            pattern = """prazo\s+de\s+(\d+)\s+m[eê]s(?:es)?""",
            option = RegexOption.IGNORE_CASE,
        )
    }
}
