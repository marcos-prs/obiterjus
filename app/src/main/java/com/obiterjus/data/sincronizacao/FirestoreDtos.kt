package com.obiterjus.data.sincronizacao

import com.google.firebase.Timestamp
import com.obiterjus.data.datajud.local.MovimentoEntity
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.domain.model.ProcessoSyncStatus
import java.time.Instant
import java.time.LocalDate

data class PublicacaoFirestoreDto(
    val id: Long = 0,
    val hash: String? = null,
    val numeroProcesso: String? = null,
    val participantesJson: String? = null,
    val prazoQuantidade: Int? = null,
    val prazoUnidade: String? = null,
    val prazoDiasUteis: Boolean = false,
    val prazoTexto: String? = null,
    val prazoDataLimite: String? = null,
    val dataDisponibilizacao: String? = null,
    val tribunal: String? = null,
    val tipoComunicacao: String? = null,
    val nomeOrgao: String? = null,
    val idOrgao: Long? = null,
    val textoRaw: String? = null,
    val textoLimpo: String? = null,
    val textoPossuiHtml: Boolean = false,
    val textoPossuiErroTemplate: Boolean = false,
    val isSigiloso: Boolean = false,
    val ativo: Boolean = true,
    val fonte: String = "",
    val capturadoEm: Timestamp? = null,
    val atualizadoEm: Timestamp? = null,
)

fun PublicacaoEntity.toFirestoreDto() = PublicacaoFirestoreDto(
    id = id,
    hash = hash,
    numeroProcesso = numeroProcesso,
    participantesJson = participantesJson,
    prazoQuantidade = prazoQuantidade,
    prazoUnidade = prazoUnidade,
    prazoDiasUteis = prazoDiasUteis,
    prazoTexto = prazoTexto,
    prazoDataLimite = prazoDataLimite?.toString(),
    dataDisponibilizacao = dataDisponibilizacao?.toString(),
    tribunal = tribunal,
    tipoComunicacao = tipoComunicacao,
    nomeOrgao = nomeOrgao,
    idOrgao = idOrgao,
    textoRaw = textoRaw,
    textoLimpo = textoLimpo,
    textoPossuiHtml = textoPossuiHtml,
    textoPossuiErroTemplate = textoPossuiErroTemplate,
    isSigiloso = isSigiloso,
    ativo = ativo,
    fonte = fonte,
    capturadoEm = capturadoEm.toTimestamp(),
    atualizadoEm = atualizadoEm.toTimestamp(),
)

fun PublicacaoFirestoreDto.toEntity(): PublicacaoEntity? =
    if (id == 0L || capturadoEm == null || atualizadoEm == null) {
        null
    } else {
        PublicacaoEntity(
            id = id,
            hash = hash,
            numeroProcesso = numeroProcesso,
            participantesJson = participantesJson,
            prazoQuantidade = prazoQuantidade,
            prazoUnidade = prazoUnidade,
            prazoDiasUteis = prazoDiasUteis,
            prazoTexto = prazoTexto,
            prazoDataLimite = prazoDataLimite?.let { raw ->
                runCatching { LocalDate.parse(raw) }.getOrNull()
            },
            dataDisponibilizacao = dataDisponibilizacao?.let { raw ->
                runCatching { LocalDate.parse(raw) }.getOrNull()
            },
            tribunal = tribunal,
            tipoComunicacao = tipoComunicacao,
            nomeOrgao = nomeOrgao,
            idOrgao = idOrgao,
            textoRaw = textoRaw,
            textoLimpo = textoLimpo,
            textoPossuiHtml = textoPossuiHtml,
            textoPossuiErroTemplate = textoPossuiErroTemplate,
            isSigiloso = isSigiloso,
            ativo = ativo,
            fonte = fonte.ifBlank { FONTE_NUVEM },
            capturadoEm = capturadoEm.toJavaInstant(),
            atualizadoEm = atualizadoEm.toJavaInstant(),
        )
    }

data class ProcessoFirestoreDto(
    val numeroProcesso: String = "",
    val tribunal: String? = null,
    val grau: String? = null,
    val classeCodigo: Int? = null,
    val classeNome: String? = null,
    val assuntosJson: String? = null,
    val orgaoJulgadorCodigo: Int? = null,
    val orgaoJulgadorNome: String? = null,
    val nivelSigilo: Int? = null,
    val dataAjuizamento: Timestamp? = null,
    val syncStatus: String = "",
    val capturadoEm: Timestamp? = null,
    val atualizadoEm: Timestamp? = null,
    val dataJudTentativasRestantes: Int = 0,
    val natureza: String? = null,
)

fun ProcessoEntity.toFirestoreDto() = ProcessoFirestoreDto(
    numeroProcesso = numeroProcesso,
    tribunal = tribunal,
    grau = grau,
    classeCodigo = classeCodigo,
    classeNome = classeNome,
    assuntosJson = assuntosJson,
    orgaoJulgadorCodigo = orgaoJulgadorCodigo,
    orgaoJulgadorNome = orgaoJulgadorNome,
    nivelSigilo = nivelSigilo,
    dataAjuizamento = dataAjuizamento?.toTimestamp(),
    syncStatus = syncStatus.name,
    capturadoEm = capturadoEm.toTimestamp(),
    atualizadoEm = atualizadoEm.toTimestamp(),
    dataJudTentativasRestantes = dataJudTentativasRestantes,
    natureza = natureza,
)

fun ProcessoFirestoreDto.toEntity(): ProcessoEntity? =
    if (numeroProcesso.isBlank() || capturadoEm == null || atualizadoEm == null) {
        null
    } else {
        ProcessoEntity(
            numeroProcesso = numeroProcesso,
            tribunal = tribunal,
            grau = grau,
            classeCodigo = classeCodigo,
            classeNome = classeNome,
            assuntosJson = assuntosJson,
            orgaoJulgadorCodigo = orgaoJulgadorCodigo,
            orgaoJulgadorNome = orgaoJulgadorNome,
            nivelSigilo = nivelSigilo,
            dataAjuizamento = dataAjuizamento?.toJavaInstant(),
            syncStatus = runCatching { ProcessoSyncStatus.valueOf(syncStatus) }
                .getOrDefault(ProcessoSyncStatus.FAILED),
            capturadoEm = capturadoEm.toJavaInstant(),
            atualizadoEm = atualizadoEm.toJavaInstant(),
            dataJudTentativasRestantes = dataJudTentativasRestantes,
            natureza = natureza,
        )
    }

data class MovimentoFirestoreDto(
    val idLocal: String = "",
    val numeroProcesso: String = "",
    val codigo: Int? = null,
    val nome: String? = null,
    val dataHora: Timestamp? = null,
    val complementosJson: String? = null,
)

fun MovimentoEntity.toFirestoreDto() = MovimentoFirestoreDto(
    idLocal = idLocal,
    numeroProcesso = numeroProcesso,
    codigo = codigo,
    nome = nome,
    dataHora = dataHora?.toTimestamp(),
    complementosJson = complementosJson,
)

fun MovimentoFirestoreDto.toEntity(): MovimentoEntity? =
    if (idLocal.isBlank() || numeroProcesso.isBlank()) {
        null
    } else {
        MovimentoEntity(
            idLocal = idLocal,
            numeroProcesso = numeroProcesso,
            codigo = codigo,
            nome = nome,
            dataHora = dataHora?.toJavaInstant(),
            complementosJson = complementosJson,
        )
    }

data class ParticipanteFirestoreDto(
    val idLocal: String = "",
    val numeroProcesso: String = "",
    val polo: String? = null,
    val nome: String? = null,
    val tipoPessoa: String? = null,
    val tipoParticipacao: String? = null,
    val ehCliente: Boolean = false,
)

fun ParticipanteEntity.toFirestoreDto() = ParticipanteFirestoreDto(
    idLocal = idLocal,
    numeroProcesso = numeroProcesso,
    polo = polo,
    nome = nome,
    tipoPessoa = tipoPessoa,
    tipoParticipacao = tipoParticipacao,
    ehCliente = ehCliente,
)

fun ParticipanteFirestoreDto.toEntity(): ParticipanteEntity? =
    if (idLocal.isBlank() || numeroProcesso.isBlank()) {
        null
    } else {
        ParticipanteEntity(
            idLocal = idLocal,
            numeroProcesso = numeroProcesso,
            polo = polo,
            nome = nome,
            tipoPessoa = tipoPessoa,
            tipoParticipacao = tipoParticipacao,
            ehCliente = ehCliente,
        )
    }

data class PerfilFirestoreDto(
    val nomeAdvogado: String = "",
    val numeroOab: String = "",
    val ufOab: String = "",
    val tipoInscricao: String = "",
    val nomeEscritorio: String = "",
    val areasAtuacao: List<String> = emptyList(),
    val intervaloBuscaDias: Int = 7,
    val sincronizacaoAutomatica: Boolean = true,
    val notificarPublicacoes: Boolean = true,
    val notificarPrazosUrgentes: Boolean = true,
    val notificarMovimentacoes: Boolean = true,
    val tema: String = "",
    val apenasPorNome: Boolean = false,
    val atualizadoEm: Timestamp? = null,
)

private fun Instant.toTimestamp(): Timestamp =
    Timestamp(epochSecond, (nano / FIRESTORE_NANOS_STEP) * FIRESTORE_NANOS_STEP)

private fun Timestamp.toJavaInstant(): Instant =
    Instant.ofEpochSecond(seconds, nanoseconds.toLong())

private const val FIRESTORE_NANOS_STEP = 1000
private const val FONTE_NUVEM = "NUVEM"
