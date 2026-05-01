package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.repository.RepositorioPublicacoes
import kotlinx.coroutines.flow.Flow

class ObservarPublicacoes(
    private val repositorio: RepositorioPublicacoes,
) {
    operator fun invoke(): Flow<List<Publicacao>> =
        repositorio.observarPublicacoes()
}
