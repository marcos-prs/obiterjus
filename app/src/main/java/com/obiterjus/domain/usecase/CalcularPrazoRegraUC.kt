package com.obiterjus.domain.usecase

import com.obiterjus.core.parser.DjenDecisaoClassifier
import com.obiterjus.core.parser.DjenPrazoExtractor
import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.domain.model.ConfiancaCalculo
import com.obiterjus.domain.model.NaturezaProcesso
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.model.toConfianca
import java.time.LocalDate

class CalcularPrazoRegraUC(
    private val calculadoraPrazos: CalculadoraPrazos,
    private val djenPrazoExtractor: DjenPrazoExtractor
) {
    suspend fun invoke(
        texto: String?,
        tipoComunicacao: String?,
        dataDisponibilizacao: LocalDate?,
        tribunal: String? = null,
        natureza: NaturezaProcesso? = null,
    ): PublicacaoPrazo? {
        if (dataDisponibilizacao == null) return null

        // Tenta extrair explicitamente do texto primeiro (via regex)
        val prazoExtraido = djenPrazoExtractor.extract(texto, dataDisponibilizacao, tribunal, natureza)
        if (prazoExtraido != null) {
            return prazoExtraido
        }

        // Decisões que não declaram prazo mas ensejam recurso previsto em lei
        // (ex.: "não conheço do agravo em REsp" → agravo interno). O app só
        // identifica o padrão; o número de dias vem do ruleset da API.
        // Padrões são do CPC — não se aplica ao penal.
        if (natureza != NaturezaProcesso.PENAL) {
            val sugestao = DjenDecisaoClassifier.classificar(texto)
            if (sugestao != null) {
                val ruleset = calculadoraPrazos.calcularPrazoPorRuleset(
                    dataBase = dataDisponibilizacao,
                    artigo = sugestao.artigoCpc,
                    tribunal = tribunal,
                    natureza = natureza,
                )
                if (ruleset != null) {
                    return PublicacaoPrazo(
                        quantidade = ruleset.quantidade,
                        unidade = "dias",
                        diasUteis = ruleset.diasUteis,
                        textoOriginal = "${sugestao.recurso} — sugerido a partir da decisão",
                        dataLimiteEstimada = ruleset.dataVencimento,
                        // Inferência jurídica nunca sai CONFIAVEL: exige o
                        // aceite do advogado (isConfirmado).
                        confiancaCalculo = ConfiancaCalculo.INCERTO,
                        isConfirmado = false,
                    )
                }
                // API indisponível → sem sugestão agora; o próximo sync tenta
                // de novo (prazo segue nulo, publicação continua elegível).
                return null
            }
        }

        // Regras de fallback com base no tipo de comunicação — números do CPC.
        // Em processo penal os prazos por tipo são outros (ex.: apelação CPP
        // art. 593 = 5 dias), então não se infere: sem prazo seguro.
        if (natureza == NaturezaProcesso.PENAL) return null

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

        val resultado = calculadoraPrazos.calcularDataLimite(
            dataBase = dataDisponibilizacao,
            quantidade = quantidade,
            unidade = unidade,
            diasUteis = diasUteis,
            tribunal = tribunal,
            natureza = natureza,
        )

        return PublicacaoPrazo(
            quantidade = quantidade,
            unidade = unidade,
            diasUteis = diasUteis,
            textoOriginal = "Inferido a partir do tipo: $tipoComunicacao",
            dataLimiteEstimada = resultado.data,
            confiancaCalculo = resultado.toConfianca(),
            isConfirmado = false,
            idExternoCalendario = null,
            provedorCalendario = null,
        )
    }
}
