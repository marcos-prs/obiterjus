package com.obiterjus.core.worker

import org.junit.Assert.assertEquals
import org.junit.Test

class DecidirAposFalhaTest {

    @Test
    fun periodicaNuncaUsaRetryDoWorkManager() {
        // Retry na periódica troca as 7h do dia seguinte por "falha + backoff".
        // Vale também para uma periódica que herdou tentativas de antes desta
        // regra (VC3 usava retry): o contador dela não importa.
        for (tentativa in 0..5) {
            assertEquals(
                AposFalha.RETENTATIVA_AVULSA,
                decidirAposFalha(origemPush = false, origemRetentativa = false, runAttemptCount = tentativa),
            )
        }
    }

    @Test
    fun retentativaContaAPeriodicaComoPrimeiraTentativa() {
        val decisoes = (0..2).map {
            decidirAposFalha(origemPush = false, origemRetentativa = true, runAttemptCount = it)
        }

        assertEquals(
            listOf(AposFalha.NOVA_TENTATIVA, AposFalha.NOVA_TENTATIVA, AposFalha.DESISTIR),
            decisoes,
        )
    }

    @Test
    fun pushMantemAsTresRetentativasDeAntes() {
        val decisoes = (0..3).map {
            decidirAposFalha(origemPush = true, origemRetentativa = false, runAttemptCount = it)
        }

        assertEquals(
            listOf(
                AposFalha.NOVA_TENTATIVA,
                AposFalha.NOVA_TENTATIVA,
                AposFalha.NOVA_TENTATIVA,
                AposFalha.DESISTIR,
            ),
            decisoes,
        )
    }

    @Test
    fun diariaEPushFazemOMesmoTotalDeExecucoes() {
        val execucoesDoPush = execucoesAteDesistir(origemPush = true, origemRetentativa = false)
        // A periódica que falhou + as execuções da retentativa avulsa.
        val execucoesDaDiaria = 1 + execucoesAteDesistir(origemPush = false, origemRetentativa = true)

        assertEquals(1 + MAX_RETENTATIVAS, execucoesDoPush)
        assertEquals(execucoesDoPush, execucoesDaDiaria)
    }

    private fun execucoesAteDesistir(origemPush: Boolean, origemRetentativa: Boolean): Int {
        var tentativa = 0
        while (decidirAposFalha(origemPush, origemRetentativa, tentativa) == AposFalha.NOVA_TENTATIVA) {
            tentativa++
        }
        return tentativa + 1
    }
}
