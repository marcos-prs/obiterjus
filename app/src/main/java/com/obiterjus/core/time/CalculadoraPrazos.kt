package com.obiterjus.core.time

import com.obiterjus.data.time.CalendarioForenseDataSource
import com.obiterjus.data.time.FeriadoRepository
import com.obiterjus.data.time.PedidoCalculoPrazo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter

class CalculadoraPrazos(
    private val feriadoRepository: FeriadoRepository,
    private val calendarioForense: CalendarioForenseDataSource,
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
        tribunal: String? = null,
    ): ResultadoCalculoPrazo? = try {
        val unidadeNorm = unidade.lowercase().trim()

        // Meses: cálculo local direto, sem consultar a API
        if (unidadeNorm == "mês" || unidadeNorm == "mes" || unidadeNorm == "meses") {
            return ResultadoCalculoPrazo.Estimado(adicionarDiasCorridos(dataBase, quantidade * 30))
        }

        // API proprietária como fonte primária quando o tribunal é conhecido
        if (tribunal != null) {
            val apiResult = calcularViaApi(dataBase, quantidade, unidadeNorm, diasUteis, tribunal)
            if (apiResult != null) return apiResult
            // null da API = indisponível/tribunal não coberto → cai no fallback local silencioso
        }

        // Fallback local → Estimado
        val dataLocal = when (unidadeNorm) {
            "dia", "dias" -> if (diasUteis) adicionarDiasUteis(dataBase, quantidade) else adicionarDiasCorridos(dataBase, quantidade)
            "hora", "horas" -> adicionarDiasUteis(dataBase, (quantidade + 7) / 8)
            else -> return null
        }
        ResultadoCalculoPrazo.Estimado(dataLocal)
    } catch (e: Exception) {
        null
    }

    private suspend fun calcularViaApi(
        dataBase: LocalDate,
        quantidade: Int,
        unidadeNorm: String,
        diasUteis: Boolean,
        tribunal: String,
    ): ResultadoCalculoPrazo? = try {
        val apiUnidade = when {
            unidadeNorm == "hora" || unidadeNorm == "horas" -> "horas"
            diasUteis -> "dias_uteis"
            else -> "dias_corridos"
        }
        val pedido = PedidoCalculoPrazo(
            tribunal = tribunal,
            dataDisponibilizacao = dataBase.format(DateTimeFormatter.ISO_LOCAL_DATE),
            prazo = quantidade,
            unidade = apiUnidade,
        )
        val resposta = calendarioForense.calcularPrazo(pedido)
        when {
            resposta.estado == "CONFIAVEL" && resposta.dataVencimento != null ->
                ResultadoCalculoPrazo.Confiavel(LocalDate.parse(resposta.dataVencimento))
            resposta.estado.startsWith("BLOQUEADO_") ->
                // API conhece o tribunal mas há ambiguidade; sinaliza incerteza (não cai no fallback)
                ResultadoCalculoPrazo.Incerto(resposta.dataVencimento?.let { LocalDate.parse(it) })
            else -> null  // estado inesperado → fallback local
        }
    } catch (e: Exception) {
        // timeout, HTTP 4xx/5xx, rede → fallback local silencioso
        null
    }
}
