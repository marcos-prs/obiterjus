package com.obiterjus.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object FormatadorData {
    private val dataBrasileira = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dataHoraBrasileira = DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    private val dataPorExtenso = DateTimeFormatter
        .ofPattern("dd MMM yyyy", java.util.Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.systemDefault())

    fun formatarData(data: LocalDate): String = data.format(dataBrasileira)

    fun formatarDataHora(instante: Instant?): String = 
        instante?.let { dataHoraBrasileira.format(it) } ?: "Data não disponível"

    fun formatarDataPorExtenso(instante: Instant?): String = 
        instante?.let { dataPorExtenso.format(it).lowercase() } ?: "Data não disponível"
}
