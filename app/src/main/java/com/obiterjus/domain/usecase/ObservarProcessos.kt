package com.obiterjus.domain.usecase

import com.obiterjus.domain.repository.RepositorioProcessos

class ObservarProcessos(
    val repositorio: RepositorioProcessos,
) {
    operator fun invoke() = repositorio.observarProcessos()
}
