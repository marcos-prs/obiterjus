package com.obiterjus.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.obiterjus.data.datajud.local.MovimentoEntity
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.domain.model.ProcessoSyncStatus
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ObiterDatabaseInstrumentedTest {
    private lateinit var database: ObiterDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ObiterDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun persistsPublicacaoProcessoAndMovimentos() = runBlocking {
        val capturedAt = Instant.parse("2026-04-29T12:00:00Z")
        val numeroProcesso = "50110879520258130245"

        database.publicacaoDao().upsert(
            PublicacaoEntity(
                id = 10L,
                hash = "hash-10",
                numeroProcesso = numeroProcesso,
                participantesJson = null,
                prazoQuantidade = null,
                prazoUnidade = null,
                prazoDiasUteis = false,
                prazoTexto = null,
                prazoDataLimite = null,
                dataDisponibilizacao = LocalDate.of(2026, 4, 29),
                tribunal = "TJMG",
                tipoComunicacao = "Intimacao",
                nomeOrgao = "1a Vara",
                idOrgao = 1L,
                textoRaw = "<p>Intime-se.</p>",
                textoLimpo = "Intime-se.",
                textoPossuiHtml = true,
                textoPossuiErroTemplate = false,
                isSigiloso = false,
                ativo = true,
                fonte = "DJEN",
                capturadoEm = capturedAt,
                atualizadoEm = capturedAt,
            ),
        )
        database.processoDao().upsert(
            ProcessoEntity(
                numeroProcesso = numeroProcesso,
                tribunal = "TJMG",
                grau = "G1",
                classeCodigo = 7,
                classeNome = "Procedimento Comum Civel",
                assuntosJson = null,
                orgaoJulgadorCodigo = 1,
                orgaoJulgadorNome = "1a Vara",
                nivelSigilo = 0,
                dataAjuizamento = capturedAt,
                syncStatus = ProcessoSyncStatus.SYNCED,
                capturadoEm = capturedAt,
                atualizadoEm = capturedAt,
            ),
        )
        database.movimentoDao().replaceForProcesso(
            numeroProcesso = numeroProcesso,
            movimentos = listOf(
                MovimentoEntity(
                    idLocal = "$numeroProcesso-1",
                    numeroProcesso = numeroProcesso,
                    codigo = 1,
                    nome = "Conclusos",
                    dataHora = capturedAt,
                    complementosJson = null,
                ),
            ),
        )

        assertNotNull(database.publicacaoDao().getById(10L))
        assertNotNull(database.processoDao().getByNumero(numeroProcesso))
        assertEquals(1, database.movimentoDao().getByProcesso(numeroProcesso).size)
    }
}
