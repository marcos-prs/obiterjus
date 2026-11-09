package com.obiterjus.core.time

import android.util.Log
import com.obiterjus.data.time.CalendarioForenseDataSource
import com.obiterjus.data.time.PedidoCalculoPrazo
import com.obiterjus.data.time.RespostaPrazo
import com.obiterjus.domain.model.NaturezaProcesso
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    private val mutexCobertura = Mutex()
    private val coberturaPorAno = mutableMapOf<Int, ConsultaCobertura>()

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
                // Duas coisas o CONFIAVEL da API não cobre: a aproximação de
                // meses feita aqui e o tribunal sem calendário judicial —
                // nenhuma das duas se pode afirmar ao advogado.
                val rebaixa = emMeses ||
                    semCalendarioJudicial(tribunalNorm, dataBase.year, data.year)
                if (rebaixa) ResultadoCalculoPrazo.Incerto(data) else ResultadoCalculoPrazo.Confiavel(data)
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
     * indisponível ou o artigo não resolve (tenta de novo no próximo sync) —
     * inclusive no BLOQUEADO_SEM_CALENDARIO do tribunal sem camada judicial.
     *
     * O prazo daqui não carrega afirmação de confiabilidade (é sugestão a
     * confirmar pelo advogado), então a cobertura judicial não rebaixa nada
     * neste caminho.
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
        } catch (e: CancellationException) {
            throw e
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
     * True quando algum dos [anos] que a contagem atravessa não tem calendário
     * judicial do [tribunal]. Nesses tribunais a API conta pulando apenas fim
     * de semana, feriado legal e a suspensão do art. 220: as portarias do
     * tribunal (recesso, emendas, Semana Santa estendida, Dia da Justiça)
     * ficam de fora, e o prazo não pode ser apresentado como confiável mesmo
     * quando a API responde CONFIAVEL — é o que acontece enquanto o servidor
     * não devolve BLOQUEADO_SEM_CALENDARIO.
     *
     * False quando a cobertura é desconhecida: ausência de informação não é
     * motivo para rebaixar.
     */
    private suspend fun semCalendarioJudicial(tribunal: String, vararg anos: Int): Boolean {
        val sigla = tribunal.uppercase()
        return anos.distinct().any { ano -> cobertura(ano)?.contains(sigla) == true }
    }

    /**
     * Siglas sem calendário judicial no [ano]; null quando não se sabe.
     *
     * Uma consulta por ano por dia — a cobertura só muda quando entra uma
     * coleta nova, e o cálculo de cada publicação não pode arrastar um /saude
     * atrás de si. A falha vale pouco de propósito: a instância free do Render
     * hiberna e a primeira chamada do dia costuma estourar; guardar esse
     * desconhecimento por 24h devolveria o rótulo CONFIAVEL justamente aos
     * tribunais sem calendário.
     */
    private suspend fun cobertura(ano: Int): Set<String>? = mutexCobertura.withLock {
        val agora = Instant.now(clock)
        coberturaPorAno[ano]?.let { cache ->
            if (agora.isBefore(cache.consultadaEm + cache.validade())) return@withLock cache.siglas
        }

        val siglas = try {
            calendarioForense.consultarSaude(ano).coberturaJudicial?.let { judicial ->
                (judicial.zerados + judicial.detalhe.filterValues { it == 0 }.keys)
                    .mapNotNull { it.trim().uppercase().takeIf(String::isNotEmpty) }
                    .toSet()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "/saude indisponível (ano=$ano): cobertura judicial desconhecida", e)
            null
        }

        coberturaPorAno[ano] = ConsultaCobertura(agora, siglas)
        siglas
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logFalha(pedido.tribunal, tentativa + 1, e)
                delay(RETRY_DELAY_MS)
            }
        }
        return try {
            calendarioForense.calcularPrazo(pedido)
        } catch (e: CancellationException) {
            throw e
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

    /** Resposta do /saude para um ano; [siglas] null = cobertura desconhecida. */
    private class ConsultaCobertura(
        val consultadaEm: Instant,
        val siglas: Set<String>?,
    ) {
        fun validade(): Duration = if (siglas == null) BACKOFF_FALHA else VALIDADE_COBERTURA
    }

    private companion object {
        const val TAG = "CalculadoraPrazos"
        const val TENTATIVAS = 2
        const val RETRY_DELAY_MS = 750L
        val VALIDADE_COBERTURA: Duration = Duration.ofHours(24)
        val BACKOFF_FALHA: Duration = Duration.ofMinutes(5)
    }
}

/** Prazo resolvido pela tabela versionada da API (origem=ruleset). */
data class PrazoRuleset(
    val quantidade: Int,
    val diasUteis: Boolean,
    val dataVencimento: LocalDate,
)
