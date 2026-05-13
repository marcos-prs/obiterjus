package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.repository.RepositorioPublicacoes
import kotlinx.coroutines.flow.Flow

class ObterPublicacaoPorId(
    private val repositorio: RepositorioPublicacoes,
) {
    operator fun invoke(id: Long): Flow<Publicacao?> =
        repositorio.observarPublicacao(id)
}
