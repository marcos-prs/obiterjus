package com.obiterjus.presentation.participantes

import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.PublicacaoParticipante
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParticipanteUtilsTest {

    @Test
    fun `deve resolver autor e reu corretamente`() {
        val participantes = listOf(
            ParticipanteProcesso(idLocal = "1", numeroProcesso = "123", polo = "AUTOR", nome = "João Silva", tipoPessoa = "F", tipoParticipacao = "AUTOR"),
            ParticipanteProcesso(idLocal = "2", numeroProcesso = "123", polo = "REU", nome = "Maria Souza", tipoPessoa = "F", tipoParticipacao = "REU")
        )
        val resolvido = participantes.resolverPartesProcesso()
        
        assertEquals("João Silva", resolvido.ativa?.nomes?.first())
        assertEquals("Maria Souza", resolvido.passiva?.nomes?.first())
        assertEquals("João Silva x Maria Souza", resolvido.formatarConfronto())
    }

    @Test
    fun `deve lidar com acento em reu`() {
        val participantes = listOf(
            ParticipanteProcesso(idLocal = "1", numeroProcesso = "123", polo = "Réu", nome = "Maria Souza", tipoPessoa = "F", tipoParticipacao = "REU")
        )
        val resolvido = participantes.resolverPartesProcesso()
        
        assertEquals("Maria Souza", resolvido.passiva?.nomes?.first())
        assertEquals("Maria Souza", resolvido.formatarConfronto())
    }

    @Test
    fun `deve resolver requerente e requerido`() {
        val participantes = listOf(
            ParticipanteProcesso(idLocal = "1", numeroProcesso = "123", polo = "REQUERENTE", nome = "Empresa A", tipoPessoa = "J", tipoParticipacao = "REQUERENTE"),
            ParticipanteProcesso(idLocal = "2", numeroProcesso = "123", polo = "REQUERIDO", nome = "Empresa B", tipoPessoa = "J", tipoParticipacao = "REQUERIDO")
        )
        val resolvido = participantes.resolverPartesProcesso()
        
        assertEquals("Empresa A x Empresa B", resolvido.formatarConfronto())
    }

    @Test
    fun `deve resolver exequente e executado`() {
        val participantes = listOf(
            ParticipanteProcesso(idLocal = "1", numeroProcesso = "123", polo = "EXEQUENTE", nome = "Banco X", tipoPessoa = "J", tipoParticipacao = "EXEQUENTE"),
            ParticipanteProcesso(idLocal = "2", numeroProcesso = "123", polo = "EXECUTADO", nome = "Devedor Y", tipoPessoa = "F", tipoParticipacao = "EXECUTADO")
        )
        val resolvido = participantes.resolverPartesProcesso()
        
        assertEquals("Banco X x Devedor Y", resolvido.formatarConfronto())
    }

    @Test
    fun `deve agrupar multiplos nomes no mesmo polo`() {
        val participantes = listOf(
            ParticipanteProcesso(idLocal = "1", numeroProcesso = "123", polo = "ATIVO", nome = "Autor 1", tipoPessoa = "F", tipoParticipacao = "AUTOR"),
            ParticipanteProcesso(idLocal = "2", numeroProcesso = "123", polo = "ATIVO", nome = "Autor 2", tipoPessoa = "F", tipoParticipacao = "AUTOR"),
            ParticipanteProcesso(idLocal = "3", numeroProcesso = "123", polo = "PASSIVO", nome = "Reu 1", tipoPessoa = "F", tipoParticipacao = "REU")
        )
        val resolvido = participantes.resolverPartesProcesso()
        
        assertEquals("Autor 1, Autor 2 x Reu 1", resolvido.formatarConfronto())
    }

    @Test
    fun `deve ignorar advogados na resolucao de partes`() {
        val participantes = listOf(
            PublicacaoParticipante(nome = "Parte A", tipo = "Autor"),
            PublicacaoParticipante(nome = "Dr. Advogado", tipo = "Advogado")
        )
        val resolvido = participantes.resolverPartesPublicacao()
        
        assertEquals("Parte A", resolvido.formatarConfronto())
        assertEquals(listOf("Dr. Advogado"), resolvido.advogados)
    }

    @Test
    fun `deve retornar null para lista vazia`() {
        val participantes = emptyList<ParticipanteProcesso>()
        val resolvido = participantes.resolverPartesProcesso()
        
        assertNull(resolvido.formatarConfronto())
    }

    @Test
    fun `deve retornar null quando nenhum polo for identificado`() {
        val participantes = listOf(
            ParticipanteProcesso(idLocal = "1", numeroProcesso = "123", polo = "TESTEMUNHA", nome = "Ninguém", tipoPessoa = "F", tipoParticipacao = "TESTEMUNHA")
        )
        val resolvido = participantes.resolverPartesProcesso()

        assertNull(resolvido.formatarConfronto())
    }

    @Test
    fun `deve extrair advogados do processo`() {
        val participantes = listOf(
            ParticipanteProcesso(idLocal = "1", numeroProcesso = "123", polo = "ATIVO", nome = "Autor X", tipoPessoa = "F", tipoParticipacao = "AUTOR"),
            ParticipanteProcesso(idLocal = "2", numeroProcesso = "123", polo = "ATIVO", nome = "Dra. Advogada", tipoPessoa = "F", tipoParticipacao = "ADVOGADO"),
        )
        val resolvido = participantes.resolverPartesProcesso()

        assertEquals(listOf("Dra. Advogada"), resolvido.advogados)
        assertEquals("Autor X", resolvido.formatarConfronto())
    }

    @Test
    fun `advogado com polo ativo nao vira parte ativa`() {
        val participantes = listOf(
            ParticipanteProcesso(idLocal = "1", numeroProcesso = "123", polo = "ATIVO", nome = "Dr. Adv", tipoPessoa = "F", tipoParticipacao = "ADVOGADO"),
        )
        val resolvido = participantes.resolverPartesProcesso()

        assertNull(resolvido.formatarConfronto())
        assertEquals(listOf("Dr. Adv"), resolvido.advogados)
    }

    @Test
    fun `deve listar clientes marcados exceto advogados`() {
        val participantes = listOf(
            ParticipanteProcesso(idLocal = "1", numeroProcesso = "123", polo = "ATIVO", nome = "Cliente A", tipoPessoa = "F", tipoParticipacao = "AUTOR", ehCliente = true),
            ParticipanteProcesso(idLocal = "2", numeroProcesso = "123", polo = "PASSIVO", nome = "Parte B", tipoPessoa = "F", tipoParticipacao = "REU", ehCliente = false),
            ParticipanteProcesso(idLocal = "3", numeroProcesso = "123", polo = "ATIVO", nome = "Adv", tipoPessoa = "F", tipoParticipacao = "ADVOGADO", ehCliente = true),
        )
        val resolvido = participantes.resolverPartesProcesso()

        assertEquals(listOf("Cliente A"), resolvido.clientes)
    }

    @Test
    fun `poloDerivado mapeia papeis para o polo correto`() {
        assertEquals("ATIVO", TiposParticipacao.poloDerivado("Autor"))
        assertEquals("PASSIVO", TiposParticipacao.poloDerivado("Réu"))
        assertNull(TiposParticipacao.poloDerivado("Terceiro interessado"))
        assertNull(TiposParticipacao.poloDerivado("Advogado"))
    }

    @Test
    fun `rotuloEspecie retorna papel e ignora polo cru`() {
        assertEquals("Agravante", TiposParticipacao.rotuloEspecie("AGRAVANTE"))
        assertEquals("Réu", TiposParticipacao.rotuloEspecie("reu"))
        assertNull(TiposParticipacao.rotuloEspecie("ATIVO"))
        assertNull(TiposParticipacao.rotuloEspecie("AT"))
        assertNull(TiposParticipacao.rotuloEspecie(null))
    }
}
