package com.obiterjus.data.djen.remote

import android.util.Log
import com.obiterjus.data.djen.remote.dto.DjenComunicacaoDto
import com.obiterjus.data.djen.remote.dto.DjenResponseDto
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.delay
import retrofit2.HttpException

class DjenRemoteDataSource(
    private val api: DjenApi,
    private val itensPorPagina: Int = DEFAULT_ITEMS_PER_PAGE,
    private val maxPaginas: Int = DEFAULT_MAX_PAGES,
    private val limiteResultadosTotal: Int = DEFAULT_TOTAL_LIMIT,
    /**
     * Esperas entre as tentativas de uma mesma página. O tamanho da lista é o
     * número de retentativas; lista vazia desliga a retentativa.
     */
    private val esperasEntreTentativasMs: List<Long> = ESPERAS_PADRAO_MS,
) {
    suspend fun buscarComunicacoes(
        dataInicio: LocalDate,
        dataFim: LocalDate,
        numeroOab: String? = null,
        ufOab: String? = null,
        nomeAdvogado: String? = null,
    ): DjenFetchResult {
        require(itensPorPagina > 0) { "itensPorPagina deve ser maior que zero." }
        require(maxPaginas > 0) { "maxPaginas deve ser maior que zero." }

        val allItems = mutableListOf<DjenComunicacaoDto>()
        var pagina = 1
        var totalRemoto: Int? = null
        var previousPageIds = emptyList<Long>()

        while (pagina <= maxPaginas) {
            Log.d(TAG, "buscarComunicacoes: OAB=$numeroOab/$ufOab | NOME=$nomeAdvogado | $dataInicio .. $dataFim | página=$pagina/$maxPaginas")
            val response = buscarPaginaComRetentativa(
                numeroOab = numeroOab,
                ufOab = ufOab,
                nomeAdvogado = nomeAdvogado,
                dataInicio = dataInicio,
                dataFim = dataFim,
                pagina = pagina,
            )
            val items = response.items
            val pageIds = items.map { it.id }
            if (totalRemoto == null && response.count != null && response.count >= 0) {
                totalRemoto = response.count
            }
            Log.d(TAG, "  → página=$pagina | itens retornados=${items.size} | totalRemoto=$totalRemoto")

            // Cinto de segurança: se o totalRemoto for absurdo, é sinal de que
            // a busca pegou homônimos do país inteiro. Aborta antes de inundar
            // o banco (e antes que a API responda 429 por excesso de paginação).
            val totalConhecido = totalRemoto
            if (pagina == 1 && totalConhecido != null && totalConhecido > limiteResultadosTotal) {
                Log.w(
                    TAG,
                    "  → STOP: totalRemoto=$totalConhecido excede limite=$limiteResultadosTotal | abortando paginação (provável homônimo).",
                )
                return emptyList<DjenComunicacaoDto>().toFetchResult(
                    totalRemoto = totalRemoto,
                    paginasConsultadas = pagina,
                    motivoParada = DjenPaginationStopReason.PARTIAL_PAGE,
                )
            }

            if (items.isEmpty()) {
                Log.d(TAG, "  → STOP: EMPTY_PAGE na página=$pagina | total acumulado=${allItems.size}")
                return allItems.toFetchResult(
                    totalRemoto = totalRemoto,
                    paginasConsultadas = pagina,
                    motivoParada = DjenPaginationStopReason.EMPTY_PAGE,
                )
            }

            if (pageIds == previousPageIds) {
                Log.w(TAG, "  → STOP: REPEATED_PAGE na página=$pagina | IDs repetidos: $pageIds")
                return allItems.toFetchResult(
                    totalRemoto = totalRemoto,
                    paginasConsultadas = pagina,
                    motivoParada = DjenPaginationStopReason.REPEATED_PAGE,
                )
            }

            allItems += items

            if (items.size < itensPorPagina) {
                Log.d(TAG, "  → STOP: PARTIAL_PAGE na página=$pagina | itens=${items.size} < $itensPorPagina | total acumulado=${allItems.size}")
                return allItems.toFetchResult(
                    totalRemoto = totalRemoto,
                    paginasConsultadas = pagina,
                    motivoParada = DjenPaginationStopReason.PARTIAL_PAGE,
                )
            }

            val total = totalRemoto
            if (total != null && allItems.size >= total) {
                Log.d(TAG, "  → STOP: COUNT_CONSUMED | acumulado=${allItems.size} >= totalRemoto=$total")
                return allItems.toFetchResult(
                    totalRemoto = totalRemoto,
                    paginasConsultadas = pagina,
                    motivoParada = DjenPaginationStopReason.COUNT_CONSUMED,
                )
            }

            previousPageIds = pageIds
            pagina += 1
        }

        Log.d(TAG, "  → STOP: PAGE_LIMIT atingido ($maxPaginas páginas) | total acumulado=${allItems.size}")
        return allItems.toFetchResult(
            totalRemoto = totalRemoto,
            paginasConsultadas = maxPaginas,
            motivoParada = DjenPaginationStopReason.PAGE_LIMIT,
        )
    }

    /**
     * Uma página, com retentativa no que é transitório.
     *
     * A API do CNJ devolve 500 de forma intermitente: medido em 19/11/2026, a
     * mesma URL que respondeu 500 respondeu 200 nas cinco chamadas seguintes.
     * Sem retentativa, um desses 500 derruba a sincronização inteira — inclusive
     * as páginas e as buscas que já tinham dado certo, porque
     * `DjenRepositoryImpl` aborta na primeira exceção. Era o que aparecia na
     * Auditoria como `HTTP 500` e deixava o dia sem publicação nenhuma.
     *
     * Retenta só o que pode mudar sozinho: 5xx, 429 (a API responde assim a
     * rajada) e falha de transporte. 4xx de requisição inválida sobe na hora —
     * insistir nele só gastaria o orçamento de 10 min do worker.
     */
    private suspend fun buscarPaginaComRetentativa(
        numeroOab: String?,
        ufOab: String?,
        nomeAdvogado: String?,
        dataInicio: LocalDate,
        dataFim: LocalDate,
        pagina: Int,
    ): DjenResponseDto {
        var tentativa = 0
        while (true) {
            try {
                return api.buscarComunicacoes(
                    numeroOab = numeroOab?.trim(),
                    ufOab = ufOab?.trim()?.uppercase(),
                    nomeAdvogado = nomeAdvogado?.trim(),
                    dataDisponibilizacaoInicio = dataInicio.toString(),
                    dataDisponibilizacaoFim = dataFim.toString(),
                    pagina = pagina,
                    itensPorPagina = itensPorPagina,
                )
            } catch (erro: Exception) {
                if (!erro.ehTransitorio()) throw erro
                if (tentativa >= esperasEntreTentativasMs.size) {
                    throw DjenIndisponivelException(erro, esperasEntreTentativasMs.size + 1)
                }
                val espera = esperasEntreTentativasMs[tentativa]
                tentativa += 1
                Log.w(
                    TAG,
                    "  → falha transitória na página=$pagina (${erro.descricaoCurta()}); " +
                        "tentativa ${tentativa + 1} em ${espera}ms",
                )
                delay(espera)
            }
        }
    }

    private fun Exception.ehTransitorio(): Boolean = when (this) {
        is HttpException -> code() >= 500 || code() == HTTP_EXCESSO_DE_REQUISICOES
        is IOException -> true
        else -> false
    }

    private fun List<DjenComunicacaoDto>.toFetchResult(
        totalRemoto: Int?,
        paginasConsultadas: Int,
        motivoParada: DjenPaginationStopReason,
    ): DjenFetchResult =
        DjenFetchResult(
            items = this,
            totalRemoto = totalRemoto,
            paginasConsultadas = paginasConsultadas,
            motivoParada = motivoParada,
        )

    companion object {
        const val DEFAULT_ITEMS_PER_PAGE = 100
        const val DEFAULT_MAX_PAGES = 100
        const val DEFAULT_TOTAL_LIMIT = 500
        private const val TAG = "DjenRemoteDataSource"
        private const val HTTP_EXCESSO_DE_REQUISICOES = 429

        /**
         * Duas retentativas, curtas. O worker tem 10 minutos de orçamento e a
         * busca pode ter várias páginas e variações de nome; esperar mais do
         * que isso arriscaria o `STOP_REASON_TIMEOUT`, que é outra falha.
         */
        private val ESPERAS_PADRAO_MS = listOf(2_000L, 6_000L)
    }
}

/**
 * A API do DJEN não respondeu nem depois das retentativas.
 *
 * Existe pela mensagem: o que chegava à Auditoria era o `HTTP 500 ` cru do
 * Retrofit, que não diz ao advogado se o problema é dele, do app ou do CNJ.
 */
class DjenIndisponivelException(
    causa: Exception,
    tentativas: Int,
) : IllegalStateException(
    "DJEN indisponível (${causa.descricaoCurta()}) após $tentativas tentativas. " +
        "A próxima execução tenta de novo.",
    causa,
)

private fun Exception.descricaoCurta(): String = when (this) {
    is HttpException -> "HTTP ${code()}"
    is IOException -> message?.takeIf { it.isNotBlank() } ?: "falha de rede"
    else -> message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
}

data class DjenFetchResult(
    val items: List<DjenComunicacaoDto>,
    val totalRemoto: Int?,
    val paginasConsultadas: Int,
    val motivoParada: DjenPaginationStopReason,
)

enum class DjenPaginationStopReason {
    EMPTY_PAGE,
    PARTIAL_PAGE,
    COUNT_CONSUMED,
    REPEATED_PAGE,
    PAGE_LIMIT,
}
