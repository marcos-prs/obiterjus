package com.obiterjus.data.minuta

import com.obiterjus.data.minuta.remote.dto.CampoParteDto
import com.obiterjus.data.minuta.remote.dto.EstruturaPecaDto
import com.obiterjus.data.minuta.remote.dto.FatoDto
import com.obiterjus.data.minuta.remote.dto.MinutaPackageDto
import com.obiterjus.data.minuta.remote.dto.MinutaSugestaoDto
import com.obiterjus.data.minuta.remote.dto.PartesDto
import com.obiterjus.data.minuta.remote.dto.PedidoDto
import com.obiterjus.data.minuta.remote.dto.PreliminarDto
import com.obiterjus.data.minuta.remote.dto.ProvaDto
import com.obiterjus.data.minuta.remote.dto.SecaoSugeridaDto
import com.obiterjus.data.minuta.remote.dto.TeseDto
import com.obiterjus.domain.model.CampoParte
import com.obiterjus.domain.model.EstruturaPeca
import com.obiterjus.domain.model.FatoPeca
import com.obiterjus.domain.model.MinutaPackage
import com.obiterjus.domain.model.MinutaSugestao
import com.obiterjus.domain.model.PartesPeca
import com.obiterjus.domain.model.PedidoPeca
import com.obiterjus.domain.model.PreliminarPeca
import com.obiterjus.domain.model.ProvaPeca
import com.obiterjus.domain.model.QualidadeExtracao
import com.obiterjus.domain.model.SecaoSugerida
import com.obiterjus.domain.model.TesePeca
import com.obiterjus.domain.model.TipoMinuta
import java.time.Instant

/* ---------- DTO de rede → domínio ---------- */

private fun CampoParteDto.toDomain() = CampoParte(valor, confianca, origemTrecho)

private fun PartesDto.toDomain() = PartesPeca(
    requerente = requerente?.toDomain(),
    requerido = requerido?.toDomain(),
)

private fun PreliminarDto.toDomain() = PreliminarPeca(titulo, texto, confianca, origemTrecho)

private fun FatoDto.toDomain() = FatoPeca(sequencia, texto, confianca, origemTrecho)

private fun TeseDto.toDomain() = TesePeca(titulo, fundamentoLegal, texto, confianca, origemTrecho)

private fun PedidoDto.toDomain() = PedidoPeca(tipo, texto, confianca, origemTrecho)

private fun ProvaDto.toDomain() = ProvaPeca(tipo, descricao, referenciaDoc, confianca, origemTrecho)

fun EstruturaPecaDto.toDomain() = EstruturaPeca(
    partes = partes.toDomain(),
    preliminares = preliminares.map { it.toDomain() },
    fatos = fatos.map { it.toDomain() },
    teses = teses.map { it.toDomain() },
    pedidos = pedidos.map { it.toDomain() },
    provas = provas.map { it.toDomain() },
)

fun MinutaPackageDto.toDomain() = MinutaPackage(
    jobId = jobId,
    tipoDetectado = metadados.tipoDetectado,
    tipoDeclarado = metadados.tipoDeclarado,
    numeroProcesso = metadados.numeroProcesso,
    vara = metadados.vara,
    paginasOriginais = metadados.paginasOriginais,
    markdown = markdown,
    estrutura = estrutura.toDomain(),
    qualidade = QualidadeExtracao(
        confiancaGeral = qualidade.confiancaGeral,
        camposIncertos = qualidade.camposIncertos.map {
            com.obiterjus.domain.model.CampoIncerto(it.campo, it.motivo, it.confianca)
        },
        lacunas = qualidade.lacunas,
        requerRevisao = qualidade.requerRevisao,
    ),
)

private fun SecaoSugeridaDto.toDomain() = SecaoSugerida(titulo, texto, fundamentoLegal, baseadoEm)

/**
 * [numeroProcesso] e [geradoEmInstant] vêm do contexto do app (a resposta da
 * API traz `gerado_em` como texto ISO e não repete o número do processo).
 */
fun MinutaSugestaoDto.toDomain(
    numeroProcesso: String?,
    geradoEmInstant: Instant,
) = MinutaSugestao(
    sugestaoId = sugestaoId,
    numeroProcesso = numeroProcesso,
    tipoMinuta = TipoMinuta.porCodigo(tipoMinuta),
    titulo = titulo,
    markdown = markdown,
    secoes = secoes.map { it.toDomain() },
    pontosAtencao = pontosAtencao,
    lacunas = lacunas,
    confiancaGeral = qualidade.confiancaGeral,
    requerRevisao = qualidade.requerRevisao,
    geradoEm = geradoEmInstant,
)

/* ---------- Domínio → DTO de request ---------- */

private fun CampoParte.toDto() = CampoParteDto(valor, confianca, origemTrecho)

private fun PartesPeca.toDto() = PartesDto(
    requerente = requerente?.toDto(),
    requerido = requerido?.toDto(),
)

fun EstruturaPeca.toDto() = EstruturaPecaDto(
    partes = partes.toDto(),
    preliminares = preliminares.map { PreliminarDto(it.titulo, it.texto, it.confianca, it.origemTrecho) },
    fatos = fatos.map { FatoDto(it.sequencia, it.texto, it.confianca, it.origemTrecho) },
    teses = teses.map { TeseDto(it.titulo, it.fundamentoLegal, it.texto, it.confianca, it.origemTrecho) },
    pedidos = pedidos.map { PedidoDto(it.tipo, it.texto, it.confianca, it.origemTrecho) },
    provas = provas.map { ProvaDto(it.tipo, it.descricao, it.referenciaDoc, it.confianca, it.origemTrecho) },
)
