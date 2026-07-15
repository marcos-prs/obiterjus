package com.obiterjus.domain.usecase

import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.domain.model.ConfiancaCalculo
import com.obiterjus.domain.model.NaturezaProcesso
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.model.toConfianca
import java.time.LocalDate

/**
 * Analisa o texto de uma [Publicacao] e extrai prazos usando expressões regulares.
 * Calcula a [PublicacaoPrazo.dataLimiteEstimada] com o [CalculadoraPrazos]
 * (API CalendárioForense — única fonte de cálculo).
 *
 * Regras de extração (por ordem de prioridade):
 * 1. Prazo em horas  → "prazo de X hora(s)"
 * 2. Prazo em dias   → "prazo de X dia(s)" ou "prazo de X dias úteis"
 * 3. Prazo em meses  → "prazo de X mês/meses"
 *
 * "Prazo de X dias" sem qualificador conta em dias úteis no cível (CPC 219)
 * e em dias corridos no penal (CPP 798) — a natureza vem do processo dono da
 * publicação, via [ResolverNaturezaProcessoUC].
 *
 * A data-base usada para calcular a data-limite é [Publicacao.dataDisponibilizacao].
 * Se ela não estiver disponível, retorna a publicação sem data-limite.
 */
class ClassificarPublicacaoUC(
    private val calculadoraPrazos: CalculadoraPrazos,
    private val resolverNatureza: ResolverNaturezaProcessoUC? = null,
) {

    suspend operator fun invoke(publicacao: Publicacao): Publicacao {
        val texto = publicacao.textoLimpo
            ?.takeIf { it.isNotBlank() }
            ?: return publicacao

        // Se já tem prazo extraído anteriormente, não sobrescreve
        if (publicacao.prazo != null && publicacao.prazo.dataLimiteEstimada != null) {
            return publicacao
        }

        val natureza = resolverNatureza?.invoke(publicacao.numeroProcesso)

        val prazo = extrairPrazo(texto, natureza) ?: return publicacao

        val dataBase = publicacao.dataDisponibilizacao
        val resultado = dataBase?.let {
            calculadoraPrazos.calcularDataLimite(
                dataBase = it,
                quantidade = prazo.quantidade,
                unidade = prazo.unidade,
                diasUteis = prazo.diasUteis,
                tribunal = publicacao.tribunal,
                natureza = natureza,
            )
        }

        return publicacao.copy(
            prazo = prazo.copy(
                dataLimiteEstimada = resultado?.data,
                confiancaCalculo = resultado?.toConfianca() ?: ConfiancaCalculo.PENDENTE,
            ),
        )
    }

    private fun extrairPrazo(texto: String, natureza: NaturezaProcesso?): PublicacaoPrazo? {
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

        // Dias úteis explícitos: "prazo de X dias úteis"
        REGEX_DIAS_UTEIS.find(texto)?.let { match ->
            val quantidade = match.groupValues[1].toIntOrNull() ?: return@let null
            return PublicacaoPrazo(
                quantidade = quantidade,
                unidade = "dias",
                diasUteis = true,
                textoOriginal = match.value,
            )
        }

        // "Prazo de X dias" sem qualificador: default pela natureza —
        // cível → úteis (CPC 219); penal → corridos (CPP 798).
        REGEX_DIAS.find(texto)?.let { match ->
            val quantidade = match.groupValues[1].toIntOrNull() ?: return@let null
            return PublicacaoPrazo(
                quantidade = quantidade,
                unidade = "dias",
                diasUteis = natureza != NaturezaProcesso.PENAL,
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

        // Dias úteis (deve ser testado antes dos dias sem qualificador)
        val REGEX_DIAS_UTEIS = Regex(
            pattern = """prazo\s+de\s+(\d+)\s+dias?\s+[úu]teis?""",
            option = RegexOption.IGNORE_CASE,
        )

        // Dias sem qualificador
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
