package com.obiterjus.core.time

import com.obiterjus.data.time.FeriadoRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay

class CalculadoraPrazos(
    private val feriadoRepository: FeriadoRepository
) {

    private val feriadosFixos: Set<MonthDay> = setOf(
        MonthDay.of(1, 1),
        MonthDay.of(4, 21),
        MonthDay.of(5, 1),
        MonthDay.of(9, 7),
        MonthDay.of(10, 12),
        MonthDay.of(11, 2),
        MonthDay.of(11, 15),
        MonthDay.of(11, 20),
        MonthDay.of(12, 25),
    )

    suspend fun adicionarDiasUteis(dataInicio: LocalDate, quantidadeDias: Int): LocalDate {
        require(quantidadeDias >= 0) { "Quantidade de dias não pode ser negativa." }

        var data = dataInicio
        var diasContados = 0

        while (diasContados < quantidadeDias) {
            data = data.plusDays(1)
            if (isDiaUtil(data)) {
                diasContados++
            }
        }

        while (!isDiaUtil(data)) {
            data = data.plusDays(1)
        }

        return data
    }

    fun adicionarDiasCorridos(dataInicio: LocalDate, quantidadeDias: Int): LocalDate =
        dataInicio.plusDays(quantidadeDias.toLong())

    suspend fun isDiaUtil(data: LocalDate): Boolean {
        val dow = data.dayOfWeek
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false
        if (MonthDay.from(data) in feriadosFixos) return false
        if (emSuspensaoForense(data)) return false

        val feriadosDinamicos = feriadoRepository.getFeriados(data.year)
        return data !in feriadosDinamicos
    }

    // CPC art. 220: prazos processuais suspensos de 20/12 a 20/01, inclusive.
    private fun emSuspensaoForense(data: LocalDate): Boolean {
        val md = MonthDay.from(data)
        return (md.monthValue == 12 && md.dayOfMonth >= 20) ||
            (md.monthValue == 1 && md.dayOfMonth <= 20)
    }

    suspend fun calcularDataLimite(
        dataBase: LocalDate,
        quantidade: Int,
        unidade: String,
        diasUteis: Boolean,
    ): LocalDate? = try {
        when (unidade.lowercase().trim()) {
            "dia", "dias" -> {
                if (diasUteis) adicionarDiasUteis(dataBase, quantidade)
                else adicionarDiasCorridos(dataBase, quantidade)
            }
            "hora", "horas" -> {
                val diasEquivalentes = (quantidade + 7) / 8
                adicionarDiasUteis(dataBase, diasEquivalentes)
            }
            "mês", "mes", "meses" -> adicionarDiasCorridos(dataBase, quantidade * 30)
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
