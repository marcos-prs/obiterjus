package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.repository.PublicacoesRepository
import kotlinx.coroutines.flow.Flow

class ObservarPublicacoes(
    private val repositorio: PublicacoesRepository,
) {
    operator fun invoke(): Flow<List<Publicacao>> =
        repositorio.observarPublicacoes()
}
