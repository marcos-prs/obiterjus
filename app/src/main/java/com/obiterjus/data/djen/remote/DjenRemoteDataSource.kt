package com.obiterjus.data.djen.remote

import android.util.Log
import com.obiterjus.data.djen.remote.dto.DjenComunicacaoDto
import java.time.LocalDate

class DjenRemoteDataSource(
    private val api: DjenApi,
    private val itensPorPagina: Int = DEFAULT_ITEMS_PER_PAGE,
    private val maxPaginas: Int = DEFAULT_MAX_PAGES,
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
            val response = api.buscarComunicacoes(
                numeroOab = numeroOab?.trim(),
                ufOab = ufOab?.trim()?.uppercase(),
                nomeAdvogado = nomeAdvogado?.trim(),
                dataDisponibilizacaoInicio = dataInicio.toString(),
                dataDisponibilizacaoFim = dataFim.toString(),
                pagina = pagina,
                itensPorPagina = itensPorPagina,
            )
            val items = response.items
            val pageIds = items.map { it.id }
            if (totalRemoto == null && response.count != null && response.count >= 0) {
                totalRemoto = response.count
            }
            Log.d(TAG, "  → página=$pagina | itens retornados=${items.size} | totalRemoto=$totalRemoto")

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
        private const val TAG = "DjenRemoteDataSource"
    }
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
