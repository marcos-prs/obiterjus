package com.obiterjus.core.time

import android.util.Log
import com.obiterjus.data.time.CalendarioForenseDataSource
import com.obiterjus.data.time.PedidoCalculoPrazo
import com.obiterjus.data.time.RespostaPrazo
import com.obiterjus.domain.model.NaturezaProcesso
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import retrofit2.HttpException

/**
 * Adapter da API CalendárioForense — única fonte de cálculo de prazos.
 *
 * O app não replica regras de contagem (dias úteis, feriados, suspensões):
 * quando a API está inacessível ou o tribunal é desconhecido, o resultado é
 * [ResultadoCalculoPrazo.Pendente] e o cálculo é reencaminhado no próximo
 * ciclo de sincronização.
 */
class CalculadoraPrazos(
    private val calendarioForense: CalendarioForenseDataSource,
) {

    suspend fun calcularDataLimite(
        dataBase: LocalDate,
        quantidade: Int,
        unidade: String,
        diasUteis: Boolean,
        tribunal: String? = null,
        natureza: NaturezaProcesso? = null,
    ): ResultadoCalculoPrazo {
        val tribunalNorm = tribunal?.trim()?.takeIf { it.isNotEmpty() }
            ?: return ResultadoCalculoPrazo.Pendente

        val unidadeNorm = unidade.lowercase().trim()
        // Meses são aproximados como 30 dias corridos cada; mesmo com resposta
        // CONFIAVEL da API, a aproximação rebaixa o resultado para Incerto.
        val emMeses = unidadeNorm == "mês" || unidadeNorm == "mes" || unidadeNorm == "meses"
        val apiUnidade = when {
            unidadeNorm == "hora" || unidadeNorm == "horas" -> "horas"
            emMeses -> "dias_corridos"
            diasUteis -> "dias_uteis"
            else -> "dias_corridos"
        }
        val apiQuantidade = if (emMeses) quantidade * 30 else quantidade

        val resposta = chamarComRetry(
            PedidoCalculoPrazo(
                tribunal = tribunalNorm,
                dataDisponibilizacao = dataBase.format(DateTimeFormatter.ISO_LOCAL_DATE),
                prazo = apiQuantidade,
                unidade = apiUnidade,
                natureza = when (natureza) {
                    NaturezaProcesso.PENAL -> "penal"
                    NaturezaProcesso.CIVEL, null -> "civel"
                },
            ),
        ) ?: return ResultadoCalculoPrazo.Pendente

        if (resposta.estado != "CONFIAVEL" && !resposta.estado.startsWith("BLOQUEADO_")) {
            Log.w(TAG, "calcular-prazo estado inesperado: ${resposta.estado} (tribunal=$tribunalNorm)")
        }

        return when {
            resposta.estado == "CONFIAVEL" && resposta.dataVencimento != null -> {
                val data = LocalDate.parse(resposta.dataVencimento)
                if (emMeses) ResultadoCalculoPrazo.Incerto(data) else ResultadoCalculoPrazo.Confiavel(data)
            }

            resposta.estado.startsWith("BLOQUEADO_") ->
                // API conhece o tribunal mas há ambiguidade; sinaliza incerteza
                ResultadoCalculoPrazo.Incerto(resposta.dataVencimento?.let { LocalDate.parse(it) })

            else -> ResultadoCalculoPrazo.Pendente
        }
    }

    /**
     * Cálculo com o número de dias resolvido pelo ruleset da API
     * (origem=ruleset + artigo do CPC) — o app não conhece os números; a
     * tabela prazos_cpc versionada é a fonte. Null quando a API está
     * indisponível ou o artigo não resolve (tenta de novo no próximo sync).
     */
    suspend fun calcularPrazoPorRuleset(
        dataBase: LocalDate,
        artigo: String,
        tribunal: String?,
        natureza: NaturezaProcesso? = null,
    ): PrazoRuleset? {
        val tribunalNorm = tribunal?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        val resposta = try {
            calendarioForense.calcularPrazo(
                PedidoCalculoPrazo(
                    tribunal = tribunalNorm,
                    dataDisponibilizacao = dataBase.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    origem = "ruleset",
                    artigo = artigo,
                    natureza = when (natureza) {
                        NaturezaProcesso.PENAL -> "penal"
                        NaturezaProcesso.CIVEL, null -> "civel"
                    },
                ),
            )
        } catch (e: Exception) {
            return null
        }

        val quantidade = resposta.prazo ?: return null
        val data = resposta.dataVencimento?.let { LocalDate.parse(it) } ?: return null
        if (!(resposta.estado == "CONFIAVEL" || resposta.estado.startsWith("CONFIAVEL_"))) return null

        return PrazoRuleset(
            quantidade = quantidade,
            diasUteis = resposta.unidade != "dias_corridos",
            dataVencimento = data,
        )
    }

    /**
     * A API roda em instância free do Render e responde 500 esporádicos;
     * uma repetição curta resolve a maioria. Timeout/rede também repete.
     * Null quando ambas as tentativas falham.
     */
    private suspend fun chamarComRetry(pedido: PedidoCalculoPrazo): RespostaPrazo? {
        repeat(TENTATIVAS - 1) { tentativa ->
            try {
                return calendarioForense.calcularPrazo(pedido)
            } catch (e: Exception) {
                logFalha(pedido.tribunal, tentativa + 1, e)
                delay(RETRY_DELAY_MS)
            }
        }
        return try {
            calendarioForense.calcularPrazo(pedido)
        } catch (e: Exception) {
            logFalha(pedido.tribunal, TENTATIVAS, e)
            null
        }
    }

    private fun logFalha(tribunal: String, tentativa: Int, e: Exception) {
        val corpoErro = (e as? HttpException)
            ?.response()?.errorBody()
            ?.let { runCatching { it.string().take(500) }.getOrNull() }
        Log.w(
            TAG,
            "calcular-prazo falhou (tribunal=$tribunal, tentativa=$tentativa/$TENTATIVAS)" +
                (corpoErro?.let { " corpo=$it" } ?: ""),
            e,
        )
    }

    private companion object {
        const val TAG = "CalculadoraPrazos"
        const val TENTATIVAS = 2
        const val RETRY_DELAY_MS = 750L
    }
}

/** Prazo resolvido pela tabela versionada da API (origem=ruleset). */
data class PrazoRuleset(
    val quantidade: Int,
    val diasUteis: Boolean,
    val dataVencimento: LocalDate,
)
