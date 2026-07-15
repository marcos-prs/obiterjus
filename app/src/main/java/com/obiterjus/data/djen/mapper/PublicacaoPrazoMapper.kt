package com.obiterjus.data.djen.mapper

import com.obiterjus.domain.usecase.CalcularPrazoRegraUC
import com.obiterjus.domain.usecase.ResolverNaturezaProcessoUC
import com.obiterjus.data.publicacao.local.PublicacaoEntity

class PublicacaoPrazoMapper(
    private val calcularPrazoRegraUC: CalcularPrazoRegraUC,
    private val resolverNatureza: ResolverNaturezaProcessoUC? = null,
) {
    suspend fun comPrazoCalculado(entity: PublicacaoEntity): PublicacaoEntity {
        if (entity.prazoQuantidade != null && entity.prazoDataLimite != null) return entity

        val texto = entity.textoLimpo?.takeIf { it.isNotBlank() } ?: return entity

        val prazo = calcularPrazoRegraUC.invoke(
            texto = texto,
            tipoComunicacao = entity.tipoComunicacao,
            dataDisponibilizacao = entity.dataDisponibilizacao,
            tribunal = entity.tribunal,
            natureza = resolverNatureza?.invoke(entity.numeroProcesso),
        ) ?: return entity

        return entity.copy(
            prazoQuantidade = entity.prazoQuantidade ?: prazo.quantidade,
            prazoUnidade = entity.prazoUnidade ?: prazo.unidade,
            prazoDiasUteis = entity.prazoQuantidade?.let { entity.prazoDiasUteis } ?: prazo.diasUteis,
            prazoTexto = entity.prazoTexto ?: prazo.textoOriginal,
            prazoDataLimite = entity.prazoDataLimite ?: prazo.dataLimiteEstimada,
            prazoConfianca = prazo.confiancaCalculo.name,
        )
    }
}
