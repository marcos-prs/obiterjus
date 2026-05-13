package com.obiterjus.data.processo.local

import com.obiterjus.data.datajud.local.MovimentoDao
import com.obiterjus.data.datajud.local.MovimentoEntity
import com.obiterjus.data.publicacao.local.toAssuntos
import com.obiterjus.domain.model.MovimentoProcesso
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.repository.RepositorioProcessos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

import com.obiterjus.data.datajud.local.ParticipanteDao
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.publicacao.local.toAssuntosJsonOrNull

class LocalProcessoRepository(
    private val processoDao: ProcessoDao,
    private val movimentoDao: MovimentoDao,
    private val participanteDao: ParticipanteDao,
) : RepositorioProcessos {
    override fun observarProcessos(): Flow<List<ProcessoMonitorado>> =
        combine(observeProcessos(), participanteDao.observeAll()) { processos, participantes ->
            val participantesPorProcesso = participantes
                .map(ParticipanteEntity::paraDominio)
                .groupBy(ParticipanteProcesso::numeroProcesso)
            processos.map { processo ->
                processo.paraDominio(
                    participantes = participantesPorProcesso[processo.numeroProcesso].orEmpty(),
                )
            }
        }

    override fun observarMovimentos(numeroProcesso: String): Flow<List<MovimentoProcesso>> =
        observeMovimentos(numeroProcesso).map { movimentos ->
            movimentos.map(MovimentoEntity::paraDominio)
        }

    override fun observarParticipantes(numeroProcesso: String): Flow<List<ParticipanteProcesso>> =
        observeParticipantes(numeroProcesso).map { participantes ->
            participantes.map(ParticipanteEntity::paraDominio)
        }

    fun observeProcessos(): Flow<List<ProcessoEntity>> = processoDao.observeAll()

    fun observeProcesso(numeroProcesso: String): Flow<ProcessoEntity?> =
        processoDao.observeByNumero(numeroProcesso)

    fun observeMovimentos(numeroProcesso: String): Flow<List<MovimentoEntity>> =
        movimentoDao.observeByProcesso(numeroProcesso)

    suspend fun upsertProcesso(processo: ProcessoEntity) {
        processoDao.upsert(processo)
    }

    suspend fun upsertProcessos(processos: List<ProcessoEntity>) {
        if (processos.isNotEmpty()) {
            processoDao.upsertAll(processos)
        }
    }

    override suspend fun obterProcesso(numeroProcesso: String): ProcessoMonitorado? {
        val entity = processoDao.getByNumero(numeroProcesso) ?: return null
        val participantes = participanteDao.getByNumeroProcesso(numeroProcesso)
            .map(ParticipanteEntity::paraDominio)
        return entity.paraDominio(participantes)
    }

    override suspend fun salvarProcesso(processo: ProcessoMonitorado) {
        processoDao.upsert(processo.paraEntidade())
        if (processo.participantes.isNotEmpty()) {
            replaceParticipantes(
                numeroProcesso = processo.numeroProcesso,
                participantes = processo.participantes.map { it.paraEntidade() },
            )
        }
    }

    override suspend fun excluirProcesso(numeroProcesso: String) {
        movimentoDao.deleteByProcesso(numeroProcesso)
        participanteDao.deleteByNumeroProcesso(numeroProcesso)
        processoDao.deleteByNumeroProcesso(numeroProcesso)
    }

    suspend fun getProcesso(numeroProcesso: String): ProcessoEntity? =
        processoDao.getByNumero(numeroProcesso)

    suspend fun getProcessos(numerosProcesso: List<String>): List<ProcessoEntity> =
        if (numerosProcesso.isEmpty()) {
            emptyList()
        } else {
            processoDao.getByNumeros(numerosProcesso)
        }

    suspend fun replaceMovimentos(
        numeroProcesso: String,
        movimentos: List<MovimentoEntity>,
    ) {
        movimentoDao.replaceForProcesso(numeroProcesso, movimentos)
    }

    suspend fun getMovimentos(numeroProcesso: String): List<MovimentoEntity> =
        movimentoDao.getByProcesso(numeroProcesso)

    suspend fun getMovimentos(ids: List<String>): List<MovimentoEntity> =
        if (ids.isEmpty()) {
            emptyList()
        } else {
            movimentoDao.getByIds(ids)
        }

    suspend fun upsertMovimentos(movimentos: List<MovimentoEntity>) {
        if (movimentos.isNotEmpty()) {
            movimentoDao.upsertAll(movimentos)
        }
    }

    suspend fun replaceParticipantes(
        numeroProcesso: String,
        participantes: List<ParticipanteEntity>,
    ) {
        participanteDao.deleteByNumeroProcesso(numeroProcesso)
        if (participantes.isNotEmpty()) {
            participanteDao.upsertAll(participantes)
        }
    }

    suspend fun getParticipantes(numeroProcesso: String): List<ParticipanteEntity> =
        participanteDao.getByNumeroProcesso(numeroProcesso)

    suspend fun getTodosParticipantes(): List<ParticipanteEntity> =
        participanteDao.getAll()

    fun observeParticipantes(numeroProcesso: String): Flow<List<ParticipanteEntity>> =
        participanteDao.observeByNumeroProcesso(numeroProcesso)
}

private fun ProcessoEntity.paraDominio(
    participantes: List<ParticipanteProcesso> = emptyList(),
): ProcessoMonitorado =
    ProcessoMonitorado(
        numeroProcesso = numeroProcesso,
        tribunal = tribunal,
        grau = grau,
        classeCodigo = classeCodigo,
        classeNome = classeNome,
        assuntos = assuntosJson.toAssuntos(),
        orgaoJulgadorCodigo = orgaoJulgadorCodigo,
        orgaoJulgadorNome = orgaoJulgadorNome,
        nivelSigilo = nivelSigilo,
        dataAjuizamento = dataAjuizamento,
        syncStatus = syncStatus,
        capturadoEm = capturadoEm,
        atualizadoEm = atualizadoEm,
        dataJudTentativasRestantes = dataJudTentativasRestantes,
        participantes = participantes,
        dataDistribuicao = dataDistribuicao,
        comarcaSecao = comarcaSecao,
        juizo = juizo,
        prioridadeTramitacao = prioridadeTramitacao,
        gratuidadeJustica = gratuidadeJustica,
        valorCausa = valorCausa,
        faseProcessual = faseProcessual,
        situacaoAtual = situacaoAtual,
        tutelaAntecipadaLiminar = tutelaAntecipadaLiminar,
        advogadosAtivo = advogadosAtivo,
        advogadosPassivo = advogadosPassivo,
        defensoriaPublica = defensoriaPublica,
        ministerioPublico = ministerioPublico,
        terceirosAuxiliares = terceirosAuxiliares,
    )

private fun MovimentoEntity.paraDominio(): MovimentoProcesso =
    MovimentoProcesso(
        idLocal = idLocal,
        numeroProcesso = numeroProcesso,
        codigo = codigo,
        nome = nome,
        dataHora = dataHora,
        complementosJson = complementosJson,
    )

private fun ParticipanteEntity.paraDominio(): com.obiterjus.domain.model.ParticipanteProcesso =
    com.obiterjus.domain.model.ParticipanteProcesso(
        idLocal = idLocal,
        numeroProcesso = numeroProcesso,
        polo = polo,
        nome = nome,
        tipoPessoa = tipoPessoa,
        tipoParticipacao = tipoParticipacao,
        cpfCnpj = cpfCnpj,
        estadoCivil = estadoCivil,
        profissao = profissao,
        endereco = endereco,
        contatos = contatos,
    )

private fun ProcessoMonitorado.paraEntidade(): ProcessoEntity =
    ProcessoEntity(
        numeroProcesso = numeroProcesso,
        tribunal = tribunal,
        grau = grau,
        classeCodigo = classeCodigo,
        classeNome = classeNome,
        assuntosJson = assuntos.toAssuntosJsonOrNull(),
        orgaoJulgadorCodigo = orgaoJulgadorCodigo,
        orgaoJulgadorNome = orgaoJulgadorNome,
        nivelSigilo = nivelSigilo,
        dataAjuizamento = dataAjuizamento,
        syncStatus = syncStatus,
        capturadoEm = capturadoEm,
        atualizadoEm = atualizadoEm,
        dataJudTentativasRestantes = dataJudTentativasRestantes,
        dataDistribuicao = dataDistribuicao,
        comarcaSecao = comarcaSecao,
        juizo = juizo,
        prioridadeTramitacao = prioridadeTramitacao,
        gratuidadeJustica = gratuidadeJustica,
        valorCausa = valorCausa,
        faseProcessual = faseProcessual,
        situacaoAtual = situacaoAtual,
        tutelaAntecipadaLiminar = tutelaAntecipadaLiminar,
        advogadosAtivo = advogadosAtivo,
        advogadosPassivo = advogadosPassivo,
        defensoriaPublica = defensoriaPublica,
        ministerioPublico = ministerioPublico,
        terceirosAuxiliares = terceirosAuxiliares,
    )

private fun com.obiterjus.domain.model.ParticipanteProcesso.paraEntidade(): ParticipanteEntity =
    ParticipanteEntity(
        idLocal = idLocal,
        numeroProcesso = numeroProcesso,
        polo = polo,
        nome = nome,
        tipoPessoa = tipoPessoa,
        tipoParticipacao = tipoParticipacao,
        cpfCnpj = cpfCnpj,
        estadoCivil = estadoCivil,
        profissao = profissao,
        endereco = endereco,
        contatos = contatos,
    )
