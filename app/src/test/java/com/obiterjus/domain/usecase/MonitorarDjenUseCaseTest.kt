package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.MonitorarDjenModo
import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.MonitorarDjenResumo
import com.obiterjus.domain.model.MonitorarDjenStopReason
import com.obiterjus.domain.repository.DjenRepository
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MonitorarDjenUseCaseTest {
    @Test
    fun normalizesOabUfBeforeCallingRepository() = runBlocking {
        val repository = FakeDjenRepository()
        val useCase = MonitorarDjenUseCase(repository)

        useCase(
            MonitorarDjenParams(
                numeroOab = " 12345 ",
                ufOab = " mg ",
                dataInicio = LocalDate.of(2026, 4, 1),
                dataFim = LocalDate.of(2026, 4, 29),
                modo = MonitorarDjenModo.MANUAL,
            ),
        )

        assertEquals("12345", repository.lastParams?.numeroOab)
        assertEquals("MG", repository.lastParams?.ufOab)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDateWindow() {
        runBlocking {
            MonitorarDjenUseCase(FakeDjenRepository())(
                MonitorarDjenParams(
                    numeroOab = "12345",
                    ufOab = "MG",
                    dataInicio = LocalDate.of(2026, 4, 29),
                    dataFim = LocalDate.of(2026, 4, 1),
                    modo = MonitorarDjenModo.MANUAL,
                ),
            )
        }
    }

    private class FakeDjenRepository : DjenRepository {
        var lastParams: MonitorarDjenParams? = null

        override suspend fun monitorar(params: MonitorarDjenParams): MonitorarDjenResumo {
            lastParams = params
            return MonitorarDjenResumo(
                totalRemoto = 0,
                totalRecebidas = 0,
                novas = 0,
                atualizadas = 0,
                sigilosas = 0,
                processosNovos = emptyList(),
                processosParaSincronizar = emptyList(),
                paginasConsultadas = 0,
                motivoParada = MonitorarDjenStopReason.EMPTY_PAGE,
                falhas = emptyList(),
            )
        }
    }
}
