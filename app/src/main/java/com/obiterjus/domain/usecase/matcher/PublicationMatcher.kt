package com.obiterjus.domain.usecase.matcher

import java.text.Normalizer
import kotlin.math.min

object PublicationMatcher {

    /**
     * Valida se uma publicação pertence ao usuário através de múltiplas camadas de tolerância:
     * 1. Match exato/sujo de OAB (Regex Anchor)
     * 2. Match Estrutural Nominal (Tokenização + Abreviação)
     * 3. Fuzzy Matching (Distância de Levenshtein - Opcional para nomes difíceis)
     */
    fun matches(
        textoPublicacao: String?,
        numeroOab: String,
        ufOab: String,
        nomeAdvogado: String
    ): Boolean {
        if (textoPublicacao.isNullOrBlank()) return false
        val textoLimpo = normalizeText(textoPublicacao)
        
        // --- REGRA 1: Match de OAB (Âncora principal) ---
        // Ex: "OAB MG 123456", "OAB/MG 123.456", "123456/MG"
        val digitsRegex = numeroOab.toList().joinToString("[.\\\\-]?")
        val regexOab = Regex("(?i)(OAB[\\s\\W]*${ufOab}[\\s\\W]*${digitsRegex}|${digitsRegex}[\\s\\W]*${ufOab})")
        
        if (regexOab.containsMatchIn(textoLimpo)) {
            return true
        }

        // --- REGRA 2: Match Estrutural de Nome ---
        if (nomeAdvogado.isNotBlank()) {
            val nomeTokens = tokenizeName(nomeAdvogado)
            if (nomeTokens.size >= 2) {
                val primeiroNome = nomeTokens.first()
                val ultimoNome = nomeTokens.last()
                
                // Filtro rápido: exige pelo menos o primeiro nome exato para não rodar Levenshtein na publicação inteira à toa
                if (textoLimpo.contains(primeiroNome, ignoreCase = true)) {
                    
                    val regexName = buildNameRegex(nomeTokens)
                    if (regexName.containsMatchIn(textoLimpo)) {
                        return true
                    }
                    
                    // --- REGRA 3: Fuzzy Matching (Levenshtein) ---
                    // Usado apenas se a pessoa errou digitação (ex: Souza vs Sousa)
                    val targetName = nomeTokens.joinToString(" ")
                    val words = textoLimpo.split("\\s+".toRegex()).filter { it.isNotBlank() }
                    
                    for (i in 0..words.size - nomeTokens.size) {
                        // Se a palavra atual é idêntica ao primeiro nome
                        if (words[i].lowercase() == primeiroNome) {
                            val window = words.subList(i, minOf(i + nomeTokens.size, words.size)).joinToString(" ").lowercase()
                            val dist = levenshtein(window, targetName)
                            
                            // Tolerância: 1 erro a cada 5 caracteres, máximo 3
                            val maxErros = minOf(3, targetName.length / 5)
                            if (dist <= maxErros) {
                                return true
                            }
                        }
                    }
                }
            }
        }

        return false
    }

    private fun normalizeText(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
    }

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
            val middles = tokens.subList(1, tokens.size - 1)
            middles.joinToString("") { middle ->
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
