package com.obiterjus.data.processo

import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.processo.local.ProcessoEntity
import java.text.Normalizer

/**
 * Política de resolução de dados do processo entre as fontes (DataJud, DJEN e
 * edições do usuário): um valor já conhecido nunca é substituído por nulo, e
 * participantes de fontes diferentes são reconciliados por identidade
 * normalizada (nome sem acentos/caixa + categoria) em vez de igualdade exata.
 */
object ProcessoDadosResolver {

    /**
     * Combina o processo já persistido com a versão recém-obtida de uma fonte.
     * Campos não informados pela fonte nova preservam o valor existente —
     * inclusive os campos expandidos preenchidos manualmente pelo usuário,
     * que nenhuma fonte automática fornece.
     */
    fun mesclarProcesso(existente: ProcessoEntity?, novo: ProcessoEntity): ProcessoEntity {
        if (existente == null) return novo
        // Migração de instância: quando o novo snapshot vem de OUTRO tribunal
        // (ex.: STJ após o TJMG), os campos que descrevem a instância — grau,
        // classe e órgão julgador — não podem ser preenchidos com os valores
        // da instância anterior; ficam como o novo tribunal os reportou.
        val mudouInstancia = novo.tribunal != null && existente.tribunal != null &&
            !novo.tribunal.equals(existente.tribunal, ignoreCase = true)
        return novo.copy(
            tribunal = novo.tribunal ?: existente.tribunal,
            grau = if (mudouInstancia) novo.grau else novo.grau ?: existente.grau,
            classeCodigo = if (mudouInstancia) novo.classeCodigo else novo.classeCodigo ?: existente.classeCodigo,
            classeNome = if (mudouInstancia) novo.classeNome else novo.classeNome ?: existente.classeNome,
            assuntosJson = novo.assuntosJson ?: existente.assuntosJson,
            orgaoJulgadorCodigo = if (mudouInstancia) novo.orgaoJulgadorCodigo else novo.orgaoJulgadorCodigo ?: existente.orgaoJulgadorCodigo,
            orgaoJulgadorNome = if (mudouInstancia) novo.orgaoJulgadorNome else novo.orgaoJulgadorNome ?: existente.orgaoJulgadorNome,
            nivelSigilo = novo.nivelSigilo ?: existente.nivelSigilo,
            dataAjuizamento = novo.dataAjuizamento ?: existente.dataAjuizamento,
            capturadoEm = minOf(existente.capturadoEm, novo.capturadoEm),
            dataDistribuicao = novo.dataDistribuicao ?: existente.dataDistribuicao,
            comarcaSecao = novo.comarcaSecao ?: existente.comarcaSecao,
            juizo = novo.juizo ?: existente.juizo,
            prioridadeTramitacao = novo.prioridadeTramitacao ?: existente.prioridadeTramitacao,
            gratuidadeJustica = novo.gratuidadeJustica ?: existente.gratuidadeJustica,
            valorCausa = novo.valorCausa ?: existente.valorCausa,
            faseProcessual = novo.faseProcessual ?: existente.faseProcessual,
            situacaoAtual = novo.situacaoAtual ?: existente.situacaoAtual,
            tutelaAntecipadaLiminar = novo.tutelaAntecipadaLiminar ?: existente.tutelaAntecipadaLiminar,
            advogadosAtivo = novo.advogadosAtivo ?: existente.advogadosAtivo,
            advogadosPassivo = novo.advogadosPassivo ?: existente.advogadosPassivo,
            defensoriaPublica = novo.defensoriaPublica ?: existente.defensoriaPublica,
            ministerioPublico = novo.ministerioPublico ?: existente.ministerioPublico,
            terceirosAuxiliares = novo.terceirosAuxiliares ?: existente.terceirosAuxiliares,
            // Natureza confirmada/corrigida pelo usuário vence a inferência nova.
            natureza = existente.natureza ?: novo.natureza,
        )
    }

    /**
     * Reconcilia os participantes já persistidos com os vindos de uma fonte.
     * A ordem importa: [existentes] são a base — mantêm o idLocal e os campos
     * de qualificação já preenchidos (CPF, endereço, telefone…); os [novos]
     * apenas completam lacunas ou entram como registros inéditos.
     */
    fun mesclarParticipantes(
        existentes: List<ParticipanteEntity>,
        novos: List<ParticipanteEntity>,
    ): List<ParticipanteEntity> {
        val porChave = LinkedHashMap<String, ParticipanteEntity>()
        (existentes + novos).forEach { candidato ->
            val normalizado = candidato.copy(polo = normalizarPolo(candidato.polo))
            val chave = normalizado.chaveIdentidade()
            val base = porChave[chave]
            porChave[chave] = if (base == null) normalizado else mesclarParticipante(base, normalizado)
        }
        return porChave.values.sortedBy { it.chaveIdentidade() }
    }

    private fun mesclarParticipante(
        base: ParticipanteEntity,
        novo: ParticipanteEntity,
    ): ParticipanteEntity =
        base.copy(
            polo = base.polo ?: novo.polo,
            nome = base.nome.ouEntao(novo.nome),
            tipoPessoa = base.tipoPessoa.ouEntao(novo.tipoPessoa),
            tipoParticipacao = base.tipoParticipacao.ouEntao(novo.tipoParticipacao),
            // Marcação do usuário vence a captura automática.
            ehCliente = base.ehCliente || novo.ehCliente,
            cpfCnpj = base.cpfCnpj.ouEntao(novo.cpfCnpj),
            estadoCivil = base.estadoCivil.ouEntao(novo.estadoCivil),
            profissao = base.profissao.ouEntao(novo.profissao),
            endereco = base.endereco.ouEntao(novo.endereco),
            contatos = base.contatos.ouEntao(novo.contatos),
            cep = base.cep.ouEntao(novo.cep),
            logradouro = base.logradouro.ouEntao(novo.logradouro),
            numeroEndereco = base.numeroEndereco.ouEntao(novo.numeroEndereco),
            telefone = base.telefone.ouEntao(novo.telefone),
            email = base.email.ouEntao(novo.email),
        )

    private fun ParticipanteEntity.chaveIdentidade(): String {
        val nomeNormalizado = normalizarNome(nome)
        // Sem nome não há como reconciliar entre fontes: mantém o registro
        // isolado pela sua própria chave primária.
        if (nomeNormalizado.isEmpty()) return "$numeroProcesso|ID|$idLocal"
        return "$numeroProcesso|${categoria()}|$nomeNormalizado"
    }

    private fun ParticipanteEntity.categoria(): String {
        val tipo = normalizarNome(tipoParticipacao)
        return if (tipo.startsWith("ADVOGAD")) "ADVOGADO" else "PARTE"
    }

    fun normalizarPolo(polo: String?): String? {
        val normalizado = normalizarNome(polo)
        return when (normalizado) {
            "" -> null
            "A", "AT", "ATIVO", "ATIVA", "POLO ATIVO" -> "ATIVO"
            "P", "PA", "PASSIVO", "PASSIVA", "POLO PASSIVO" -> "PASSIVO"
            else -> normalizado
        }
    }

    fun normalizarNome(valor: String?): String {
        if (valor.isNullOrBlank()) return ""
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
            .replace(MARCAS_DIACRITICAS, "")
            .uppercase()
            .replace(ESPACOS, " ")
            .trim()
    }

    private fun String?.ouEntao(outro: String?): String? =
        this?.takeIf { it.isNotBlank() } ?: outro?.takeIf { it.isNotBlank() }

    private val MARCAS_DIACRITICAS = Regex("""\p{M}+""")
    private val ESPACOS = Regex("""\s+""")
}
