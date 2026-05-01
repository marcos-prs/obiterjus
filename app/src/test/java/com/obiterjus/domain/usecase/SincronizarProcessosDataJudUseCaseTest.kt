package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.ProcessoDataJudSyncRequest
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.domain.repository.DataJudRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SincronizarProcessosDataJudUseCaseTest {
    @Test
    fun normalizesAndDeduplicatesRequestsBeforeCallingRepository() = runBlocking {
        val repository = FakeDataJudRepository()
        val useCase = SincronizarProcessosDataJudUseCase(repository)

        useCase(
            SincronizarProcessosDataJudParams(
                processos = listOf(
                    ProcessoDataJudSyncRequest("5011087-95.2025.8.13.0245", " tjmg "),
                    ProcessoDataJudSyncRequest("50110879520258130245", "TJMG"),
                    ProcessoDataJudSyncRequest("123", "TJMG"),
                ),
            ),
        )

        assertEquals(
            listOf(ProcessoDataJudSyncRequest("50110879520258130245", "tjmg")),
            repository.lastParams?.processos,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyValidProcessList() {
        runBlocking {
            SincronizarProcessosDataJudUseCase(FakeDataJudRepository())(
                SincronizarProcessosDataJudParams(
                    processos = listOf(ProcessoDataJudSyncRequest("123", "TJMG")),
                ),
            )
        }
    }

    private class FakeDataJudRepository : DataJudRepository {
        var lastParams: SincronizarProcessosDataJudParams? = null

        override suspend fun sincronizar(
            params: SincronizarProcessosDataJudParams,
        ): SincronizarProcessosDataJudResumo {
            lastParams = params
            return SincronizarProcessosDataJudResumo(
                solicitados = params.processos.size,
                normalizados = params.processos.size,
                encontrados = 0,
                naoEncontrados = 0,
                falhas = 0,
                movimentosSalvos = 0,
                resultados = emptyList(),
            )
        }
    }
}
