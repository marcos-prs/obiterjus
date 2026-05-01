package com.obiterjus.data.datajud.mapper

import com.obiterjus.data.datajud.remote.dto.DataJudCodigoNomeDto
import com.obiterjus.data.datajud.remote.dto.DataJudMovimentoDto
import com.obiterjus.data.datajud.remote.dto.DataJudOrgaoJulgadorDto
import com.obiterjus.data.datajud.remote.dto.DataJudProcessoDto
import com.obiterjus.domain.model.ProcessoSyncStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class DataJudMappersTest {
    @Test
    fun mapsProcessoAndMovimentosToLocalEntities() {
        val syncedAt = Instant.parse("2026-04-29T12:00:00Z")
        val dto = DataJudProcessoDto(
            numeroProcesso = "5011087-95.2025.8.13.0245",
            tribunal = "TJMG",
            grau = "G1",
            classe = DataJudCodigoNomeDto(codigo = 7, nome = " Procedimento Comum Civel "),
            assuntos = listOf(
                DataJudCodigoNomeDto(codigo = 101, nome = " Contratos Bancarios "),
                DataJudCodigoNomeDto(codigo = 102, nome = " Indenizacao "),
            ),
            orgaoJulgador = DataJudOrgaoJulgadorDto(codigo = 10, nome = " 1a Vara "),
            nivelSigilo = 0,
            dataAjuizamento = "20260219143412",
            movimentos = listOf(
                DataJudMovimentoDto(
                    codigo = 123,
                    nome = " Conclusos ",
                    dataHora = "2026-04-02T05:01:47.757000Z",
                    complementosTabelados = listOf(
                        JsonObject(mapOf("texto" to JsonPrimitive("Complemento"))),
                    ),
                ),
            ),
        )

        val processo = dto.toProcessoEntity(
            fallbackNumeroProcesso = "50110879520258130245",
            fallbackTribunal = "TJMG",
            syncedAt = syncedAt,
            syncStatus = ProcessoSyncStatus.SYNCED,
        )
        val movimentos = dto.toMovimentoEntities(processo.numeroProcesso)

        assertEquals("50110879520258130245", processo.numeroProcesso)
        assertEquals("TJMG", processo.tribunal)
        assertEquals("Procedimento Comum Civel", processo.classeNome)
        assertTrue(processo.assuntosJson.orEmpty().contains("Contratos Bancarios"))
        assertTrue(processo.assuntosJson.orEmpty().contains("Indenizacao"))
        assertEquals("1a Vara", processo.orgaoJulgadorNome)
        assertEquals(Instant.parse("2026-02-19T14:34:12Z"), processo.dataAjuizamento)
        assertEquals(ProcessoSyncStatus.SYNCED, processo.syncStatus)

        assertEquals(1, movimentos.size)
        assertEquals("Conclusos", movimentos.first().nome)
        assertEquals(Instant.parse("2026-04-02T05:01:47.757Z"), movimentos.first().dataHora)
        assertNotNull(movimentos.first().complementosJson)
        assertEquals(64, movimentos.first().idLocal.length)
    }

    @Test
    fun movementIdIsStableForSameInput() {
        val dto = DataJudProcessoDto(
            numeroProcesso = "50110879520258130245",
            movimentos = listOf(
                DataJudMovimentoDto(
                    codigo = 123,
                    nome = "Conclusos",
                    dataHora = "20260219143412",
                ),
            ),
        )

        val first = dto.toMovimentoEntities("50110879520258130245").first()
        val second = dto.toMovimentoEntities("50110879520258130245").first()

        assertEquals(first.idLocal, second.idLocal)
        assertTrue(first.complementosJson == null)
    }
}
