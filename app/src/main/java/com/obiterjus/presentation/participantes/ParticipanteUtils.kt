package com.obiterjus.presentation.participantes

import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.PublicacaoParticipante

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
    val clientes: List<String> = emptyList(),
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
fun List<ParticipanteProcesso>.resolverPartesProcesso(): PartesResolvidas =
    PartesResolvidas(
        ativa = filter { it.ehParteAtiva() }.toParteAgrupadaProcesso(),
        passiva = filter { it.ehPartePassiva() }.toParteAgrupadaProcesso(),
        advogados = filter { it.ehAdvogado() }
            .mapNotNull { it.nome.exibicaoOuNull() }
            .distinct(),
        clientes = filter { it.ehCliente && !it.ehAdvogado() }
            .mapNotNull { it.nome.exibicaoOuNull() }
            .distinct(),
    )

/**
 * Resolve as partes de uma publicação.
 */
fun List<PublicacaoParticipante>.resolverPartesPublicacao(): PartesResolvidas =
    PartesResolvidas(
        ativa = filter { it.ehParteAtiva() }.toParteAgrupadaPublicacao(),
        passiva = filter { it.ehPartePassiva() }.toParteAgrupadaPublicacao(),
        advogados = filter { it.ehAdvogado() }
            .mapNotNull { it.nome.exibicaoOuNull() }
            .distinct(),
    )

// --- Classificação (catálogo em [TiposParticipacao]) ---

/**
 * Grupo de um participante de processo. O advogado tem prioridade sobre o polo
 * cru — um advogado capturado com polo "ATIVO" continua sendo advogado, não
 * parte. Em seguida o papel (tipoParticipacao) vence o polo bruto.
 */
private fun ParticipanteProcesso.grupoResolvido(): GrupoPolo? {
    if (ehAdvogado()) return GrupoPolo.ADVOGADO
    return TiposParticipacao.grupoDeTipoOuPolo(tipoParticipacao)
        ?: TiposParticipacao.grupoDeTipoOuPolo(polo)
}

fun ParticipanteProcesso.ehParteAtiva(): Boolean = grupoResolvido() == GrupoPolo.ATIVO

fun ParticipanteProcesso.ehPartePassiva(): Boolean = grupoResolvido() == GrupoPolo.PASSIVO

fun ParticipanteProcesso.ehAdvogado(): Boolean = TiposParticipacao.ehAdvogadoTipo(tipoParticipacao)

private fun PublicacaoParticipante.ehParteAtiva(): Boolean =
    !ehAdvogado() && TiposParticipacao.grupoDeTipoOuPolo(tipo) == GrupoPolo.ATIVO

private fun PublicacaoParticipante.ehPartePassiva(): Boolean =
    !ehAdvogado() && TiposParticipacao.grupoDeTipoOuPolo(tipo) == GrupoPolo.PASSIVO

private fun PublicacaoParticipante.ehAdvogado(): Boolean =
    TiposParticipacao.ehAdvogadoTipo(tipo)

// --- Agrupamento ---

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

private fun String?.exibicaoOuNull(): String? =
    orEmpty().trim().takeIf { it.isNotBlank() }
