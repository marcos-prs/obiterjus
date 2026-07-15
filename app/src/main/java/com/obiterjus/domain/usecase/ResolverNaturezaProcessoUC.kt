package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.NaturezaProcesso
import com.obiterjus.domain.repository.ProcessosRepository

/**
 * Resolve a natureza (cível/penal) do processo dono de uma publicação, para
 * que a contagem de prazo use o default correto (CPC 219 → dias úteis;
 * CPP 798 → dias corridos). Null quando o processo é desconhecido ou a
 * natureza ainda não foi inferida/confirmada — tratado como cível.
 */
class ResolverNaturezaProcessoUC(
    private val processosRepository: ProcessosRepository,
) {
    suspend operator fun invoke(numeroProcesso: String?): NaturezaProcesso? {
        val numero = numeroProcesso?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { processosRepository.obterProcesso(numero)?.natureza }
            .getOrNull()
    }
}
