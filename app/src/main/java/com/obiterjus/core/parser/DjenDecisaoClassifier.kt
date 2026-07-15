package com.obiterjus.core.parser

import java.text.Normalizer

/**
 * Classificação determinística de decisões cujo recurso cabível decorre de
 * lei — o texto nunca declara o prazo (ex.: "não conheço do agravo em
 * recurso especial" enseja agravo interno em 15 dias, CPC arts. 1.021 e
 * 1.070, sem que a decisão o diga).
 *
 * O app identifica o padrão e informa o ARTIGO; o número de dias é
 * resolvido pelo ruleset versionado da API CalendárioForense
 * (origem=ruleset). Padrões restritos a decisões monocráticas denegatórias
 * em tribunal, onde o cabimento do agravo interno é unívoco.
 *
 * O resultado é sempre uma SUGESTÃO (confiança rebaixada para INCERTO e
 * isConfirmado=false): inferência jurídica exige aceite do advogado.
 */
object DjenDecisaoClassifier {

    data class SugestaoRecursoCabivel(
        /** Chave na tabela prazos_cpc da API (ex.: "1070"). */
        val artigoCpc: String,
        /** Nome exibível do recurso sugerido. */
        val recurso: String,
    )

    // Dispositivos de decisão monocrática denegatória em tribunal.
    // Conservador: só a primeira pessoa do singular ("não conheço", "nego"),
    // que é a marca da decisão unipessoal — acórdãos usam voz colegiada
    // ("a Turma negou", "acordam os ministros") e não casam com os padrões,
    // mesmo quando a monocrática cita ementas de precedentes.
    private val PADROES_AGRAVO_INTERNO = listOf(
        Regex("""NAO\s+CONHECO\s+DO\s+AGRAVO"""),
        Regex("""NAO\s+CONHECO\s+DO\s+RECURSO\s+(ESPECIAL|EXTRAORDINARIO)"""),
        Regex("""NEGO\s+PROVIMENTO\s+AO\s+AGRAVO\s+EM\s+RECURSO"""),
        Regex("""NEGO\s+PROVIMENTO\s+AO\s+RECURSO\s+(ESPECIAL|EXTRAORDINARIO)"""),
        Regex("""NEGO\s+SEGUIMENTO\s+AO\s+(AGRAVO|RECURSO)"""),
    )

    private val AGRAVO_INTERNO = SugestaoRecursoCabivel(
        artigoCpc = "1070",
        recurso = "Agravo interno (CPC arts. 1.021 e 1.070)",
    )

    fun classificar(textoLimpo: String?): SugestaoRecursoCabivel? {
        val texto = textoLimpo?.takeIf { it.isNotBlank() } ?: return null
        val normalizado = normalizar(texto)

        return if (PADROES_AGRAVO_INTERNO.any { it.containsMatchIn(normalizado) }) {
            AGRAVO_INTERNO
        } else {
            null
        }
    }

    private fun normalizar(valor: String): String =
        Normalizer.normalize(valor, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}+"""), "")
            .uppercase()
            .replace(Regex("""\s+"""), " ")
}
