package com.obiterjus.core.parser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SigiloDetectorTest {
    @Test
    fun detectsExplicitDjenSecrecyMessages() {
        assertTrue(SigiloDetector.isSigiloso("Processo sob sigilo"))
        assertTrue(SigiloDetector.isSigiloso("Texto omitido conforme legislação aplicável"))
    }

    @Test
    fun detectsDataJudSecrecyLevel() {
        assertTrue(SigiloDetector.isSigiloso("Publicação comum", nivelSigilo = 1))
    }

    @Test
    fun commonTextIsNotSecret() {
        assertFalse(SigiloDetector.isSigiloso("Intime-se a parte autora."))
    }
}
