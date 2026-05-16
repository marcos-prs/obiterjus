package com.obiterjus.domain.usecase

import com.obiterjus.domain.repository.ProcessosRepository

class ObservarProcessos(
    val repositorio: ProcessosRepository,
) {
    operator fun invoke() = repositorio.observarProcessos()
}
