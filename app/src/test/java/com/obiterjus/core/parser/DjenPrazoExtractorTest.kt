package com.obiterjus.core.parser

import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.data.time.FeriadoRepository
import com.obiterjus.data.time.BrasilApiDataSource
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class DjenPrazoExtractorTest {

    private val feriadoRepository = FeriadoRepository(
        brasilApiDataSource = object : BrasilApiDataSource {
            override suspend fun getFeriadosNacionais(ano: Int): Response<List<com.obiterjus.data.time.FeriadoDto>> =
                Response.success(emptyList())
        }
    )
    private val calculadoraPrazos = CalculadoraPrazos(feriadoRepository)
    private val extractor = DjenPrazoExtractor(calculadoraPrazos)

    @Test
    fun extractsCalendarDayDeadline() = runBlocking {
        val prazo = extractor.extract(
            texto = "Intime-se a parte para manifestar-se no prazo de 15 dias.",
            dataDisponibilizacao = LocalDate.of(2026, 4, 30),
        )

        assertEquals(15, prazo?.quantidade)
        assertEquals("dias", prazo?.unidade)
        assertEquals(false, prazo?.diasUteis)
        assertEquals(LocalDate.of(2026, 5, 15), prazo?.dataLimiteEstimada)
    }

    @Test
    fun extractsBusinessDayDeadline() = runBlocking {
        val prazo = extractor.extract(
            texto = "Fica intimado para regularizar em 5 dias úteis.",
            dataDisponibilizacao = LocalDate.of(2026, 4, 30),
        )

        assertEquals(5, prazo?.quantidade)
        assertEquals(true, prazo?.diasUteis)
    }
}
