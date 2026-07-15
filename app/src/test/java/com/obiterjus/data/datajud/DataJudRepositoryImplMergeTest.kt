package com.obiterjus.data.datajud

import com.obiterjus.data.datajud.local.MovimentoDao
import com.obiterjus.data.datajud.local.MovimentoEntity
import com.obiterjus.data.datajud.local.ParticipanteDao
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.datajud.remote.DataJudApi
import com.obiterjus.data.datajud.remote.DataJudRemoteDataSource
import com.obiterjus.data.datajud.remote.dto.DataJudCodigoNomeDto
import com.obiterjus.data.datajud.remote.dto.DataJudHitDto
import com.obiterjus.data.datajud.remote.dto.DataJudHitsDto
import com.obiterjus.data.datajud.remote.dto.DataJudParteDto
import com.obiterjus.data.datajud.remote.dto.DataJudPessoaDto
import com.obiterjus.data.datajud.remote.dto.DataJudPoloDto
import com.obiterjus.data.datajud.remote.dto.DataJudProcessoDto
import com.obiterjus.data.datajud.remote.dto.DataJudSearchRequestDto
import com.obiterjus.data.datajud.remote.dto.DataJudSearchResponseDto
import com.obiterjus.data.datajud.remote.dto.DataJudTotalDto
import com.obiterjus.data.processo.local.LocalProcessoRepository
import com.obiterjus.data.processo.local.ProcessoDao
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.domain.model.ProcessoDataJudSyncRequest
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.SincronizarProcessosDataJudParams
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DataJudRepositoryImplMergeTest {
    private val numeroProcesso = "50110879520258130245"

    @Test
    fun foundMesclaProcessoEParticipantesEmVezDeSobrescrever() = runBlocking {
        val processoDao = FakeProcessoDao(
            processos = mapOf(
                numeroProcesso to ProcessoEntity(
                    numeroProcesso = numeroProcesso,
                    tribunal = "TJMG",
                    grau = null,
                    classeCodigo = null,
                    classeNome = null,
                    assuntosJson = null,
                    orgaoJulgadorCodigo = null,
                    orgaoJulgadorNome = "1ª Vara Cível",
                    nivelSigilo = null,
                    dataAjuizamento = null,
                    syncStatus = ProcessoSyncStatus.PENDING,
                    capturadoEm = Instant.parse("2026-01-01T00:00:00Z"),
                    atualizadoEm = Instant.parse("2026-01-01T00:00:00Z"),
                    valorCausa = 15000.0,
                    advogadosAtivo = "Marcos Paulo",
                ),
            ),
        )
        val participanteDao = FakeParticipanteDao(
            participantes = listOf(
                ParticipanteEntity(
                    idLocal = "djen-1",
                    numeroProcesso = numeroProcesso,
                    polo = "ATIVO",
                    nome = "Antonio Araujo",
                    tipoPessoa = null,
                    tipoParticipacao = "Autor",
                    telefone = "31 99999-0000",
                ),
            ),
        )
        val repository = DataJudRepositoryImpl(
            remoteDataSource = remoteDataSource(
                DataJudProcessoDto(
                    numeroProcesso = numeroProcesso,
                    tribunal = "TJMG",
                    classe = DataJudCodigoNomeDto(codigo = 7, nome = "Procedimento Comum Cível"),
                    polos = listOf(
                        DataJudPoloDto(
                            polo = "AT",
                            partes = listOf(
                                DataJudParteDto(
                                    tipoParticipacao = "REQUERENTE",
                                    pessoa = DataJudPessoaDto(nome = "ANTÔNIO ARAÚJO", tipoPessoa = "fisica"),
                                ),
                            ),
                        ),
                        DataJudPoloDto(
                            polo = "PA",
                            partes = listOf(
                                DataJudParteDto(
                                    tipoParticipacao = "REQUERIDO",
                                    pessoa = DataJudPessoaDto(nome = "BANCO EXEMPLO S.A.", tipoPessoa = "juridica"),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            localProcessoRepository = LocalProcessoRepository(
                processoDao = processoDao,
                movimentoDao = FakeMovimentoDao(),
                participanteDao = participanteDao,
            ),
            clock = Clock.fixed(Instant.parse("2026-04-29T12:00:00Z"), ZoneOffset.UTC),
        )

        repository.sincronizar(
            SincronizarProcessosDataJudParams(
                processos = listOf(
                    ProcessoDataJudSyncRequest(numeroProcesso = numeroProcesso, tribunal = "TJMG"),
                ),
            ),
        )

        val processo = processoDao.saved.getValue(numeroProcesso)
        // Ganhou os dados do DataJud
        assertEquals("Procedimento Comum Cível", processo.classeNome)
        assertEquals(ProcessoSyncStatus.SYNCED, processo.syncStatus)
        // Preservou o que o DataJud não informa (dados do DJEN e do usuário)
        assertEquals("1ª Vara Cível", processo.orgaoJulgadorNome)
        assertEquals(15000.0, processo.valorCausa!!, 0.0)
        assertEquals("Marcos Paulo", processo.advogadosAtivo)
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), processo.capturadoEm)

        val participantes = participanteDao.saved
        assertEquals(2, participantes.size)
        val antonio = participantes.first { it.nome == "Antonio Araujo" }
        // Reconciliado com o registro do DJEN: idLocal e telefone preservados,
        // tipoPessoa completado pelo DataJud
        assertEquals("djen-1", antonio.idLocal)
        assertEquals("31 99999-0000", antonio.telefone)
        assertEquals("fisica", antonio.tipoPessoa)
        assertEquals(1, participantes.count { it.nome == "BANCO EXEMPLO S.A." })
    }

    private fun remoteDataSource(processoDto: DataJudProcessoDto): DataJudRemoteDataSource =
        DataJudRemoteDataSource(
            api = object : DataJudApi {
                override suspend fun buscarProcesso(
                    endpoint: String,
                    authorization: String,
                    request: DataJudSearchRequestDto,
                ): DataJudSearchResponseDto =
                    DataJudSearchResponseDto(
                        hits = DataJudHitsDto(
                            total = DataJudTotalDto(value = 1),
                            hits = listOf(DataJudHitDto(source = processoDto)),
                        ),
                    )
            },
            apiKey = "token",
        )

    private class FakeProcessoDao(
        processos: Map<String, ProcessoEntity>,
    ) : ProcessoDao {
        val saved = processos.toMutableMap()

        override suspend fun upsert(processo: ProcessoEntity) {
            saved[processo.numeroProcesso] = processo
        }

        override suspend fun upsertAll(processos: List<ProcessoEntity>) {
            processos.forEach { saved[it.numeroProcesso] = it }
        }

        override fun observeAll(): Flow<List<ProcessoEntity>> = emptyFlow()
        override fun observeByNumero(numeroProcesso: String): Flow<ProcessoEntity?> = emptyFlow()
        override suspend fun getByNumero(numeroProcesso: String): ProcessoEntity? = saved[numeroProcesso]
        override suspend fun getByNumeros(numerosProcesso: List<String>): List<ProcessoEntity> =
            numerosProcesso.mapNotNull(saved::get)

        override suspend fun deleteByNumeroProcesso(numeroProcesso: String) {
            saved.remove(numeroProcesso)
        }
    }

    private class FakeMovimentoDao : MovimentoDao() {
        override suspend fun upsertAll(movimentos: List<MovimentoEntity>) = Unit
        override fun observeByProcesso(numeroProcesso: String): Flow<List<MovimentoEntity>> = emptyFlow()
        override suspend fun getByProcesso(numeroProcesso: String): List<MovimentoEntity> = emptyList()
        override suspend fun getByIds(ids: List<String>): List<MovimentoEntity> = emptyList()
        override suspend fun replaceForProcesso(numeroProcesso: String, movimentos: List<MovimentoEntity>) = Unit
        override suspend fun deleteByProcesso(numeroProcesso: String) = Unit
    }

    private class FakeParticipanteDao(
        participantes: List<ParticipanteEntity>,
    ) : ParticipanteDao {
        val saved = participantes.toMutableList()

        override suspend fun upsertAll(participantes: List<ParticipanteEntity>) {
            saved += participantes
        }

        override fun observeByNumeroProcesso(numeroProcesso: String): Flow<List<ParticipanteEntity>> = emptyFlow()
        override fun observeAll(): Flow<List<ParticipanteEntity>> = emptyFlow()
        override suspend fun getByNumeroProcesso(numeroProcesso: String): List<ParticipanteEntity> =
            saved.filter { it.numeroProcesso == numeroProcesso }

        override suspend fun getAll(): List<ParticipanteEntity> = saved.toList()
        override suspend fun deleteByNumeroProcesso(numeroProcesso: String) {
            saved.removeAll { it.numeroProcesso == numeroProcesso }
        }
    }
}
