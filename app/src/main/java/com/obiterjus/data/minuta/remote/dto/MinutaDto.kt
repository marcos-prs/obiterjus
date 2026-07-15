package com.obiterjus.data.minuta.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ---------- Ingestão e acompanhamento de job ---------- */

@Serializable
data class IngestRespostaDto(
    @SerialName("job_id") val jobId: String,
    val status: String,
    @SerialName("estimated_seconds") val estimatedSeconds: Int = 30,
)

@Serializable
data class StatusJobDto(
    @SerialName("job_id") val jobId: String,
    val status: String,
    val stage: String? = null,
    @SerialName("progress_pct") val progressPct: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)

/* ---------- MinutaPackage (resultado da extração) ---------- */

@Serializable
data class CampoParteDto(
    val valor: String,
    val confianca: Double,
    @SerialName("origem_trecho") val origemTrecho: String? = null,
)

@Serializable
data class PartesDto(
    val requerente: CampoParteDto? = null,
    val requerido: CampoParteDto? = null,
)

@Serializable
data class PreliminarDto(
    val titulo: String,
    val texto: String,
    val confianca: Double,
    @SerialName("origem_trecho") val origemTrecho: String? = null,
)

@Serializable
data class FatoDto(
    val sequencia: Int,
    val texto: String,
    val confianca: Double,
    @SerialName("origem_trecho") val origemTrecho: String? = null,
)

@Serializable
data class TeseDto(
    val titulo: String,
    @SerialName("fundamento_legal") val fundamentoLegal: List<String> = emptyList(),
    val texto: String,
    val confianca: Double,
    @SerialName("origem_trecho") val origemTrecho: String? = null,
)

@Serializable
data class PedidoDto(
    val tipo: String,
    val texto: String,
    val confianca: Double,
    @SerialName("origem_trecho") val origemTrecho: String? = null,
)

@Serializable
data class ProvaDto(
    val tipo: String,
    val descricao: String,
    @SerialName("referencia_doc") val referenciaDoc: String? = null,
    val confianca: Double,
    @SerialName("origem_trecho") val origemTrecho: String? = null,
)

@Serializable
data class EstruturaPecaDto(
    val partes: PartesDto = PartesDto(),
    val preliminares: List<PreliminarDto> = emptyList(),
    val fatos: List<FatoDto> = emptyList(),
    val teses: List<TeseDto> = emptyList(),
    val pedidos: List<PedidoDto> = emptyList(),
    val provas: List<ProvaDto> = emptyList(),
)

@Serializable
data class CampoIncertoDto(
    val campo: String,
    val motivo: String,
    val confianca: Double,
)

@Serializable
data class QualidadeDto(
    @SerialName("confianca_geral") val confiancaGeral: Double,
    @SerialName("campos_incertos") val camposIncertos: List<CampoIncertoDto> = emptyList(),
    val lacunas: List<String> = emptyList(),
    @SerialName("requer_revisao") val requerRevisao: Boolean,
)

@Serializable
data class MetadadosDto(
    @SerialName("tipo_detectado") val tipoDetectado: String? = null,
    @SerialName("tipo_declarado") val tipoDeclarado: String,
    @SerialName("tipo_confirmado") val tipoConfirmado: Boolean = true,
    @SerialName("numero_processo") val numeroProcesso: String? = null,
    val vara: String? = null,
    @SerialName("paginas_originais") val paginasOriginais: Int = 0,
    val origem: String? = null,
)

@Serializable
data class AuditoriaDto(
    @SerialName("hash_arquivo_entrada") val hashArquivoEntrada: String,
    @SerialName("modelo_ia") val modeloIa: String,
    @SerialName("versao_conversor") val versaoConversor: String,
    @SerialName("pdf_descartado") val pdfDescartado: Boolean = true,
)

@Serializable
data class MinutaPackageDto(
    @SerialName("job_id") val jobId: String,
    @SerialName("versao_schema") val versaoSchema: String = "1.0",
    @SerialName("processado_em") val processadoEm: String,
    val metadados: MetadadosDto,
    val markdown: String,
    val estrutura: EstruturaPecaDto,
    val qualidade: QualidadeDto,
    val auditoria: AuditoriaDto,
)

/* ---------- Sugestão de minuta ---------- */

@Serializable
data class SugestaoMinutaRequestDto(
    @SerialName("tipo_minuta") val tipoMinuta: String,
    @SerialName("tipo_peca_origem") val tipoPecaOrigem: String,
    val estrutura: EstruturaPecaDto,
    @SerialName("numero_processo") val numeroProcesso: String? = null,
    val vara: String? = null,
    val orientacoes: String? = null,
    val origem: String = "app-android",
)

@Serializable
data class SecaoSugeridaDto(
    val titulo: String,
    val texto: String,
    @SerialName("fundamento_legal") val fundamentoLegal: List<String> = emptyList(),
    @SerialName("baseado_em") val baseadoEm: List<String> = emptyList(),
)

@Serializable
data class QualidadeSugestaoDto(
    @SerialName("confianca_geral") val confiancaGeral: Double,
    @SerialName("requer_revisao") val requerRevisao: Boolean = true,
)

@Serializable
data class AuditoriaSugestaoDto(
    @SerialName("modelo_ia") val modeloIa: String,
    @SerialName("versao_prompt") val versaoPrompt: String,
    @SerialName("hash_estrutura") val hashEstrutura: String,
)

@Serializable
data class MinutaSugestaoDto(
    @SerialName("sugestao_id") val sugestaoId: String,
    @SerialName("versao_schema") val versaoSchema: String = "1.0",
    @SerialName("gerado_em") val geradoEm: String,
    @SerialName("tipo_minuta") val tipoMinuta: String,
    val titulo: String,
    val markdown: String,
    val secoes: List<SecaoSugeridaDto> = emptyList(),
    @SerialName("pontos_atencao") val pontosAtencao: List<String> = emptyList(),
    val lacunas: List<String> = emptyList(),
    val qualidade: QualidadeSugestaoDto,
    val auditoria: AuditoriaSugestaoDto,
)
