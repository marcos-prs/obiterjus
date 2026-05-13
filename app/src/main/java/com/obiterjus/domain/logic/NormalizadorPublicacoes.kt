package com.obiterjus.domain.logic

import com.obiterjus.domain.model.Publicacao
import java.time.LocalDate

/**
 * Lógica de negócio para normalização, agrupamento e deduplicação de publicações.
 */
object NormalizadorPublicacoes {

    /**
     * Agrupa publicações por data de disponibilização.
     */
    fun agruparPorData(publicacoes: List<Publicacao>): Map<LocalDate, List<Publicacao>> {
        return publicacoes
            .filter { it.dataDisponibilizacao != null }
            .groupBy { it.dataDisponibilizacao!! }
            .toSortedMap(compareByDescending { it })
    }

    /**
     * Remove apenas publicações idênticas de fato.
     */
    fun deduplicar(publicacoes: List<Publicacao>): List<Publicacao> {
        return publicacoes.distinctBy { it.chaveExata() }
    }

    /**
     * Calcula a ordem de cada publicação dentro do seu dia e o total de publicações daquele dia.
     * Retorna um mapa de ID da publicação -> Par(Ordem, Total no Dia).
     */
    fun calcularMetadadosOrdem(publicacoes: List<Publicacao>): Map<Long, Pair<Int, Int>> {
        val grupos = publicacoes.groupBy { it.dataDisponibilizacao }
        val resultado = mutableMapOf<Long, Pair<Int, Int>>()

        grupos.forEach { (_, pubsNoDia) ->
            val total = pubsNoDia.size
            pubsNoDia.sortedBy { it.id }.forEachIndexed { index, pub ->
                resultado[pub.id] = (index + 1) to total
            }
        }
        return resultado
    }

    private fun Publicacao.chaveExata(): String =
        listOf(
            hash?.trim().orEmpty(),
            numeroProcesso.orEmpty(),
            dataDisponibilizacao?.toString().orEmpty(),
            tribunal.orEmpty(),
            tipoComunicacao.orEmpty(),
            nomeOrgao.orEmpty(),
            textoLimpo.orEmpty(),
            participantes.joinToString("|") { participante ->
                listOf(
                    participante.tipo.trim(),
                    participante.nome.trim(),
                    participante.documento.orEmpty().trim(),
                ).joinToString("~")
            },
            isSigiloso.toString(),
        )
            .joinToString("||")
            .lowercase()
}
