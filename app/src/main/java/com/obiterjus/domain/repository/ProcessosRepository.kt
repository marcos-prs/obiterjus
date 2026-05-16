package com.obiterjus.domain.repository

import com.obiterjus.domain.model.MovimentoProcesso
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.ProcessoMonitorado
import kotlinx.coroutines.flow.Flow

interface ProcessosRepository {
    fun observarProcessos(): Flow<List<ProcessoMonitorado>>

    fun observarMovimentos(numeroProcesso: String): Flow<List<MovimentoProcesso>>

    fun observarParticipantes(numeroProcesso: String): Flow<List<ParticipanteProcesso>>

    suspend fun obterProcesso(numeroProcesso: String): ProcessoMonitorado?

    suspend fun salvarProcesso(processo: ProcessoMonitorado)

    suspend fun excluirProcesso(numeroProcesso: String)
}
