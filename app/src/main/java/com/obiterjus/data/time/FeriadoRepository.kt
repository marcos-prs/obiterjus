package com.obiterjus.data.time

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FeriadoRepository(
    private val brasilApiDataSource: BrasilApiDataSource
) {
    // Cache em memória simples para evitar buscas repetidas no mesmo ano
    private val feriadosPorAno = mutableMapOf<Int, Set<LocalDate>>()

    suspend fun getFeriados(ano: Int): Set<LocalDate> {
        feriadosPorAno[ano]?.let { return it }

        return try {
            val response = brasilApiDataSource.getFeriadosNacionais(ano)
            if (response.isSuccessful) {
                val feriadosStr = response.body() ?: emptyList()
                val locais = feriadosStr.mapNotNull { 
                    try {
                        LocalDate.parse(it.date, DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        null
                    }
                }.toSet()
                feriadosPorAno[ano] = locais
                locais
            } else {
                emptySet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }
}
