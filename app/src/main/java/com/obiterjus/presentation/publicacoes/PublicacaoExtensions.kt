package com.obiterjus.presentation.publicacoes

import com.obiterjus.presentation.componentes.cards.PrioridadeStripe
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.TipoAto
import com.obiterjus.domain.model.tipoAto

// Public extension to expose priority logic for Publicacao
fun Publicacao.prioridadeStripe(): PrioridadeStripe = when {
    // Utilize o receiver para acessar propriedades da Publicacao
    this.isSigiloso -> PrioridadeStripe.CRITICA
    this.tipoAto == TipoAto.SENTENCA -> PrioridadeStripe.SENTENCA
    this.tipoAto == TipoAto.DECISAO -> PrioridadeStripe.DECISAO
    this.prazo != null -> PrioridadeStripe.FAVORAVEL
    else -> PrioridadeStripe.ROTINEIRO
}
