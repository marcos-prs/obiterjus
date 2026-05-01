package com.obiterjus.domain.usecase

import com.obiterjus.core.parser.DjenPrazoExtractor
import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.domain.model.PublicacaoPrazo
import java.time.LocalDate

class CalcularPrazoRegraUC(
    private val calculadoraPrazos: CalculadoraPrazos,
    private val djenPrazoExtractor: DjenPrazoExtractor
) {
    suspend fun invoke(
        texto: String?,
        tipoComunicacao: String?,
        dataDisponibilizacao: LocalDate?
    ): PublicacaoPrazo? {
        if (dataDisponibilizacao == null) return null

        // Tenta extrair explicitamente do texto primeiro (via regex)
        val prazoExtraido = djenPrazoExtractor.extract(texto, dataDisponibilizacao)
        if (prazoExtraido != null) {
            return prazoExtraido
        }

        // Regras de fallback legais com base no tipo de comunicação
        val tipo = tipoComunicacao?.lowercase()?.trim() ?: return null
        
        val quantidade: Int
        val unidade = "dias"
        val diasUteis = true

        when {
            tipo.contains("citação") || tipo.contains("citacao") -> quantidade = 15
            tipo.contains("agravo") -> quantidade = 15
            tipo.contains("embargos de declaração") || tipo.contains("embargos de declaracao") -> quantidade = 5
            tipo.contains("apelação") || tipo.contains("apelacao") -> quantidade = 15
            tipo.contains("recurso inominado") -> quantidade = 10
            else -> return null // Não consegue inferir um prazo seguro
        }

        val dataLimite = calculadoraPrazos.calcularDataLimite(
            dataBase = dataDisponibilizacao,
            quantidade = quantidade,
            unidade = unidade,
            diasUteis = diasUteis
        )

        return PublicacaoPrazo(
            quantidade = quantidade,
            unidade = unidade,
            diasUteis = diasUteis,
            textoOriginal = "Inferido a partir do tipo: $tipoComunicacao",
            dataLimiteEstimada = dataLimite,
            isConfirmado = false,
            idExternoCalendario = null,
            provedorCalendario = null
        )
    }
}
