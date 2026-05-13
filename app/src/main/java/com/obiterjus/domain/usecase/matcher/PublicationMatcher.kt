package com.obiterjus.domain.usecase.matcher

import com.obiterjus.domain.model.ConfiancaMatch
import java.text.Normalizer

object PublicationMatcher {

    /**
     * Classifica a confiança de pertencimento da publicação ao usuário, sem nunca
     * descartar item. Camadas (da mais forte para a mais fraca):
     *   ALTA  → OAB do usuário aparece literalmente no corpo (regex tolerante).
     *   MEDIA → nome do usuário aparece com forma estrutural (primeiro + último,
     *           aceitando abreviação dos nomes do meio).
     *   BAIXA → só bate por fuzzy (Levenshtein dentro de tolerância) OU nenhum
     *           dos critérios casa no corpo. Significa: "veio da API mas o corpo
     *           não corrobora — possível homônimo, recomenda verificação".
     */
    fun classificar(
        textoPublicacao: String?,
        numeroOab: String,
        ufOab: String,
        nomeAdvogado: String,
    ): ConfiancaMatch {
        if (textoPublicacao.isNullOrBlank()) return ConfiancaMatch.BAIXA
        val textoLimpo = normalizeText(textoPublicacao)

        if (numeroOab.isNotBlank() && ufOab.isNotBlank() && casaOab(textoLimpo, numeroOab, ufOab)) {
            return ConfiancaMatch.ALTA
        }

        if (nomeAdvogado.isNotBlank()) {
            val tokens = tokenizeName(nomeAdvogado)
            if (tokens.size >= 2) {
                val primeiroNome = tokens.first()
                if (textoLimpo.contains(primeiroNome, ignoreCase = true)) {
                    if (buildNameRegex(tokens).containsMatchIn(textoLimpo)) {
                        return ConfiancaMatch.MEDIA
                    }
                    if (casaFuzzy(textoLimpo, tokens)) {
                        return ConfiancaMatch.BAIXA
                    }
                }
            }
        }

        return ConfiancaMatch.BAIXA
    }

    /** Mantido para compatibilidade com testes legados. */
    fun matches(
        textoPublicacao: String?,
        numeroOab: String,
        ufOab: String,
        nomeAdvogado: String,
    ): Boolean = classificar(textoPublicacao, numeroOab, ufOab, nomeAdvogado) != ConfiancaMatch.BAIXA

    private fun casaOab(textoLimpo: String, numeroOab: String, ufOab: String): Boolean {
        val digitsRegex = numeroOab.toList().joinToString("[.\\\\-]?")
        val regexOab = Regex(
            "(?i)(OAB[\\s\\W]*${ufOab}[\\s\\W]*${digitsRegex}|${digitsRegex}[\\s\\W]*${ufOab})",
        )
        return regexOab.containsMatchIn(textoLimpo)
    }

    private fun casaFuzzy(textoLimpo: String, tokens: List<String>): Boolean {
        val targetName = tokens.joinToString(" ")
        val words = textoLimpo.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val primeiroNome = tokens.first()
        for (i in 0..words.size - tokens.size) {
            if (words[i].lowercase() == primeiroNome) {
                val window = words.subList(i, minOf(i + tokens.size, words.size))
                    .joinToString(" ").lowercase()
                val dist = levenshtein(window, targetName)
                val maxErros = minOf(3, targetName.length / 5)
                if (dist <= maxErros) return true
            }
        }
        return false
    }

    private fun normalizeText(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")

    private fun tokenizeName(name: String): List<String> {
        val stopwords = setOf("de", "da", "do", "das", "dos", "e")
        return normalizeText(name)
            .split("\\s+".toRegex())
            .map { it.lowercase() }
            .filter { it.isNotBlank() && it !in stopwords }
    }

    private fun buildNameRegex(tokens: List<String>): Regex {
        if (tokens.size < 2) return Regex("(?i)\\b${tokens.first()}\\b")
        val first = tokens.first()
        val last = tokens.last()
        val middleRegex = if (tokens.size > 2) {
            tokens.subList(1, tokens.size - 1).joinToString("") { middle ->
                val initial = middle.first()
                "(?:$initial\\w*\\.?\\s+)?"
            }
        } else {
            ""
        }
        return Regex("(?i)\\b$first\\s+$middleRegex$last\\b")
    }

    fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length

        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }
}
