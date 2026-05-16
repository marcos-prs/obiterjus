package com.obiterjus.domain.usecase

import com.obiterjus.domain.repository.ProcessosRepository

class ObservarMovimentosProcesso(
    private val repositorioProcessos: ProcessosRepository,
) {
    operator fun invoke(numeroProcesso: String) =
        repositorioProcessos.observarMovimentos(numeroProcesso)
}
