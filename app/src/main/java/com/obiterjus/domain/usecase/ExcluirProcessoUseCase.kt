package com.obiterjus.domain.usecase

import com.obiterjus.domain.repository.ProcessosRepository

class ExcluirProcessoUseCase(
    private val repositorio: ProcessosRepository,
) {
    suspend operator fun invoke(numeroProcesso: String) {
        repositorio.excluirProcesso(numeroProcesso)
    }
}
