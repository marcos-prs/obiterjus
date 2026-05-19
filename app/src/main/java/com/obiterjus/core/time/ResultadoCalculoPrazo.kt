package com.obiterjus.core.time

import java.time.LocalDate

sealed class ResultadoCalculoPrazo {
    abstract val data: LocalDate?

    /** API retornou CONFIAVEL com data de vencimento. */
    data class Confiavel(override val data: LocalDate) : ResultadoCalculoPrazo()

    /** API reconhece o tribunal mas sinaliza bloqueio/ambiguidade (BLOQUEADO_*). */
    data class Incerto(override val data: LocalDate?) : ResultadoCalculoPrazo()

    /** Cálculo local — API indisponível, tribunal não coberto ou unidade em meses. */
    data class Estimado(override val data: LocalDate?) : ResultadoCalculoPrazo()
}
