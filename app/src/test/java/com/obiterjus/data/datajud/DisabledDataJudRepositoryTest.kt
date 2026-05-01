package com.obiterjus.data.datajud

import com.obiterjus.domain.model.ProcessoDataJudSyncRequest
import com.obiterjus.domain.model.ProcessoDataJudSyncStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DisabledDataJudRepositoryTest {
    @Test
    fun returnsFailureSummaryWithoutCallingNetwork() = runBlocking {
        val repository = DisabledDataJudRepository(reason = "sem chave")

        val resumo = repository.sincronizar(
            SincronizarProcessosDataJudParams(
                processos = listOf(
                    ProcessoDataJudSyncRequest(
                        numeroProcesso = "50110879520258130245",
                        tribunal = "TJMG",
                    ),
                ),
            ),
        )

        assertEquals(1, resumo.solicitados)
        assertEquals(0, resumo.encontrados)
        assertEquals(1, resumo.falhas)
        assertEquals(ProcessoDataJudSyncStatus.FAILED, resumo.resultados.first().status)
        assertEquals("sem chave", resumo.resultados.first().mensagem)
    }
}
