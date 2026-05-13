package com.obiterjus.domain.usecase

import com.obiterjus.domain.repository.RepositorioProcessos

class ExcluirProcessoUseCase(
    private val repositorio: RepositorioProcessos,
) {
    suspend operator fun invoke(numeroProcesso: String) {
        repositorio.excluirProcesso(numeroProcesso)
    }
}
