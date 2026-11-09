package com.obiterjus.domain.usecase

import android.util.Log
import com.obiterjus.data.publicacao.local.PublicacaoDao
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.domain.model.ConfiancaCalculo
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

/**
 * Fecha as pendências de cálculo deixadas pela ingestão.
 *
 * A API CalendárioForense roda em plano gratuito e hiberna: publicação capturada
 * enquanto ela está fora sai com prazo em PENDENTE, sem data. Este caso de uso
 * varre essas publicações, refaz o cálculo pelo mesmo caminho da ingestão
 * ([CalcularPrazoRegraUC]) e **grava** o resultado.
 *
 * A gravação é o ponto: antes disso o recálculo acontecia na leitura da agenda,
 * a cada emissão do Flow, e o resultado era descartado — nunca convergia e ainda
 * duplicava a lógica de extração num segundo conjunto de regexes.
 *
 * Roda no [com.obiterjus.core.worker.DjenSyncWorker], após a sincronização, e no
 * [com.obiterjus.core.worker.PrazosWorker], antes de contar os prazos a vencer.
 * O que continuar pendente é tentado de novo no dia seguinte.
 */
class RecalcularPrazosPendentesUC(
    private val publicacaoDao: PublicacaoDao,
    private val calcularPrazoRegraUC: CalcularPrazoRegraUC,
    private val resolverNatureza: ResolverNaturezaProcessoUC? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    suspend operator fun invoke(
        janelaDias: Long = JANELA_DIAS,
        limite: Int = LIMITE_POR_EXECUCAO,
    ): ResumoRecalculo {
        val candidatas = publicacaoDao.getComPrazoPendente(
            dataMinima = LocalDate.now(clock).minusDays(janelaDias),
            limite = limite,
        )
        if (candidatas.isEmpty()) return ResumoRecalculo()

        var resolvidas = 0
        var falhas = 0
        var ignoradas = 0

        // Tribunal sem calendário judicial do ano responde sem data para todas
        // as publicações daquele ano (BLOQUEADO_SEM_CALENDARIO): a primeira já
        // disse o que as outras ouviriam, e são até 200 chamadas por execução
        // numa API que hiberna. O próximo ciclo pergunta de novo — a coleta do
        // calendário pode ter entrado nesse meio-tempo.
        val semDataNoCiclo = mutableSetOf<Pair<String, Int>>()

        candidatas.forEach { entity ->
            val tribunalAno = entity.tribunalAno()
            if (tribunalAno != null && tribunalAno in semDataNoCiclo) {
                ignoradas += 1
                return@forEach
            }
            try {
                val prazo = calcularPrazoRegraUC.invoke(
                    texto = entity.textoLimpo,
                    tipoComunicacao = entity.tipoComunicacao,
                    dataDisponibilizacao = entity.dataDisponibilizacao,
                    tribunal = entity.tribunal,
                    natureza = resolverNatureza?.invoke(entity.numeroProcesso),
                )
                // Sem data a publicação continua elegível: não adianta gravar
                // quantidade sem vencimento, e o próximo ciclo tenta de novo.
                val dataLimite = prazo?.dataLimiteEstimada
                if (dataLimite == null) {
                    // INCERTO sem data é resposta da API, não falha de rede
                    // (essa sai PENDENTE, e aí vale a pena insistir).
                    if (tribunalAno != null && prazo?.confiancaCalculo == ConfiancaCalculo.INCERTO) {
                        semDataNoCiclo += tribunalAno
                    }
                    return@forEach
                }

                publicacaoDao.atualizarPrazo(
                    id = entity.id,
                    quantidade = prazo.quantidade,
                    unidade = prazo.unidade,
                    diasUteis = prazo.diasUteis,
                    texto = prazo.textoOriginal,
                    dataLimite = dataLimite,
                    confianca = prazo.confiancaCalculo.name,
                )
                resolvidas += 1
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // Uma publicação problemática não pode abortar a varredura das demais.
                falhas += 1
                Log.w(TAG, "Recálculo falhou para publicação id=${entity.id}", error)
            }
        }

        Log.d(
            TAG,
            "Recálculo: analisadas=${candidatas.size} | resolvidas=$resolvidas | " +
                "falhas=$falhas | ignoradas=$ignoradas",
        )
        return ResumoRecalculo(
            analisadas = candidatas.size,
            resolvidas = resolvidas,
            falhas = falhas,
        )
    }

    private companion object {
        const val TAG = "RecalcularPrazos"

        /**
         * Publicação sem prazo no texto casa com a consulta indefinidamente.
         * A janela limita o retrabalho ao que ainda pode ter prazo em curso.
         */
        const val JANELA_DIAS = 90L

        /** Teto por execução: a API responde uma chamada por vez e hiberna. */
        const val LIMITE_POR_EXECUCAO = 200
    }
}

/** Chave do que a API já respondeu sobre calendário judicial: sigla + ano. */
private fun PublicacaoEntity.tribunalAno(): Pair<String, Int>? {
    val sigla = tribunal?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: return null
    val ano = dataDisponibilizacao?.year ?: return null
    return sigla to ano
}

data class ResumoRecalculo(
    val analisadas: Int = 0,
    val resolvidas: Int = 0,
    val falhas: Int = 0,
) {
    val pendentes: Int get() = analisadas - resolvidas - falhas
}
