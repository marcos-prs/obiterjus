package com.obiterjus.data.sincronizacao

import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.data.cliente.local.ClienteEntity
import com.obiterjus.data.cliente.local.ClienteProcessoEntity
import com.obiterjus.data.cliente.local.RepresentanteLegalEmbutido
import com.obiterjus.data.datajud.local.MovimentoEntity
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.minuta.local.MinutaEntity
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import com.obiterjus.domain.model.ConfiancaMatch
import com.obiterjus.domain.model.ProcessoSyncStatus
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cada entidade é preenchida com valores distintos e não-default em TODOS os
 * campos, convertida para o DTO do Firestore e de volta. Um campo que exista na
 * entidade e falte no DTO volta com o valor padrão e quebra a comparação.
 *
 * Isto existe porque a sincronização já perdeu campos duas vezes em silêncio:
 * os 14 campos expandidos de ProcessoEntity e os 10 de qualificação de
 * ParticipanteEntity eram zerados a cada restauração da nuvem. Ao acrescentar
 * um campo numa entidade sincronizada, este teste falha até o DTO acompanhar.
 */
class FirestoreDtosRoundTripTest {

    // O Timestamp do Firestore guarda microssegundos; nanos precisam ser
    // múltiplos de 1000 para o ida-e-volta fechar.
    private val instante = Instant.ofEpochSecond(1_755_000_000L, 123_000L)
    private val outroInstante = Instant.ofEpochSecond(1_744_000_000L, 456_000L)

    @Test
    fun `processo preserva todos os campos`() {
        val original = ProcessoEntity(
            numeroProcesso = "0000001-11.2024.8.26.0100",
            tribunal = "TJSP",
            grau = "G1",
            classeCodigo = 7,
            classeNome = "Procedimento Comum",
            assuntosJson = """["Dano moral"]""",
            orgaoJulgadorCodigo = 42,
            orgaoJulgadorNome = "1ª Vara Cível",
            nivelSigilo = 1,
            dataAjuizamento = instante,
            syncStatus = ProcessoSyncStatus.SYNCED,
            capturadoEm = instante,
            atualizadoEm = outroInstante,
            dataJudTentativasRestantes = 3,
            dataDistribuicao = outroInstante,
            comarcaSecao = "São Paulo",
            juizo = "1ª Vara",
            prioridadeTramitacao = "Idoso",
            gratuidadeJustica = "Deferida",
            valorCausa = 12_345.67,
            faseProcessual = "Conhecimento",
            situacaoAtual = "Concluso",
            tutelaAntecipadaLiminar = "Indeferida",
            advogadosAtivo = "Fulano OAB/SP 1",
            advogadosPassivo = "Beltrano OAB/SP 2",
            defensoriaPublica = "Não",
            ministerioPublico = "Custos legis",
            terceirosAuxiliares = "Perito",
            natureza = "CIVEL",
        )

        assertEquals(original, original.toFirestoreDto().toEntity())
    }

    @Test
    fun `publicacao preserva todos os campos`() {
        val original = PublicacaoEntity(
            id = 987L,
            hash = "abc123",
            numeroProcesso = "0000001-11.2024.8.26.0100",
            participantesJson = """[{"nome":"Fulano"}]""",
            prazoQuantidade = 15,
            prazoUnidade = "dias",
            prazoDiasUteis = true,
            prazoTexto = "prazo de 15 dias",
            prazoDataLimite = LocalDate.of(2026, 3, 10),
            prazoConfianca = "CONFIAVEL",
            dataDisponibilizacao = LocalDate.of(2026, 2, 20),
            tribunal = "TJSP",
            tipoComunicacao = "Intimação",
            nomeOrgao = "1ª Vara Cível",
            idOrgao = 55L,
            textoRaw = "<p>texto</p>",
            textoLimpo = "texto",
            textoPossuiHtml = true,
            textoPossuiErroTemplate = true,
            isSigiloso = true,
            ativo = false,
            fonte = "DJEN",
            capturadoEm = instante,
            atualizadoEm = outroInstante,
            confiancaMatch = ConfiancaMatch.BAIXA,
            duplicataDe = 986L,
            totalDuplicatas = 2,
        )

        assertEquals(original, original.toFirestoreDto().toEntity())
    }

    @Test
    fun `participante preserva a qualificacao completa`() {
        val original = ParticipanteEntity(
            idLocal = "part-1",
            numeroProcesso = "0000001-11.2024.8.26.0100",
            polo = "ATIVO",
            nome = "Fulano de Tal",
            tipoPessoa = "FISICA",
            tipoParticipacao = "AUTOR",
            ehCliente = true,
            cpfCnpj = "12345678901",
            estadoCivil = "Casado",
            profissao = "Engenheiro",
            endereco = "Rua A, 1",
            contatos = "11999999999",
            cep = "01001000",
            logradouro = "Rua A",
            numeroEndereco = "1",
            telefone = "11999999999",
            email = "fulano@exemplo.com",
        )

        assertEquals(original, original.toFirestoreDto().toEntity())
    }

    @Test
    fun `movimento preserva todos os campos`() {
        val original = MovimentoEntity(
            idLocal = "mov-1",
            numeroProcesso = "0000001-11.2024.8.26.0100",
            codigo = 123,
            nome = "Conclusão",
            dataHora = instante,
            complementosJson = """["x"]""",
        )

        assertEquals(original, original.toFirestoreDto().toEntity())
    }

    @Test
    fun `cliente preserva todos os campos incluindo representante`() {
        val original = ClienteEntity(
            id = "cli-1",
            tipoPessoa = "JURIDICA",
            nome = "Empresa X Ltda",
            nomeNormalizado = "EMPRESA X LTDA",
            documento = "12.345.678/0001-90",
            documentoNormalizado = "12345678000190",
            nacionalidade = "Brasileira",
            estadoCivil = "N/A",
            profissao = "N/A",
            cep = "01001000",
            logradouro = "Rua A",
            numeroEndereco = "100",
            complemento = "Sala 2",
            bairro = "Centro",
            municipio = "São Paulo",
            uf = "SP",
            telefone = "1133334444",
            email = "contato@empresax.com",
            observacoes = "Cliente antigo",
            representante = RepresentanteLegalEmbutido(
                nome = "Sicrano",
                documento = "98765432100",
                nacionalidade = "Brasileiro",
                estadoCivil = "Solteiro",
                profissao = "Administrador",
                cargo = "Sócio",
            ),
            criadoEm = instante,
            atualizadoEm = outroInstante,
        )

        assertEquals(original, original.toFirestoreDto().toEntity())
    }

    @Test
    fun `vinculo cliente processo preserva todos os campos`() {
        val original = ClienteProcessoEntity(
            clienteId = "cli-1",
            numeroProcesso = "0000001-11.2024.8.26.0100",
            participanteIdLocal = "part-1",
            vinculadoEm = instante,
        )

        assertEquals(original, original.toFirestoreDto().toEntity())
    }

    @Test
    fun `prazo sugerido preserva todos os campos exceto o id autogerado`() {
        val original = PrazoSugeridoEntity(
            id = 0L,
            publicacaoId = 987L,
            quantidade = 15,
            unidade = "dias",
            diasUteis = true,
            textoOriginal = "prazo de 15 dias",
            dataLimite = LocalDate.of(2026, 3, 10),
            isConfirmado = true,
            idExternoCalendario = "evt-1",
            provedorCalendario = "google",
            isCumprido = true,
        )

        assertEquals(original, original.toFirestoreDto().toEntity())
    }

    @Test
    fun `minuta preserva todos os campos`() {
        val original = MinutaEntity(
            sugestaoId = "sug-1",
            numeroProcesso = "0000001-11.2024.8.26.0100",
            tipoMinuta = "contestacao",
            titulo = "Contestação",
            confiancaGeral = 0.87,
            requerRevisao = true,
            geradoEm = instante,
            payloadJson = """{"sugestao_id":"sug-1"}""",
        )

        assertEquals(original, original.toFirestoreDto().toEntity())
    }
}
