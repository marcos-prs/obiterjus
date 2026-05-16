package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.repository.PublicacoesRepository
import kotlinx.coroutines.flow.Flow

class ObterPublicacaoPorId(
    private val repositorio: PublicacoesRepository,
) {
    operator fun invoke(id: Long): Flow<Publicacao?> =
        repositorio.observarPublicacao(id)
}
