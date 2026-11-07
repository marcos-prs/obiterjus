package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.MinutaSugestao
import com.obiterjus.domain.repository.MinutaRepository
import kotlinx.coroutines.flow.Flow

/** Sugestões já geradas para um processo, mais recentes primeiro. */
class ObservarMinutasDoProcesso(
    private val repositorioMinuta: MinutaRepository,
) {
    operator fun invoke(numeroProcesso: String): Flow<List<MinutaSugestao>> =
        repositorioMinuta.observarMinutasDoProcesso(numeroProcesso)
}
