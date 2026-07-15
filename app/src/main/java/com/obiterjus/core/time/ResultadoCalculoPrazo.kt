package com.obiterjus.core.time

import java.time.LocalDate

sealed class ResultadoCalculoPrazo {
    abstract val data: LocalDate?

    /** API retornou CONFIAVEL com data de vencimento. */
    data class Confiavel(override val data: LocalDate) : ResultadoCalculoPrazo()

    /** API reconhece o tribunal mas sinaliza bloqueio/ambiguidade (BLOQUEADO_*),
     *  ou o resultado depende de aproximação (ex.: prazo em meses). */
    data class Incerto(override val data: LocalDate?) : ResultadoCalculoPrazo()

    /** Cálculo ainda não obtido — API indisponível ou tribunal ausente.
     *  Nunca carrega data: o app não calcula prazos localmente; a API
     *  Calendário Forense é a única fonte. Recalculado no próximo sync. */
    data object Pendente : ResultadoCalculoPrazo() {
        override val data: LocalDate? = null
    }
}
