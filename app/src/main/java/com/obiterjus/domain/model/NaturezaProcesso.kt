package com.obiterjus.domain.model

import java.text.Normalizer

/**
 * Natureza do processo para fins de contagem de prazo.
 *
 * - [CIVEL]: prazos processuais em dias úteis (CPC art. 219), cadeia
 *   disponibilização → publicação → dies a quo.
 * - [PENAL]: mesma cadeia da cível, mas contagem em dias corridos
 *   (CPP art. 798), com prorrogação de vencimento não útil pelo §3º.
 *
 * A natureza é inferida da classe/assuntos CNJ do DataJud e pode ser
 * corrigida pelo usuário; a API CalendárioForense nunca a infere — o app
 * declara. Prazos de natureza material (decadência/prescrição) não são uma
 * natureza do processo: são declarados por prazo, quando identificados.
 */
enum class NaturezaProcesso {
    CIVEL,
    PENAL,
    ;

    companion object {
        fun fromNome(nome: String?): NaturezaProcesso? =
            nome?.trim()?.uppercase()?.let { valor ->
                entries.firstOrNull { it.name == valor }
            }
    }
}

/**
 * Inferência determinística por palavras-chave da classe e assuntos CNJ.
 * Retorna null quando não há indício penal — o cálculo trata null como
 * cível, mas o dado fica distinguível de uma natureza confirmada.
 */
object NaturezaProcessoInferencia {

    private val INDICIOS_PENAIS = listOf(
        "PENAL",
        "CRIMINAL",
        "CRIME",
        "INQUERITO POLICIAL",
        "AUTO DE PRISAO",
        "PRISAO PREVENTIVA",
        "PRISAO TEMPORARIA",
        "MEDIDA DE SEGURANCA",
        "LIBERDADE PROVISORIA",
        "HABEAS CORPUS",
        "TERMO CIRCUNSTANCIADO",
        "EXECUCAO DA PENA",
    )

    fun inferir(classeNome: String?, assuntos: List<String>): NaturezaProcesso? {
        val textos = (listOf(classeNome) + assuntos)
            .mapNotNull { it?.takeIf(String::isNotBlank) }
            .map(::normalizar)
        if (textos.isEmpty()) return null
        return if (textos.any { texto -> INDICIOS_PENAIS.any(texto::contains) }) {
            NaturezaProcesso.PENAL
        } else {
            null
        }
    }

    private fun normalizar(valor: String): String =
        Normalizer.normalize(valor, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}+"""), "")
            .uppercase()
}
