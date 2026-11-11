package com.obiterjus.data.sincronizacao

import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.obiterjus.data.agenda.local.PrazoSugeridoDao
import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.data.cliente.local.ClienteDao
import com.obiterjus.data.cliente.local.ClienteEntity
import com.obiterjus.data.cliente.local.ClienteProcessoEntity
import com.obiterjus.data.datajud.local.MovimentoEntity
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.djen.DjenPartesResolver
import com.obiterjus.data.minuta.local.MinutaDao
import com.obiterjus.data.minuta.local.MinutaEntity
import com.obiterjus.data.processo.local.LocalProcessoRepository
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.data.publicacao.local.LocalPublicacaoRepository
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.domain.model.SincronizacaoNuvemResumo
import com.obiterjus.domain.repository.CadastroOabRepository
import com.obiterjus.domain.repository.SincronizacaoRepository
import com.obiterjus.domain.usecase.AplicarPreferenciaSincronizacaoUC
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class FirestoreSincronizacaoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localProcessoRepository: LocalProcessoRepository,
    private val localPublicacaoRepository: LocalPublicacaoRepository,
    private val clienteDao: ClienteDao,
    private val prazoSugeridoDao: PrazoSugeridoDao,
    private val minutaDao: MinutaDao,
    private val repositorioCadastroOab: CadastroOabRepository,
    private val perfilPreferencesRepository: PerfilPreferencesRepository,
    private val aplicarPreferenciaSincronizacao: AplicarPreferenciaSincronizacaoUC,
    private val partesResolver: DjenPartesResolver? = null,
) : SincronizacaoRepository {

    override suspend fun nuvemAutorizada(): Boolean =
        perfilPreferencesRepository.preferencias.first().sincronizacaoNuvemAutorizada

    override suspend fun enviarTudo(userId: String): SincronizacaoNuvemResumo {
        val userRef = firestore.usuarioRef(userId)
        val processos = localProcessoRepository.observeProcessos().first()

        // Publicação sigilosa não sai do aparelho: o inteiro teor viaja no DTO,
        // e o que corre em segredo de justiça não tem por que estar na nuvem.
        val publicacoes = localPublicacaoRepository.observePublicacoes().first()
            .filterNot(PublicacaoEntity::isSigiloso)
        val idsPublicacoes = publicacoes.mapTo(mutableSetOf(), PublicacaoEntity::id)

        val movimentos = processos.flatMap { processo ->
            localProcessoRepository.getMovimentos(processo.numeroProcesso)
        }
        val participantes = localProcessoRepository.getTodosParticipantes()
        val clientes = clienteDao.observarTodos().first()
        val vinculos = clienteDao.observarVinculos().first()
        val prazos = prazoSugeridoDao.observeAll().first()
            .filter { it.publicacaoId in idsPublicacoes }
        val minutas = minutaDao.listarTodas()

        enviar(userRef.collection(COLECAO_PROCESSOS), processos, ProcessoEntity::numeroProcesso) { it.toFirestoreDto() }
        enviar(userRef.collection(COLECAO_PUBLICACOES), publicacoes, { it.id.toString() }) { it.toFirestoreDto() }
        enviar(userRef.collection(COLECAO_MOVIMENTOS), movimentos, MovimentoEntity::idLocal) { it.toFirestoreDto() }
        enviar(userRef.collection(COLECAO_PARTICIPANTES), participantes, ParticipanteEntity::idLocal) { it.toFirestoreDto() }
        enviar(userRef.collection(COLECAO_CLIENTES), clientes, ClienteEntity::id) { it.toFirestoreDto() }
        enviar(userRef.collection(COLECAO_VINCULOS), vinculos, { "${it.clienteId}_${it.numeroProcesso}" }) { it.toFirestoreDto() }
        enviar(userRef.collection(COLECAO_PRAZOS), prazos, { it.publicacaoId.toString() }) { it.toFirestoreDto() }
        enviar(userRef.collection(COLECAO_MINUTAS), minutas, MinutaEntity::sugestaoId) { it.toFirestoreDto() }

        return SincronizacaoNuvemResumo(
            processos = processos.size,
            publicacoes = publicacoes.size,
            movimentos = movimentos.size,
            participantes = participantes.size,
            clientes = clientes.size + vinculos.size,
            prazos = prazos.size,
            minutas = minutas.size,
        )
    }

    override suspend fun restaurarTudo(userId: String): SincronizacaoNuvemResumo {
        val userRef = firestore.usuarioRef(userId)

        val processos = baixar(userRef.collection(COLECAO_PROCESSOS), ProcessoFirestoreDto::class.java) { it.toEntity() }
        val publicacoes = baixar(userRef.collection(COLECAO_PUBLICACOES), PublicacaoFirestoreDto::class.java) { it.toEntity() }
        val movimentos = baixar(userRef.collection(COLECAO_MOVIMENTOS), MovimentoFirestoreDto::class.java) { it.toEntity() }
        val participantes = baixar(userRef.collection(COLECAO_PARTICIPANTES), ParticipanteFirestoreDto::class.java) { it.toEntity() }
        val clientes = baixar(userRef.collection(COLECAO_CLIENTES), ClienteFirestoreDto::class.java) { it.toEntity() }
        val vinculos = baixar(userRef.collection(COLECAO_VINCULOS), ClienteProcessoFirestoreDto::class.java) { it.toEntity() }
        val prazos = baixar(userRef.collection(COLECAO_PRAZOS), PrazoSugeridoFirestoreDto::class.java) { it.toEntity() }
        val minutas = baixar(userRef.collection(COLECAO_MINUTAS), MinutaFirestoreDto::class.java) { it.toEntity() }

        // Ordem obrigatória: processos e publicações primeiro, porque prazos e
        // vínculos têm chave estrangeira para elas.
        val processosRestaurados = mergeProcessos(processos)
        val publicacoesRestauradas = mergePublicacoes(publicacoes)
        val movimentosRestaurados = mergeMovimentos(movimentos)
        val participantesRestaurados = mergeParticipantes(participantes)
        val clientesRestaurados = mergeClientes(clientes, vinculos)
        val prazosRestaurados = mergePrazos(prazos)
        minutas.forEach { minutaDao.upsert(it) }

        partesResolver?.atualizarPartesDosProcessos(
            processos.map(ProcessoEntity::numeroProcesso).distinct(),
        )

        return SincronizacaoNuvemResumo(
            processos = processosRestaurados,
            publicacoes = publicacoesRestauradas,
            movimentos = movimentosRestaurados,
            participantes = participantesRestaurados,
            clientes = clientesRestaurados,
            prazos = prazosRestaurados,
            minutas = minutas.size,
        )
    }

    override suspend fun enviarPerfil(userId: String): Result<Unit> = runCatching {
        val cadastro = repositorioCadastroOab.cadastro.first()
        val preferencias = perfilPreferencesRepository.preferencias.first()

        val dto = PerfilFirestoreDto(
            nomeAdvogado = cadastro.nomeAdvogado,
            numeroOab = cadastro.numero,
            ufOab = cadastro.uf,
            tipoInscricao = cadastro.tipoInscricao,
            nomeEscritorio = cadastro.nomeEscritorio,
            areasAtuacao = cadastro.areasAtuacao,
            intervaloBuscaDias = preferencias.intervaloBuscaDias,
            sincronizacaoAutomatica = preferencias.sincronizacaoAutomatica,
            notificarPublicacoes = preferencias.notificarPublicacoes,
            notificarPrazosUrgentes = preferencias.notificarPrazosUrgentes,
            notificarMovimentacoes = preferencias.notificarMovimentacoes,
            tema = preferencias.tema.name,
            apenasPorNome = preferencias.apenasPorNome,
            atualizadoEm = Timestamp.now(),
        )

        firestore.usuarioRef(userId)
            .collection(COLECAO_PERFIL)
            .document(DOC_PERFIL)
            .set(dto, SetOptions.merge())
            .await()
    }

    override suspend fun restaurarPerfil(userId: String): Result<Unit> = runCatching {
        val snapshot = firestore.usuarioRef(userId)
            .collection(COLECAO_PERFIL)
            .document(DOC_PERFIL)
            .get()
            .await()

        val dto = snapshot.toObject(PerfilFirestoreDto::class.java) ?: return@runCatching

        repositorioCadastroOab.salvarCadastro(
            numero = dto.numeroOab,
            uf = dto.ufOab,
            nomeAdvogado = dto.nomeAdvogado,
            tipoInscricao = dto.tipoInscricao,
            nomeEscritorio = dto.nomeEscritorio,
            areasAtuacao = dto.areasAtuacao,
        )
        perfilPreferencesRepository.saveIntervaloBuscaDias(dto.intervaloBuscaDias)
        // Precisa reagendar junto: restaurar `false` aqui e deixar o agendamento
        // intacto fazia o próximo onCreate cancelar o job das 7h sem rastro.
        aplicarPreferenciaSincronizacao(dto.sincronizacaoAutomatica)
        perfilPreferencesRepository.saveNotificarPublicacoes(dto.notificarPublicacoes)
        perfilPreferencesRepository.saveNotificarPrazosUrgentes(dto.notificarPrazosUrgentes)
        perfilPreferencesRepository.saveNotificarMovimentacoes(dto.notificarMovimentacoes)
        perfilPreferencesRepository.saveTema(
            runCatching { com.obiterjus.ui.theme.TipoTema.valueOf(dto.tema) }
                .getOrDefault(com.obiterjus.ui.theme.TipoTema.SISTEMA)
        )
        perfilPreferencesRepository.saveApenasPorNome(dto.apenasPorNome)
        // A autorização de nuvem não vem do perfil remoto de propósito: é
        // consentimento e tem de ser dado neste aparelho.
    }

    /**
     * O Firestore não apaga subcoleção junto com o documento pai: apagar
     * `users/{uid}` deixaria o acervo inteiro órfão, visível no console e sem
     * dono que possa pedir sua exclusão. Daí a lista explícita — qualquer
     * coleção nova em [enviarTudo] tem de entrar aqui também.
     */
    override suspend fun excluirTudo(userId: String): Result<Unit> = runCatching {
        val userRef = firestore.usuarioRef(userId)
        COLECOES_DA_CONTA.forEach { nome -> apagarColecao(userRef.collection(nome)) }
        userRef.delete().await()
    }

    /**
     * Grava em lotes de [TAMANHO_LOTE]. Antes era um `await()` por documento —
     * milhares de idas e voltas numa rede móvel, e um erro no meio deixava a
     * nuvem parcialmente escrita sem nenhum sinal.
     */
    private suspend fun <T> enviar(
        colecao: CollectionReference,
        itens: List<T>,
        id: (T) -> String,
        dto: (T) -> Any,
    ) {
        itens.chunked(TAMANHO_LOTE).forEach { lote ->
            val batch = firestore.batch()
            lote.forEach { item ->
                batch.set(colecao.document(id(item)), dto(item), SetOptions.merge())
            }
            batch.commit().await()
        }
    }

    private suspend fun apagarColecao(colecao: CollectionReference) {
        val documentos = colecao.get().await().documents
        documentos.chunked(TAMANHO_LOTE).forEach { lote ->
            val batch = firestore.batch()
            lote.forEach { documento -> batch.delete(documento.reference) }
            batch.commit().await()
        }
    }

    private suspend fun <D : Any, E> baixar(
        colecao: CollectionReference,
        tipo: Class<D>,
        paraEntidade: (D) -> E?,
    ): List<E> = colecao.get().await().documents.mapNotNull { documento ->
        documento.toObject(tipo)?.let(paraEntidade)
    }

    private suspend fun mergeProcessos(processosNuvem: List<ProcessoEntity>): Int {
        val processosLocais = localProcessoRepository
            .getProcessos(processosNuvem.map(ProcessoEntity::numeroProcesso))
            .associateBy(ProcessoEntity::numeroProcesso)
        val processosParaSalvar = processosNuvem.filter { remoto ->
            val local = processosLocais[remoto.numeroProcesso]
            local == null || remoto.atualizadoEm > local.atualizadoEm
        }
        localProcessoRepository.upsertProcessos(processosParaSalvar)
        return processosParaSalvar.size
    }

    private suspend fun mergePublicacoes(publicacoesNuvem: List<PublicacaoEntity>): Int {
        val publicacoesLocais = localPublicacaoRepository
            .getPublicacoes(publicacoesNuvem.map(PublicacaoEntity::id))
            .associateBy(PublicacaoEntity::id)
        val publicacoesParaSalvar = publicacoesNuvem.filter { remoto ->
            val local = publicacoesLocais[remoto.id]
            local == null || remoto.atualizadoEm > local.atualizadoEm
        }
        localPublicacaoRepository.upsertPublicacoes(publicacoesParaSalvar)
        return publicacoesParaSalvar.size
    }

    private suspend fun mergeMovimentos(movimentosNuvem: List<MovimentoEntity>): Int {
        val processosExistentes = localProcessoRepository
            .getProcessos(movimentosNuvem.map(MovimentoEntity::numeroProcesso).distinct())
            .map(ProcessoEntity::numeroProcesso)
            .toSet()
        val movimentosExistentes = localProcessoRepository
            .getMovimentos(movimentosNuvem.map(MovimentoEntity::idLocal))
            .map(MovimentoEntity::idLocal)
            .toSet()
        val movimentosParaSalvar = movimentosNuvem.filter { movimento ->
            movimento.numeroProcesso in processosExistentes &&
                movimento.idLocal !in movimentosExistentes
        }
        localProcessoRepository.upsertMovimentos(movimentosParaSalvar)
        return movimentosParaSalvar.size
    }

    private suspend fun mergeParticipantes(participantesNuvem: List<ParticipanteEntity>): Int {
        val processosExistentes = localProcessoRepository
            .getProcessos(participantesNuvem.map(ParticipanteEntity::numeroProcesso).distinct())
            .map(ProcessoEntity::numeroProcesso)
            .toSet()
        val participantesPorProcesso = participantesNuvem
            .filter { participante -> participante.numeroProcesso in processosExistentes }
            .groupBy(ParticipanteEntity::numeroProcesso)

        participantesPorProcesso.forEach { (numeroProcesso, participantes) ->
            localProcessoRepository.replaceParticipantes(numeroProcesso, participantes)
        }
        return participantesPorProcesso.values.sumOf { it.size }
    }

    private suspend fun mergeClientes(
        clientesNuvem: List<ClienteEntity>,
        vinculosNuvem: List<ClienteProcessoEntity>,
    ): Int {
        // Mesma regra dos processos: a nuvem só sobrescreve o que for mais
        // recente, para não desfazer uma edição feita neste aparelho.
        val clientesLocais = clienteDao.observarTodos().first().associateBy(ClienteEntity::id)
        val clientesParaSalvar = clientesNuvem.filter { remoto ->
            val local = clientesLocais[remoto.id]
            local == null || remoto.atualizadoEm > local.atualizadoEm
        }
        clientesParaSalvar.forEach { clienteDao.upsert(it) }

        val clientesExistentes = clientesNuvem.mapTo(mutableSetOf(), ClienteEntity::id)
        val processosExistentes = localProcessoRepository
            .getProcessos(vinculosNuvem.map(ClienteProcessoEntity::numeroProcesso).distinct())
            .map(ProcessoEntity::numeroProcesso)
            .toSet()
        val vinculosValidos = vinculosNuvem.filter { vinculo ->
            vinculo.clienteId in clientesExistentes && vinculo.numeroProcesso in processosExistentes
        }
        vinculosValidos.forEach { clienteDao.vincular(it) }

        return clientesParaSalvar.size + vinculosValidos.size
    }

    /**
     * Só traz prazo que ainda não existe aqui. A tabela não tem carimbo de
     * atualização, e o `insert` é REPLACE — sobrescrever apagaria um "cumprido"
     * marcado neste aparelho.
     */
    private suspend fun mergePrazos(prazosNuvem: List<PrazoSugeridoEntity>): Int {
        val publicacoesExistentes = localPublicacaoRepository
            .getPublicacoes(prazosNuvem.map(PrazoSugeridoEntity::publicacaoId).distinct())
            .map(PublicacaoEntity::id)
            .toSet()
        val prazosLocais = prazoSugeridoDao.observeAll().first()
            .mapTo(mutableSetOf(), PrazoSugeridoEntity::publicacaoId)
        val prazosValidos = prazosNuvem.filter {
            it.publicacaoId in publicacoesExistentes && it.publicacaoId !in prazosLocais
        }
        prazosValidos.forEach { prazoSugeridoDao.insert(it) }
        return prazosValidos.size
    }

    private fun FirebaseFirestore.usuarioRef(userId: String): DocumentReference =
        collection(COLECAO_USUARIOS).document(userId)

    private companion object {
        const val COLECAO_USUARIOS = "users"
        const val COLECAO_PROCESSOS = "processos"
        const val COLECAO_PUBLICACOES = "publicacoes"
        const val COLECAO_MOVIMENTOS = "movimentos"
        const val COLECAO_PARTICIPANTES = "participantes"
        const val COLECAO_CLIENTES = "clientes"
        const val COLECAO_VINCULOS = "clientes_processos"
        const val COLECAO_PRAZOS = "prazos"
        const val COLECAO_MINUTAS = "minutas"
        const val COLECAO_PERFIL = "perfil"
        const val DOC_PERFIL = "profile"

        /** Tudo o que pende de `users/{uid}` e vai embora com a conta. */
        val COLECOES_DA_CONTA = listOf(
            COLECAO_PROCESSOS,
            COLECAO_PUBLICACOES,
            COLECAO_MOVIMENTOS,
            COLECAO_PARTICIPANTES,
            COLECAO_CLIENTES,
            COLECAO_VINCULOS,
            COLECAO_PRAZOS,
            COLECAO_MINUTAS,
            COLECAO_PERFIL,
        )

        /** Teto de operações por WriteBatch no Firestore. */
        const val TAMANHO_LOTE = 500
    }
}
