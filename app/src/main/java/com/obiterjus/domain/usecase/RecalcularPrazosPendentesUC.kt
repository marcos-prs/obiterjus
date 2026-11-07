package com.obiterjus.domain.usecase

import android.util.Log
import com.obiterjus.data.publicacao.local.PublicacaoDao
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

        candidatas.forEach { entity ->
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
                val dataLimite = prazo?.dataLimiteEstimada ?: return@forEach

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
            "Recálculo: analisadas=${candidatas.size} | resolvidas=$resolvidas | falhas=$falhas",
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

data class ResumoRecalculo(
    val analisadas: Int = 0,
    val resolvidas: Int = 0,
    val falhas: Int = 0,
) {
    val pendentes: Int get() = analisadas - resolvidas - falhas
}
