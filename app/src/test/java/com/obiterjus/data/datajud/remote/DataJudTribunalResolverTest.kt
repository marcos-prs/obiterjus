package com.obiterjus.data.datajud.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataJudTribunalResolverTest {
    private val resolver = DataJudTribunalResolver()

    @Test
    fun resolvesExplicitTribunalSigla() {
        assertEquals(
            ResolvedDataJudTribunal("TJMG", "api_publica_tjmg"),
            resolver.resolve("tj-mg", "50110879520258130245"),
        )
        assertEquals(
            ResolvedDataJudTribunal("STJ", "api_publica_stj"),
            resolver.resolve("stj", null),
        )
    }

    @Test
    fun infersStateCourtFromCnjNumberWhenTribunalIsMissing() {
        assertEquals(
            ResolvedDataJudTribunal("TJMG", "api_publica_tjmg"),
            resolver.resolve(null, "50110879520258130245"),
        )
    }

    @Test
    fun returnsNullWhenTribunalIsUnsupportedAndNumberCannotBeInferred() {
        assertNull(resolver.resolve("abc", "00000000020269000000"))
    }
}
