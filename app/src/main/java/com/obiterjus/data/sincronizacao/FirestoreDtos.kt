package com.obiterjus.data.sincronizacao

import com.google.firebase.Timestamp
import com.obiterjus.data.datajud.local.MovimentoEntity
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.data.cliente.local.ClienteEntity
import com.obiterjus.data.cliente.local.ClienteProcessoEntity
import com.obiterjus.data.cliente.local.RepresentanteLegalEmbutido
import com.obiterjus.data.minuta.local.MinutaEntity
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.domain.model.ConfiancaMatch
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
    val prazoConfianca: String? = null,
    val confiancaMatch: String = ConfiancaMatch.ALTA.name,
    val duplicataDe: Long? = null,
    val totalDuplicatas: Int = 0,
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
    prazoConfianca = prazoConfianca,
    confiancaMatch = confiancaMatch.name,
    duplicataDe = duplicataDe,
    totalDuplicatas = totalDuplicatas,
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
            prazoConfianca = prazoConfianca,
            confiancaMatch = runCatching { ConfiancaMatch.valueOf(confiancaMatch) }
                .getOrDefault(ConfiancaMatch.ALTA),
            duplicataDe = duplicataDe,
            totalDuplicatas = totalDuplicatas,
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
    // Campos expandidos. Ficaram de fora até aqui, e como o restore faz UPDATE
    // de todas as colunas, um documento mais recente apagava os 14 de uma vez.
    val dataDistribuicao: Timestamp? = null,
    val comarcaSecao: String? = null,
    val juizo: String? = null,
    val prioridadeTramitacao: String? = null,
    val gratuidadeJustica: String? = null,
    val valorCausa: Double? = null,
    val faseProcessual: String? = null,
    val situacaoAtual: String? = null,
    val tutelaAntecipadaLiminar: String? = null,
    val advogadosAtivo: String? = null,
    val advogadosPassivo: String? = null,
    val defensoriaPublica: String? = null,
    val ministerioPublico: String? = null,
    val terceirosAuxiliares: String? = null,
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
    dataDistribuicao = dataDistribuicao?.toTimestamp(),
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
            dataDistribuicao = dataDistribuicao?.toJavaInstant(),
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
    // Qualificação da parte: também ficava de fora e era zerada no restore.
    val cpfCnpj: String? = null,
    val estadoCivil: String? = null,
    val profissao: String? = null,
    val endereco: String? = null,
    val contatos: String? = null,
    val cep: String? = null,
    val logradouro: String? = null,
    val numeroEndereco: String? = null,
    val telefone: String? = null,
    val email: String? = null,
)

fun ParticipanteEntity.toFirestoreDto() = ParticipanteFirestoreDto(
    idLocal = idLocal,
    numeroProcesso = numeroProcesso,
    polo = polo,
    nome = nome,
    tipoPessoa = tipoPessoa,
    tipoParticipacao = tipoParticipacao,
    ehCliente = ehCliente,
    cpfCnpj = cpfCnpj,
    estadoCivil = estadoCivil,
    profissao = profissao,
    endereco = endereco,
    contatos = contatos,
    cep = cep,
    logradouro = logradouro,
    numeroEndereco = numeroEndereco,
    telefone = telefone,
    email = email,
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
            cpfCnpj = cpfCnpj,
            estadoCivil = estadoCivil,
            profissao = profissao,
            endereco = endereco,
            contatos = contatos,
            cep = cep,
            logradouro = logradouro,
            numeroEndereco = numeroEndereco,
            telefone = telefone,
            email = email,
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

/* ---------- Clientes ---------- */

data class RepresentanteFirestoreDto(
    val nome: String? = null,
    val documento: String? = null,
    val nacionalidade: String? = null,
    val estadoCivil: String? = null,
    val profissao: String? = null,
    val cargo: String? = null,
)

data class ClienteFirestoreDto(
    val id: String = "",
    val tipoPessoa: String = "",
    val nome: String = "",
    val nomeNormalizado: String = "",
    val documento: String? = null,
    val documentoNormalizado: String? = null,
    val nacionalidade: String? = null,
    val estadoCivil: String? = null,
    val profissao: String? = null,
    val cep: String? = null,
    val logradouro: String? = null,
    val numeroEndereco: String? = null,
    val complemento: String? = null,
    val bairro: String? = null,
    val municipio: String? = null,
    val uf: String? = null,
    val telefone: String? = null,
    val email: String? = null,
    val observacoes: String? = null,
    val representante: RepresentanteFirestoreDto? = null,
    val criadoEm: Timestamp? = null,
    val atualizadoEm: Timestamp? = null,
)

fun ClienteEntity.toFirestoreDto() = ClienteFirestoreDto(
    id = id,
    tipoPessoa = tipoPessoa,
    nome = nome,
    nomeNormalizado = nomeNormalizado,
    documento = documento,
    documentoNormalizado = documentoNormalizado,
    nacionalidade = nacionalidade,
    estadoCivil = estadoCivil,
    profissao = profissao,
    cep = cep,
    logradouro = logradouro,
    numeroEndereco = numeroEndereco,
    complemento = complemento,
    bairro = bairro,
    municipio = municipio,
    uf = uf,
    telefone = telefone,
    email = email,
    observacoes = observacoes,
    representante = representante?.let {
        RepresentanteFirestoreDto(
            nome = it.nome,
            documento = it.documento,
            nacionalidade = it.nacionalidade,
            estadoCivil = it.estadoCivil,
            profissao = it.profissao,
            cargo = it.cargo,
        )
    },
    criadoEm = criadoEm.toTimestamp(),
    atualizadoEm = atualizadoEm.toTimestamp(),
)

fun ClienteFirestoreDto.toEntity(): ClienteEntity? =
    if (id.isBlank() || nome.isBlank() || criadoEm == null || atualizadoEm == null) {
        null
    } else {
        ClienteEntity(
            id = id,
            tipoPessoa = tipoPessoa,
            nome = nome,
            nomeNormalizado = nomeNormalizado,
            documento = documento,
            documentoNormalizado = documentoNormalizado,
            nacionalidade = nacionalidade,
            estadoCivil = estadoCivil,
            profissao = profissao,
            cep = cep,
            logradouro = logradouro,
            numeroEndereco = numeroEndereco,
            complemento = complemento,
            bairro = bairro,
            municipio = municipio,
            uf = uf,
            telefone = telefone,
            email = email,
            observacoes = observacoes,
            representante = representante?.let {
                RepresentanteLegalEmbutido(
                    nome = it.nome,
                    documento = it.documento,
                    nacionalidade = it.nacionalidade,
                    estadoCivil = it.estadoCivil,
                    profissao = it.profissao,
                    cargo = it.cargo,
                )
            },
            criadoEm = criadoEm.toJavaInstant(),
            atualizadoEm = atualizadoEm.toJavaInstant(),
        )
    }

data class ClienteProcessoFirestoreDto(
    val clienteId: String = "",
    val numeroProcesso: String = "",
    val participanteIdLocal: String? = null,
    val vinculadoEm: Timestamp? = null,
)

fun ClienteProcessoEntity.toFirestoreDto() = ClienteProcessoFirestoreDto(
    clienteId = clienteId,
    numeroProcesso = numeroProcesso,
    participanteIdLocal = participanteIdLocal,
    vinculadoEm = vinculadoEm.toTimestamp(),
)

fun ClienteProcessoFirestoreDto.toEntity(): ClienteProcessoEntity? =
    if (clienteId.isBlank() || numeroProcesso.isBlank() || vinculadoEm == null) {
        null
    } else {
        ClienteProcessoEntity(
            clienteId = clienteId,
            numeroProcesso = numeroProcesso,
            participanteIdLocal = participanteIdLocal,
            vinculadoEm = vinculadoEm.toJavaInstant(),
        )
    }

/* ---------- Prazos e minutas ---------- */

/**
 * O `id` da entidade é autoGenerate e não é estável entre aparelhos; o
 * documento é identificado por `publicacaoId`, que é a chave real (índice
 * único na tabela).
 */
data class PrazoSugeridoFirestoreDto(
    val publicacaoId: Long = 0,
    val quantidade: Int = 0,
    val unidade: String = "",
    val diasUteis: Boolean = false,
    val textoOriginal: String = "",
    val dataLimite: String? = null,
    val isConfirmado: Boolean = false,
    val idExternoCalendario: String? = null,
    val provedorCalendario: String? = null,
    val isCumprido: Boolean = false,
)

fun PrazoSugeridoEntity.toFirestoreDto() = PrazoSugeridoFirestoreDto(
    publicacaoId = publicacaoId,
    quantidade = quantidade,
    unidade = unidade,
    diasUteis = diasUteis,
    textoOriginal = textoOriginal,
    dataLimite = dataLimite?.toString(),
    isConfirmado = isConfirmado,
    idExternoCalendario = idExternoCalendario,
    provedorCalendario = provedorCalendario,
    isCumprido = isCumprido,
)

fun PrazoSugeridoFirestoreDto.toEntity(): PrazoSugeridoEntity? =
    if (publicacaoId == 0L) {
        null
    } else {
        PrazoSugeridoEntity(
            publicacaoId = publicacaoId,
            quantidade = quantidade,
            unidade = unidade,
            diasUteis = diasUteis,
            textoOriginal = textoOriginal,
            dataLimite = dataLimite?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            isConfirmado = isConfirmado,
            idExternoCalendario = idExternoCalendario,
            provedorCalendario = provedorCalendario,
            isCumprido = isCumprido,
        )
    }

data class MinutaFirestoreDto(
    val sugestaoId: String = "",
    val numeroProcesso: String? = null,
    val tipoMinuta: String = "",
    val titulo: String = "",
    val confiancaGeral: Double = 0.0,
    val requerRevisao: Boolean = true,
    val geradoEm: Timestamp? = null,
    val payloadJson: String = "",
)

fun MinutaEntity.toFirestoreDto() = MinutaFirestoreDto(
    sugestaoId = sugestaoId,
    numeroProcesso = numeroProcesso,
    tipoMinuta = tipoMinuta,
    titulo = titulo,
    confiancaGeral = confiancaGeral,
    requerRevisao = requerRevisao,
    geradoEm = geradoEm.toTimestamp(),
    payloadJson = payloadJson,
)

fun MinutaFirestoreDto.toEntity(): MinutaEntity? =
    if (sugestaoId.isBlank() || geradoEm == null) {
        null
    } else {
        MinutaEntity(
            sugestaoId = sugestaoId,
            numeroProcesso = numeroProcesso,
            tipoMinuta = tipoMinuta,
            titulo = titulo,
            confiancaGeral = confiancaGeral,
            requerRevisao = requerRevisao,
            geradoEm = geradoEm.toJavaInstant(),
            payloadJson = payloadJson,
        )
    }

private fun Instant.toTimestamp(): Timestamp =
    Timestamp(epochSecond, (nano / FIRESTORE_NANOS_STEP) * FIRESTORE_NANOS_STEP)

private fun Timestamp.toJavaInstant(): Instant =
    Instant.ofEpochSecond(seconds, nanoseconds.toLong())

private const val FIRESTORE_NANOS_STEP = 1000
private const val FONTE_NUVEM = "NUVEM"
