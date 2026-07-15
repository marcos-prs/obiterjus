package com.obiterjus.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DjenDecisaoClassifierTest {

    @Test
    fun identificaAgravoInternoEmNaoConhecimentoDeAResp() {
        // Dispositivo real de decisão monocrática do STJ (AREsp), que cita
        // ementa de precedente contendo "Acórdão" — a citação não pode
        // afastar a classificação.
        val texto = """
            DECISÃO
            Cuida-se de Agravo em Recurso Especial apresentado à decisão que
            inadmitiu Recurso Especial interposto com fundamento no art. 105,
            III, da Constituição Federal.
            (EAREsp 746.775/PR, Rel. Ministro João Otávio de Noronha, Rel. p/
            Acórdão Ministro Luis Felipe Salomão, Corte Especial, DJe de 30.11.2018.)
            Ante o exposto, com base no art. 21-E, V, c/c o art. 253, parágrafo
            único, I, ambos do Regimento Interno do Superior Tribunal de Justiça,
            não conheço do Agravo em Recurso Especial.
            Publique-se. Intimem-se.
        """.trimIndent()

        val sugestao = DjenDecisaoClassifier.classificar(texto)

        assertEquals("1070", sugestao?.artigoCpc)
    }

    @Test
    fun identificaNegativaDeProvimentoMonocratica() {
        val sugestao = DjenDecisaoClassifier.classificar(
            "Ante o exposto, nego provimento ao Agravo em Recurso Especial.",
        )
        assertEquals("1070", sugestao?.artigoCpc)
    }

    @Test
    fun naoSugereParaJulgamentoColegiado() {
        // Voz colegiada — não é decisão unipessoal; agravo interno não cabe.
        val sugestao = DjenDecisaoClassifier.classificar(
            "A Turma, por unanimidade, negou provimento ao agravo interno, " +
                "nos termos do voto do relator.",
        )
        assertNull(sugestao)
    }

    @Test
    fun naoSugereParaTextoNeutro() {
        assertNull(
            DjenDecisaoClassifier.classificar(
                "Processo distribuído pelo sistema automático em 10/06/2026.",
            ),
        )
    }
}
