package com.obiterjus.domain.usecase

import com.obiterjus.domain.repository.MinutaRepository

/** Remove uma sugestão persistida. */
class ExcluirMinutaUC(
    private val repositorioMinuta: MinutaRepository,
) {
    suspend operator fun invoke(sugestaoId: String) =
        repositorioMinuta.excluirMinuta(sugestaoId)
}
