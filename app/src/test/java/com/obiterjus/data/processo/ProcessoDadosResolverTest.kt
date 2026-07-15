package com.obiterjus.data.processo

import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.domain.model.ProcessoSyncStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProcessoDadosResolverTest {

    @Test
    fun mesclarProcessoPreservaCamposQueAFonteNaoInforma() {
        val existente = processo(
            tribunal = "TJMG",
            orgaoJulgadorNome = "1ª Vara Cível",
            valorCausa = 15000.0,
            advogadosAtivo = "Marcos Paulo",
            capturadoEm = Instant.parse("2026-01-01T00:00:00Z"),
        )
        val vindoDoDataJud = processo(
            tribunal = null,
            classeNome = "Procedimento Comum Cível",
            classeCodigo = 7,
            syncStatus = ProcessoSyncStatus.SYNCED,
            capturadoEm = Instant.parse("2026-02-01T00:00:00Z"),
            atualizadoEm = Instant.parse("2026-02-01T00:00:00Z"),
        )

        val merged = ProcessoDadosResolver.mesclarProcesso(existente, vindoDoDataJud)

        // Fonte nova prevalece no que informa
        assertEquals("Procedimento Comum Cível", merged.classeNome)
        assertEquals(7, merged.classeCodigo)
        assertEquals(ProcessoSyncStatus.SYNCED, merged.syncStatus)
        // O que a fonte não informa é preservado
        assertEquals("TJMG", merged.tribunal)
        assertEquals("1ª Vara Cível", merged.orgaoJulgadorNome)
        assertEquals(15000.0, merged.valorCausa!!, 0.0)
        assertEquals("Marcos Paulo", merged.advogadosAtivo)
        // Primeira captura é mantida
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), merged.capturadoEm)
        assertEquals(Instant.parse("2026-02-01T00:00:00Z"), merged.atualizadoEm)
    }

    @Test
    fun mesclarProcessoSemExistenteRetornaONovo() {
        val novo = processo(tribunal = "TJSP")
        assertEquals(novo, ProcessoDadosResolver.mesclarProcesso(null, novo))
    }

    @Test
    fun migracaoDeInstanciaNaoHerdaGrauClasseNemOrgaoDaInstanciaAnterior() {
        // Processo registrado no TJMG (G2) migrou para o STJ via AREsp.
        val existente = processo(
            tribunal = "TJMG",
            grau = "G2",
            classeNome = "Recurso Extraordinário",
            classeCodigo = 1348,
            orgaoJulgadorNome = "Gabinete da 3ª Vice-Presidência",
            capturadoEm = Instant.parse("2026-01-01T00:00:00Z"),
        )
        val vindoDoStj = processo(
            tribunal = "STJ",
            grau = null,
            classeNome = "Agravo em Recurso Especial",
            classeCodigo = 12395,
            orgaoJulgadorNome = null,
            syncStatus = ProcessoSyncStatus.SYNCED,
            capturadoEm = Instant.parse("2026-06-11T00:00:00Z"),
            atualizadoEm = Instant.parse("2026-06-11T00:00:00Z"),
        )

        val merged = ProcessoDadosResolver.mesclarProcesso(existente, vindoDoStj)

        assertEquals("STJ", merged.tribunal)
        assertEquals("Agravo em Recurso Especial", merged.classeNome)
        // Campos de instância NÃO regressam aos valores do TJMG
        assertNull(merged.grau)
        assertNull(merged.orgaoJulgadorNome)
    }

    @Test
    fun mesmaInstanciaContinuaPreservandoGrauEOrgao() {
        val existente = processo(
            tribunal = "TJMG",
            grau = "G2",
            orgaoJulgadorNome = "Gabinete da 3ª Vice-Presidência",
        )
        val reSync = processo(
            tribunal = "TJMG",
            grau = null,
            orgaoJulgadorNome = null,
            syncStatus = ProcessoSyncStatus.SYNCED,
        )

        val merged = ProcessoDadosResolver.mesclarProcesso(existente, reSync)

        assertEquals("G2", merged.grau)
        assertEquals("Gabinete da 3ª Vice-Presidência", merged.orgaoJulgadorNome)
    }

    @Test
    fun mesclarParticipantesReconciliaDjenComDataJudPorNomeNormalizado() {
        val doDjen = participante(
            idLocal = "djen-1",
            nome = "Antonio Araujo",
            polo = "ATIVO",
            tipoParticipacao = "Autor",
            telefone = "31 99999-0000",
        )
        val doDataJud = participante(
            idLocal = "datajud-1",
            nome = "ANTÔNIO ARAÚJO",
            polo = "AT",
            tipoParticipacao = "REQUERENTE",
            tipoPessoa = "fisica",
        )

        val merged = ProcessoDadosResolver.mesclarParticipantes(
            existentes = listOf(doDjen),
            novos = listOf(doDataJud),
        )

        assertEquals(1, merged.size)
        val unico = merged.single()
        // Registro existente é a base: idLocal estável e dados do usuário preservados
        assertEquals("djen-1", unico.idLocal)
        assertEquals("Antonio Araujo", unico.nome)
        assertEquals("31 99999-0000", unico.telefone)
        // A fonte nova completa as lacunas
        assertEquals("fisica", unico.tipoPessoa)
        assertEquals("ATIVO", unico.polo)
    }

    @Test
    fun mesclarParticipantesNaoMisturaAdvogadoComParteHomonima() {
        val parte = participante(idLocal = "p1", nome = "João da Silva", tipoParticipacao = "Autor")
        val advogado = participante(idLocal = "p2", nome = "João da Silva", tipoParticipacao = "ADVOGADO")

        val merged = ProcessoDadosResolver.mesclarParticipantes(
            existentes = listOf(parte),
            novos = listOf(advogado),
        )

        assertEquals(2, merged.size)
    }

    @Test
    fun mesclarParticipantesAdicionaInéditosEDeduplicaExistentes() {
        val duplicadoA = participante(idLocal = "a", nome = "Banco Exemplo S.A.", polo = "PASSIVO")
        val duplicadoB = participante(idLocal = "b", nome = "BANCO EXEMPLO S.A.", polo = "PA")
        val inedito = participante(idLocal = "c", nome = "Maria Souza", polo = "AT")

        val merged = ProcessoDadosResolver.mesclarParticipantes(
            existentes = listOf(duplicadoA, duplicadoB),
            novos = listOf(inedito),
        )

        assertEquals(2, merged.size)
        assertEquals("ATIVO", merged.first { it.idLocal == "c" }.polo)
        assertEquals("a", merged.first { it.polo == "PASSIVO" }.idLocal)
    }

    @Test
    fun participanteSemNomeNaoEReconciliado() {
        val semNomeA = participante(idLocal = "x", nome = null)
        val semNomeB = participante(idLocal = "y", nome = " ")

        val merged = ProcessoDadosResolver.mesclarParticipantes(
            existentes = listOf(semNomeA),
            novos = listOf(semNomeB),
        )

        assertEquals(2, merged.size)
    }

    @Test
    fun normalizarPoloCobreCodigosDoDataJudERotulosDoDjen() {
        assertEquals("ATIVO", ProcessoDadosResolver.normalizarPolo("AT"))
        assertEquals("ATIVO", ProcessoDadosResolver.normalizarPolo("Polo Ativo"))
        assertEquals("PASSIVO", ProcessoDadosResolver.normalizarPolo("PA"))
        assertEquals("PASSIVO", ProcessoDadosResolver.normalizarPolo("passivo"))
        assertEquals("TC", ProcessoDadosResolver.normalizarPolo("TC"))
        assertNull(ProcessoDadosResolver.normalizarPolo(null))
        assertNull(ProcessoDadosResolver.normalizarPolo("  "))
    }

    private fun processo(
        numeroProcesso: String = "50110879520258130245",
        tribunal: String? = null,
        grau: String? = null,
        classeCodigo: Int? = null,
        classeNome: String? = null,
        orgaoJulgadorNome: String? = null,
        valorCausa: Double? = null,
        advogadosAtivo: String? = null,
        syncStatus: ProcessoSyncStatus = ProcessoSyncStatus.PENDING,
        capturadoEm: Instant = Instant.EPOCH,
        atualizadoEm: Instant = capturadoEm,
    ): ProcessoEntity =
        ProcessoEntity(
            numeroProcesso = numeroProcesso,
            tribunal = tribunal,
            grau = grau,
            classeCodigo = classeCodigo,
            classeNome = classeNome,
            assuntosJson = null,
            orgaoJulgadorCodigo = null,
            orgaoJulgadorNome = orgaoJulgadorNome,
            nivelSigilo = null,
            dataAjuizamento = null,
            syncStatus = syncStatus,
            capturadoEm = capturadoEm,
            atualizadoEm = atualizadoEm,
            valorCausa = valorCausa,
            advogadosAtivo = advogadosAtivo,
        )

    private fun participante(
        idLocal: String,
        nome: String?,
        polo: String? = null,
        tipoParticipacao: String? = null,
        tipoPessoa: String? = null,
        telefone: String? = null,
    ): ParticipanteEntity =
        ParticipanteEntity(
            idLocal = idLocal,
            numeroProcesso = "50110879520258130245",
            polo = polo,
            nome = nome,
            tipoPessoa = tipoPessoa,
            tipoParticipacao = tipoParticipacao,
            telefone = telefone,
        )
}
