package com.obiterjus.domain.repository

import com.obiterjus.domain.model.OabCadastro
import com.obiterjus.domain.model.SincronizacaoStatus
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface RepositorioCadastroOab {
    val cadastro: Flow<OabCadastro>

    val status: Flow<SincronizacaoStatus>

    suspend fun salvarCadastro(
        numero: String,
        uf: String,
        nomeAdvogado: String = "",
        dataInicio: LocalDate? = null,
        dataFim: LocalDate? = null,
    )

    suspend fun registrarSucesso(
        executadoEm: Instant,
        novasPublicacoes: Int,
    )

    suspend fun registrarFalha(
        executadoEm: Instant,
        mensagem: String,
    )
}
