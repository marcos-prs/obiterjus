package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.EstruturaPeca
import com.obiterjus.domain.model.MinutaSugestao
import com.obiterjus.domain.model.TipoMinuta
import com.obiterjus.domain.repository.MinutaRepository

/** Gera o rascunho a partir da estrutura revisada e o persiste no processo. */
class SugerirMinutaUC(
    private val repositorioMinuta: MinutaRepository,
) {
    suspend operator fun invoke(
        tipoMinuta: TipoMinuta,
        tipoPecaOrigem: String,
        textoOrigem: String,
        estrutura: EstruturaPeca,
        numeroProcesso: String? = null,
        vara: String? = null,
        orientacoes: String? = null,
    ): MinutaSugestao =
        repositorioMinuta.sugerirMinuta(
            tipoMinuta = tipoMinuta,
            tipoPecaOrigem = tipoPecaOrigem,
            textoOrigem = textoOrigem,
            estrutura = estrutura,
            numeroProcesso = numeroProcesso,
            vara = vara,
            orientacoes = orientacoes,
        )
}
