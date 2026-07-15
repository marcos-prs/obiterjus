package com.obiterjus.data.sincronizacao

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.obiterjus.data.datajud.local.MovimentoEntity
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.djen.DjenPartesResolver
import com.obiterjus.data.processo.local.LocalProcessoRepository
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.data.publicacao.local.LocalPublicacaoRepository
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.domain.model.SincronizacaoNuvemResumo
import com.obiterjus.domain.repository.CadastroOabRepository
import com.obiterjus.domain.repository.SincronizacaoRepository
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class FirestoreSincronizacaoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localProcessoRepository: LocalProcessoRepository,
    private val localPublicacaoRepository: LocalPublicacaoRepository,
    private val repositorioCadastroOab: CadastroOabRepository,
    private val perfilPreferencesRepository: PerfilPreferencesRepository,
    private val partesResolver: DjenPartesResolver? = null,
) : SincronizacaoRepository {

    override suspend fun enviarTudo(userId: String): SincronizacaoNuvemResumo {
        val userRef = firestore.usuarioRef(userId)
        val processos = localProcessoRepository.observeProcessos().first()
        val publicacoes = localPublicacaoRepository.observePublicacoes().first()
        val movimentos = processos.flatMap { processo ->
            localProcessoRepository.getMovimentos(processo.numeroProcesso)
        }
        val participantes = localProcessoRepository.getTodosParticipantes()

        processos.forEach { processo ->
            userRef.collection(COLECAO_PROCESSOS)
                .document(processo.numeroProcesso)
                .set(processo.toFirestoreDto(), SetOptions.merge())
                .await()
        }

        publicacoes.forEach { publicacao ->
            userRef.collection(COLECAO_PUBLICACOES)
                .document(publicacao.id.toString())
                .set(publicacao.toFirestoreDto(), SetOptions.merge())
                .await()
        }

        movimentos.forEach { movimento ->
            userRef.collection(COLECAO_MOVIMENTOS)
                .document(movimento.idLocal)
                .set(movimento.toFirestoreDto(), SetOptions.merge())
                .await()
        }

        participantes.forEach { participante ->
            userRef.collection(COLECAO_PARTICIPANTES)
                .document(participante.idLocal)
                .set(participante.toFirestoreDto(), SetOptions.merge())
                .await()
        }

        return SincronizacaoNuvemResumo(
            processos = processos.size,
            publicacoes = publicacoes.size,
            movimentos = movimentos.size,
            participantes = participantes.size,
        )
    }

    override suspend fun restaurarTudo(userId: String): SincronizacaoNuvemResumo {
        val userRef = firestore.usuarioRef(userId)
        val processos = userRef.collection(COLECAO_PROCESSOS)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(ProcessoFirestoreDto::class.java)?.toEntity() }
        val publicacoes = userRef.collection(COLECAO_PUBLICACOES)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(PublicacaoFirestoreDto::class.java)?.toEntity() }
        val movimentos = userRef.collection(COLECAO_MOVIMENTOS)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(MovimentoFirestoreDto::class.java)?.toEntity() }
        val participantes = userRef.collection(COLECAO_PARTICIPANTES)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(ParticipanteFirestoreDto::class.java)?.toEntity() }

        val processosRestaurados = mergeProcessos(processos)
        val publicacoesRestauradas = mergePublicacoes(publicacoes)
        val movimentosRestaurados = mergeMovimentos(movimentos)
        val participantesRestaurados = mergeParticipantes(participantes)
        partesResolver?.atualizarPartesDosProcessos(
            processos.map(ProcessoEntity::numeroProcesso).distinct(),
        )

        return SincronizacaoNuvemResumo(
            processos = processosRestaurados,
            publicacoes = publicacoesRestauradas,
            movimentos = movimentosRestaurados,
            participantes = participantesRestaurados,
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
        perfilPreferencesRepository.saveSincronizacaoAutomatica(dto.sincronizacaoAutomatica)
        perfilPreferencesRepository.saveNotificarPublicacoes(dto.notificarPublicacoes)
        perfilPreferencesRepository.saveNotificarPrazosUrgentes(dto.notificarPrazosUrgentes)
        perfilPreferencesRepository.saveNotificarMovimentacoes(dto.notificarMovimentacoes)
        perfilPreferencesRepository.saveTema(
            runCatching { com.obiterjus.ui.theme.TipoTema.valueOf(dto.tema) }
                .getOrDefault(com.obiterjus.ui.theme.TipoTema.SISTEMA)
        )
        perfilPreferencesRepository.saveApenasPorNome(dto.apenasPorNome)
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

    private fun FirebaseFirestore.usuarioRef(userId: String): DocumentReference =
        collection(COLECAO_USUARIOS).document(userId)

    private companion object {
        const val COLECAO_USUARIOS = "users"
        const val COLECAO_PROCESSOS = "processos"
        const val COLECAO_PUBLICACOES = "publicacoes"
        const val COLECAO_MOVIMENTOS = "movimentos"
        const val COLECAO_PARTICIPANTES = "participantes"
        const val COLECAO_PERFIL = "perfil"
        const val DOC_PERFIL = "profile"
    }
}
