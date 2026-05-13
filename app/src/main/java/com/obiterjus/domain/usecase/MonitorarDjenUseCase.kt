package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.MonitorarDjenResumo
import com.obiterjus.domain.repository.DjenRepository

class MonitorarDjenUseCase(
    private val repository: DjenRepository,
) {
    suspend operator fun invoke(params: MonitorarDjenParams): MonitorarDjenResumo {
        val numeroOab = params.numeroOab.trim()
        val ufOab = params.ufOab.trim().uppercase()
        val nome = params.nomeAdvogado?.trim().orEmpty()

        if (params.apenasPorNome) {
            require(nome.isNotBlank()) {
                "Nome do advogado é obrigatório no modo de busca por nome."
            }
        } else {
            require(numeroOab.isNotBlank()) { "Número da OAB é obrigatório." }
            require(ufOab.length == 2) { "UF da OAB deve ter 2 caracteres." }
        }
        require(!params.dataFim.isBefore(params.dataInicio)) {
            "Data final não pode ser anterior à data inicial."
        }

        return repository.monitorar(
            params.copy(
                numeroOab = numeroOab,
                ufOab = ufOab,
                nomeAdvogado = nome.takeIf { it.isNotBlank() },
            ),
        )
    }
}
