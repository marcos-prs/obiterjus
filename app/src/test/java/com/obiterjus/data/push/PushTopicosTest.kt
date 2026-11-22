package com.obiterjus.data.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrato com o emissor (`push/fcm.py`, no repositório `Obiter-Push`).
 *
 * O risco que estes testes cobrem é silencioso: se os dois lados derivarem
 * nomes diferentes, o push vai para um tópico sem ninguém inscrito, **o FCM
 * responde sucesso e nada falha em lugar nenhum**. O advogado simplesmente não
 * é avisado.
 *
 * Os vetores abaixo são os mesmos afirmados em `tests/test_fcm.py`, lá.
 */
class PushTopicosTest {

    @Test
    fun topicoCegoDeBrasilia() {
        assertEquals("sync-diaria-h07-utcm180", PushTopicos.acordaCego(7, -180))
    }

    @Test
    fun topicoCegoDoAcre() {
        assertEquals("sync-diaria-h07-utcm300", PushTopicos.acordaCego(7, -300))
    }

    @Test
    fun offsetZeroUsaP() {
        assertEquals("sync-diaria-h07-utcp0", PushTopicos.acordaCego(7, 0))
    }

    @Test
    fun offsetPositivoUsaP() {
        assertEquals("sync-diaria-h07-utcp330", PushTopicos.acordaCego(7, 330))
    }

    @Test
    fun topicoPorOabBateComOVetorDoServidor() {
        assertEquals(
            "oab-0a43aa23415f38a27776fca5",
            PushTopicos.porOab("123456", "RS", SAL),
        )
    }

    @Test
    fun ufMinusculaEEspacoDaoOMesmoTopico() {
        assertEquals(
            PushTopicos.porOab("123456", "RS", SAL),
            PushTopicos.porOab(" 123456 ", "rs", SAL),
        )
    }

    @Test
    fun oabDiferenteDaTopicoDiferente() {
        assertNotEquals(
            PushTopicos.porOab("123456", "RS", SAL),
            PushTopicos.porOab("123456", "SP", SAL),
        )
    }

    @Test
    fun salDiferenteDaTopicoDiferente() {
        // É por isso que a PUSH_APP_KEY do servidor tem de ser a mesma que
        // compilou o APK: divergir manda o push para um tópico vazio.
        assertNotEquals(
            PushTopicos.porOab("123456", "RS", SAL),
            PushTopicos.porOab("123456", "RS", "outro-sal"),
        )
    }

    @Test
    fun nomeAceitoPeloFcm() {
        val topicos = listOf(
            PushTopicos.acordaCego(7, -720),
            PushTopicos.acordaCego(7, 840),
            PushTopicos.porOab("123456", "RS", SAL),
        )

        topicos.forEach { topico ->
            assertTrue(topico, Regex("[a-zA-Z0-9\\-_.~%]+").matches(topico))
        }
        assertEquals("oab-".length + 24, PushTopicos.porOab("123456", "RS", SAL).length)
    }

    private companion object {
        const val SAL = "sal-de-teste"
    }
}
