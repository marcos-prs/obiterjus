package com.obiterjus.domain.model

import java.time.Instant

/**
 * Modelos de domínio da minuta (API Obiter-Minuta).
 *
 * Fluxo em duas etapas:
 * 1. Extração — envia o PDF da peça recebida e recebe a [MinutaPackage] com a
 *    [EstruturaPeca] extraída. O advogado revisa antes de gerar.
 * 2. Sugestão — a partir da estrutura revisada, gera a [MinutaSugestao]: um
 *    rascunho da peça de resposta que **sempre** requer revisão humana.
 *
 * Estes modelos são desacoplados dos DTOs de rede (mapeados em
 * `data/minuta/MinutaMappers.kt`).
 */

/** Peça de resposta que o advogado quer gerar. [codigo] é o valor enviado à API. */
enum class TipoMinuta(val codigo: String, val rotulo: String) {
    CONTESTACAO("contestacao", "Contestação"),
    REPLICA("replica", "Réplica"),
    IMPUGNACAO("impugnacao", "Impugnação"),
    RECURSO("recurso", "Recurso"),
    CONTRARRAZOES("contrarrazoes", "Contrarrazões"),
    EMBARGOS_DECLARACAO("embargos_declaracao", "Embargos de declaração"),
    MANIFESTACAO("manifestacao", "Manifestação"),
    OUTRO("outro", "Outro"),
    ;

    companion object {
        fun porCodigo(codigo: String?): TipoMinuta =
            entries.firstOrNull { it.codigo == codigo } ?: OUTRO
    }
}

/**
 * Tipo declarado da peça **recebida** que será extraída (o `tipo_declarado` do
 * `/ingest`). Difere de [TipoMinuta], que é a peça a **gerar** em resposta.
 * [codigo] casa com o enum `TipoPeca` da API.
 */
enum class TipoPecaOrigem(val codigo: String, val rotulo: String) {
    INICIAL("inicial", "Petição inicial"),
    CONTESTACAO("contestacao", "Contestação"),
    TUTELA("tutela", "Tutela"),
    REPLICA("replica", "Réplica"),
    RECURSO("recurso", "Recurso"),
    OUTRO("outro", "Outro"),
}

/* ---------- Extração ---------- */

/** Campo extraído com o grau de confiança da IA (0.0–1.0). */
data class CampoParte(
    val valor: String,
    val confianca: Double,
    val origemTrecho: String? = null,
)

data class PartesPeca(
    val requerente: CampoParte? = null,
    val requerido: CampoParte? = null,
)

data class PreliminarPeca(
    val titulo: String,
    val texto: String,
    val confianca: Double,
    val origemTrecho: String? = null,
)

data class FatoPeca(
    val sequencia: Int,
    val texto: String,
    val confianca: Double,
    val origemTrecho: String? = null,
)

data class TesePeca(
    val titulo: String,
    val fundamentoLegal: List<String> = emptyList(),
    val texto: String,
    val confianca: Double,
    val origemTrecho: String? = null,
)

data class PedidoPeca(
    val tipo: String,
    val texto: String,
    val confianca: Double,
    val origemTrecho: String? = null,
)

data class ProvaPeca(
    val tipo: String,
    val descricao: String,
    val referenciaDoc: String? = null,
    val confianca: Double,
    val origemTrecho: String? = null,
)

data class EstruturaPeca(
    val partes: PartesPeca = PartesPeca(),
    val preliminares: List<PreliminarPeca> = emptyList(),
    val fatos: List<FatoPeca> = emptyList(),
    val teses: List<TesePeca> = emptyList(),
    val pedidos: List<PedidoPeca> = emptyList(),
    val provas: List<ProvaPeca> = emptyList(),
)

data class CampoIncerto(
    val campo: String,
    val motivo: String,
    val confianca: Double,
)

data class QualidadeExtracao(
    val confiancaGeral: Double,
    val camposIncertos: List<CampoIncerto> = emptyList(),
    val lacunas: List<String> = emptyList(),
    val requerRevisao: Boolean,
)

/** Resultado da extração. Transitório: o endpoint `/result` expira na 1ª leitura. */
data class MinutaPackage(
    val jobId: String,
    val tipoDetectado: String?,
    val tipoDeclarado: String,
    val numeroProcesso: String?,
    val vara: String?,
    val paginasOriginais: Int,
    val markdown: String,
    val estrutura: EstruturaPeca,
    val qualidade: QualidadeExtracao,
)

/** Progresso do job de extração, consumido durante o polling. */
data class ExtracaoProgresso(
    val jobId: String,
    val status: String,
    val stage: String? = null,
    val progressoPct: Int = 0,
) {
    val concluido: Boolean get() = status.equals("done", ignoreCase = true) ||
        status.equals("completed", ignoreCase = true) ||
        status.equals("success", ignoreCase = true)

    val falhou: Boolean get() = status.equals("error", ignoreCase = true) ||
        status.equals("failed", ignoreCase = true)
}

/* ---------- Sugestão ---------- */

data class SecaoSugerida(
    val titulo: String,
    val texto: String,
    val fundamentoLegal: List<String> = emptyList(),
    val baseadoEm: List<String> = emptyList(),
)

/**
 * Rascunho gerado da peça de resposta. [requerRevisao] é sempre `true`: a
 * sugestão nunca é apresentada como peça pronta.
 */
data class MinutaSugestao(
    val sugestaoId: String,
    val numeroProcesso: String?,
    val tipoMinuta: TipoMinuta,
    val titulo: String,
    val markdown: String,
    val secoes: List<SecaoSugerida> = emptyList(),
    val pontosAtencao: List<String> = emptyList(),
    val lacunas: List<String> = emptyList(),
    val confiancaGeral: Double,
    val requerRevisao: Boolean = true,
    val geradoEm: Instant,
)
