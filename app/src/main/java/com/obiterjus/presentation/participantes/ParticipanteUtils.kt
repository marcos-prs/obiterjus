package com.obiterjus.presentation.participantes

import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.PublicacaoParticipante
import java.util.Locale

/**
 * Models for structured party representation.
 */
data class ParteAgrupada(
    val nomes: List<String>,
    val tipo: String? = null,
)

data class PartesResolvidas(
    val ativa: ParteAgrupada? = null,
    val passiva: ParteAgrupada? = null,
    val advogados: List<String> = emptyList(),
)

/**
 * Formata o confronto entre as partes (ex: "Autor x Réu").
 */
fun PartesResolvidas.formatarConfronto(): String? {
    val nomeAtiva = ativa?.nomes?.joinToString(", ")
    val nomePassiva = passiva?.nomes?.joinToString(", ")

    return when {
        nomeAtiva != null && nomePassiva != null -> "$nomeAtiva x $nomePassiva"
        nomeAtiva != null -> nomeAtiva
        nomePassiva != null -> nomePassiva
        else -> null
    }
}

/**
 * Resolve as partes de um processo monitorado (DataJud).
 */
fun List<ParticipanteProcesso>.resolverPartesProcesso(): PartesResolvidas {
    val ativa = extrairParteAtivaProcesso()
    val passiva = extrairPartePassivaProcesso()
    
    return PartesResolvidas(
        ativa = ativa,
        passiva = passiva
    )
}

/**
 * Resolve as partes de uma publicação.
 */
fun List<PublicacaoParticipante>.resolverPartesPublicacao(): PartesResolvidas {
    val ativa = extrairParteAtivaPublicacao()
    val passiva = extrairPartePassivaPublicacao()
    val advogados = extrairAdvogados()

    return PartesResolvidas(
        ativa = ativa,
        passiva = passiva,
        advogados = advogados
    )
}

// --- Lógica Interna (Migrada de DetalhePublicacaoViewModel.kt) ---

const val VALOR_POLO_ATIVO = "ATIVO"
const val VALOR_POLO_PASSIVO = "PASSIVO"


private fun List<ParticipanteProcesso>.extrairParteAtivaProcesso(): ParteAgrupada? {
    val selecionados = filter { it.ehParteAtiva() }
    return selecionados.toParteAgrupadaProcesso()
}

private fun List<ParticipanteProcesso>.extrairPartePassivaProcesso(): ParteAgrupada? {
    val selecionados = filter { it.ehPartePassiva() }
    return selecionados.toParteAgrupadaProcesso()
}

private fun List<PublicacaoParticipante>.extrairParteAtivaPublicacao(): ParteAgrupada? {
    val selecionados = filter { it.ehParteAtiva() }
    return selecionados.toParteAgrupadaPublicacao()
}

private fun List<PublicacaoParticipante>.extrairPartePassivaPublicacao(): ParteAgrupada? {
    val selecionados = filter { it.ehPartePassiva() }
    return selecionados.toParteAgrupadaPublicacao()
}

private fun List<PublicacaoParticipante>.extrairAdvogados(): List<String> =
    asSequence()
        .filter { it.ehAdvogado() }
        .mapNotNull { it.nome.exibicaoOuNull() }
        .distinct()
        .toList()

private fun List<ParticipanteProcesso>.toParteAgrupadaProcesso(): ParteAgrupada? {
    val nomes = mapNotNull { it.nome.exibicaoOuNull() }.distinct()
    if (nomes.isEmpty()) return null

    val tipos = mapNotNull { it.tipoParticipacao.exibicaoOuNull() }
        .ifEmpty { mapNotNull { it.polo.exibicaoOuNull() } }
        .distinct()

    return ParteAgrupada(
        nomes = nomes,
        tipo = tipos.firstOrNull(),
    )
}

private fun List<PublicacaoParticipante>.toParteAgrupadaPublicacao(): ParteAgrupada? {
    val nomes = mapNotNull { it.nome.exibicaoOuNull() }.distinct()
    if (nomes.isEmpty()) return null

    val tipos = mapNotNull { it.tipo.exibicaoOuNull() }.distinct()

    return ParteAgrupada(
        nomes = nomes,
        tipo = tipos.firstOrNull(),
    )
}

fun ParticipanteProcesso.ehParteAtiva(): Boolean {
    val poloNormalizado = polo.normalizado()
    val participacaoNormalizada = tipoParticipacao.normalizado()
    return poloNormalizado in tiposPartesAtivas || participacaoNormalizada in tiposPartesAtivas
}

fun ParticipanteProcesso.ehPartePassiva(): Boolean {
    val poloNormalizado = polo.normalizado()
    val participacaoNormalizada = tipoParticipacao.normalizado()
    return poloNormalizado in tiposPartesPassivas || participacaoNormalizada in tiposPartesPassivas
}

private fun PublicacaoParticipante.ehParteAtiva(): Boolean {
    val tipoNormalizado = tipo.normalizado()
    return tipoNormalizado in tiposPartesAtivas
}

private fun PublicacaoParticipante.ehPartePassiva(): Boolean {
    val tipoNormalizado = tipo.normalizado()
    return tipoNormalizado in tiposPartesPassivas
}

private fun PublicacaoParticipante.ehAdvogado(): Boolean {
    val tipoNormalizado = tipo.normalizado()
    return tipoNormalizado.startsWith("advogado") || tipoNormalizado.startsWith("procurador")
}

private fun String?.normalizado(): String =
    orEmpty().trim().lowercase(Locale.ROOT)

private fun String?.exibicaoOuNull(): String? =
    orEmpty().trim().takeIf { it.isNotBlank() }

private val tiposPartesAtivas = setOf(
    "autor",
    "requerente",
    "exequente",
    "impetrante",
    "apelante",
    "agravante",
    "ativo",
    "ativa",
)

private val tiposPartesPassivas = setOf(
    "reu",
    "réu",
    "requerido",
    "executado",
    "impetrado",
    "apelado",
    "agravado",
    "passivo",
    "passiva",
)
