package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.MinutaPackage
import com.obiterjus.domain.repository.MinutaRepository

/** Lê o resultado de um job de extração concluído (consome o `/result`). */
class ObterResultadoExtracaoUC(
    private val repositorioMinuta: MinutaRepository,
) {
    suspend operator fun invoke(jobId: String): MinutaPackage =
        repositorioMinuta.obterResultado(jobId)
}
