package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.MonitorarDjenModo
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.MonitorarDjenResumo
import com.obiterjus.domain.model.MonitorarDjenStopReason
import com.obiterjus.domain.model.ProcessoDataJudSyncRequest
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import com.obiterjus.domain.model.SincronizarProcessosDataJudResumo
import com.obiterjus.domain.repository.DataJudRepository
import com.obiterjus.domain.repository.DjenRepository
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitorarCnjUseCaseTest {
    @Test
    fun syncsDataJudForNewDjenProcesses() = runBlocking {
        val processo = ProcessoDataJudSyncRequest(
            numeroProcesso = "50110879520258130245",
            tribunal = "TJMG",
        )
        val djenRepository = FakeDjenRepository(
            resumo = djenResumo(processos = listOf(processo)),
        )
        val dataJudRepository = FakeDataJudRepository()
        val useCase = MonitorarCnjUseCase(
            monitorarDjenUseCase = MonitorarDjenUseCase(djenRepository),
            sincronizarProcessosDataJudUseCase = SincronizarProcessosDataJudUseCase(dataJudRepository),
        )

        val resumo = useCase(params())

        assertTrue(resumo.teveSincronizacaoDataJud)
        assertEquals(1, resumo.totalProcessosSincronizados)
        assertEquals(listOf(processo), dataJudRepository.lastParams?.processos)
    }

    @Test
    fun skipsDataJudWhenDjenHasNoNewProcesses() = runBlocking {
        val djenRepository = FakeDjenRepository(
            resumo = djenResumo(processos = emptyList()),
        )
        val dataJudRepository = FakeDataJudRepository()
        val useCase = MonitorarCnjUseCase(
            monitorarDjenUseCase = MonitorarDjenUseCase(djenRepository),
            sincronizarProcessosDataJudUseCase = SincronizarProcessosDataJudUseCase(dataJudRepository),
        )

        val resumo = useCase(params())

        assertFalse(resumo.teveSincronizacaoDataJud)
        assertEquals(0, resumo.totalProcessosSincronizados)
        assertNull(resumo.dataJud)
        assertNull(dataJudRepository.lastParams)
    }

    private fun params(): MonitorarDjenParams =
        MonitorarDjenParams(
            numeroOab = "12345",
            ufOab = "MG",
            dataInicio = LocalDate.of(2026, 4, 1),
            dataFim = LocalDate.of(2026, 4, 29),
            modo = MonitorarDjenModo.MANUAL,
        )

    private fun djenResumo(
        processos: List<ProcessoDataJudSyncRequest>,
    ): MonitorarDjenResumo =
        MonitorarDjenResumo(
            totalRemoto = processos.size,
            totalRecebidas = processos.size,
            novas = processos.size,
            atualizadas = 0,
            sigilosas = 0,
            processosNovos = processos.map { it.numeroProcesso },
            processosParaSincronizar = processos,
            paginasConsultadas = 1,
            motivoParada = MonitorarDjenStopReason.COUNT_CONSUMED,
            falhas = emptyList(),
        )

    private class FakeDjenRepository(
        private val resumo: MonitorarDjenResumo,
    ) : DjenRepository {
        override suspend fun monitorar(params: MonitorarDjenParams): MonitorarDjenResumo = resumo
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
                encontrados = params.processos.size,
                naoEncontrados = 0,
                falhas = 0,
                movimentosSalvos = 0,
                resultados = emptyList(),
            )
        }
    }
}
