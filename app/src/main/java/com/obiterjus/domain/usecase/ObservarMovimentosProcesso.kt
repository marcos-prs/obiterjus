package com.obiterjus.domain.usecase

import com.obiterjus.domain.repository.RepositorioProcessos

class ObservarMovimentosProcesso(
    private val repositorioProcessos: RepositorioProcessos,
) {
    operator fun invoke(numeroProcesso: String) =
        repositorioProcessos.observarMovimentos(numeroProcesso)
}
