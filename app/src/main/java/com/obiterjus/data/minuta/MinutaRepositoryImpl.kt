package com.obiterjus.data.minuta

import com.obiterjus.data.minuta.local.MinutaDao
import com.obiterjus.data.minuta.local.MinutaEntity
import com.obiterjus.data.minuta.remote.ObiterMinutaDataSource
import com.obiterjus.data.minuta.remote.dto.MinutaSugestaoDto
import com.obiterjus.data.minuta.remote.dto.SugestaoMinutaRequestDto
import com.obiterjus.domain.model.EstruturaPeca
import com.obiterjus.domain.model.ExtracaoProgresso
import com.obiterjus.domain.model.MinutaPackage
import com.obiterjus.domain.model.MinutaSugestao
import com.obiterjus.domain.model.TipoMinuta
import com.obiterjus.domain.repository.MinutaRepository
import java.io.InputStream
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class MinutaRepositoryImpl(
    private val dataSource: ObiterMinutaDataSource,
    private val minutaDao: MinutaDao,
    /** Firebase ID token do usuário autenticado; a API valida a audience. */
    private val tokenProvider: suspend () -> String?,
) : MinutaRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override fun extrairPeca(
        pdf: InputStream,
        nomeArquivo: String,
        tipoDeclarado: String,
        numeroProcesso: String?,
        vara: String?,
    ): Flow<ExtracaoProgresso> = flow {
        val autorizacao = autorizacao()
        val bytes = pdf.use { it.readBytes() }
        val filePart = MultipartBody.Part.createFormData(
            name = "file",
            filename = nomeArquivo,
            body = bytes.toRequestBody(MEDIA_PDF.toMediaType()),
        )

        val resposta = dataSource.ingerir(
            autorizacao = autorizacao,
            file = filePart,
            tipoDeclarado = tipoDeclarado.toFormBody(),
            numeroProcesso = numeroProcesso?.toFormBody(),
            vara = vara?.toFormBody(),
            origem = ORIGEM.toFormBody(),
        )
        emit(ExtracaoProgresso(jobId = resposta.jobId, status = resposta.status))

        // Polling: o trabalho pesado roda no worker Celery da API.
        repeat(MAX_TENTATIVAS) {
            delay(INTERVALO_POLLING_MS)
            val status = dataSource.consultarStatus(autorizacao, resposta.jobId)
            val progresso = ExtracaoProgresso(
                jobId = status.jobId,
                status = status.status,
                stage = status.stage,
                progressoPct = status.progressPct,
            )
            emit(progresso)
            if (progresso.concluido) return@flow
            if (progresso.falhou) {
                throw IllegalStateException("A extração da peça falhou no servidor.")
            }
        }
        throw IllegalStateException("Tempo esgotado aguardando a extração da peça.")
    }.flowOn(Dispatchers.IO)

    override suspend fun obterResultado(jobId: String): MinutaPackage {
        // O status pode virar "completed" um instante antes de o worker gravar o
        // resultado no Redis; um 404 aqui é transitório e vale reter algumas vezes.
        repeat(TENTATIVAS_RESULTADO) { tentativa ->
            try {
                return dataSource.obterResultado(autorizacao(), jobId).toDomain()
            } catch (e: HttpException) {
                if (e.code() != 404 || tentativa == TENTATIVAS_RESULTADO - 1) throw e
                delay(INTERVALO_RESULTADO_MS)
            }
        }
        error("Resultado da extração indisponível.")
    }

    override suspend fun sugerirMinuta(
        tipoMinuta: TipoMinuta,
        tipoPecaOrigem: String,
        textoOrigem: String,
        estrutura: EstruturaPeca,
        numeroProcesso: String?,
        vara: String?,
        orientacoes: String?,
    ): MinutaSugestao {
        val dto = dataSource.sugerirMinuta(
            autorizacao(),
            SugestaoMinutaRequestDto(
                tipoMinuta = tipoMinuta.codigo,
                tipoPecaOrigem = tipoPecaOrigem,
                textoOrigem = textoOrigem,
                estrutura = estrutura.toDto(),
                numeroProcesso = numeroProcesso,
                vara = vara,
                orientacoes = orientacoes,
            ),
        )
        val geradoEm = runCatching { Instant.parse(dto.geradoEm) }.getOrDefault(Instant.now())

        minutaDao.upsert(
            MinutaEntity(
                sugestaoId = dto.sugestaoId,
                numeroProcesso = numeroProcesso,
                tipoMinuta = dto.tipoMinuta,
                titulo = dto.titulo,
                confiancaGeral = dto.qualidade.confiancaGeral,
                requerRevisao = dto.qualidade.requerRevisao,
                geradoEm = geradoEm,
                payloadJson = json.encodeToString(MinutaSugestaoDto.serializer(), dto),
            ),
        )
        return dto.toDomain(numeroProcesso = numeroProcesso, geradoEmInstant = geradoEm)
    }

    override fun observarMinutasDoProcesso(numeroProcesso: String): Flow<List<MinutaSugestao>> =
        minutaDao.observarPorProcesso(numeroProcesso)
            .map { entidades -> entidades.mapNotNull { it.toDomainOuNull() } }

    override suspend fun obterMinuta(sugestaoId: String): MinutaSugestao? =
        minutaDao.buscarPorId(sugestaoId)?.toDomainOuNull()

    override suspend fun excluirMinuta(sugestaoId: String) {
        minutaDao.excluir(sugestaoId)
    }

    /** Reconstrói a sugestão a partir do payload; ignora linhas corrompidas. */
    private fun MinutaEntity.toDomainOuNull(): MinutaSugestao? = runCatching {
        json.decodeFromString(MinutaSugestaoDto.serializer(), payloadJson)
            .toDomain(numeroProcesso = numeroProcesso, geradoEmInstant = geradoEm)
    }.getOrNull()

    private fun String.toFormBody(): okhttp3.RequestBody = toRequestBody(MEDIA_TEXT.toMediaType())

    private suspend fun autorizacao(): String {
        val token = tokenProvider()
            ?: throw IllegalStateException(
                "Sessão expirada. Entre novamente para usar o gerador de minutas.",
            )
        return "Bearer $token"
    }

    private companion object {
        const val MEDIA_PDF = "application/pdf"
        const val MEDIA_TEXT = "text/plain"
        const val ORIGEM = "app-android"
        const val INTERVALO_POLLING_MS = 2_500L
        const val MAX_TENTATIVAS = 60 // ~2,5 min de teto
        const val TENTATIVAS_RESULTADO = 3
        const val INTERVALO_RESULTADO_MS = 1_000L
    }
}
