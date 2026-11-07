package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.MinutaSugestao
import com.obiterjus.domain.repository.MinutaRepository

/** Carrega uma sugestão persistida pelo seu id. */
class ObterMinutaUC(
    private val repositorioMinuta: MinutaRepository,
) {
    suspend operator fun invoke(sugestaoId: String): MinutaSugestao? =
        repositorioMinuta.obterMinuta(sugestaoId)
}
