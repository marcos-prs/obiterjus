package com.obiterjus.domain.model

import java.time.Instant

data class ProcessoMonitorado(
    val numeroProcesso: String,
    val tribunal: String?,
    val grau: String?,
    val classeCodigo: Int?,
    val classeNome: String?,
    val assuntos: List<String>,
    val orgaoJulgadorCodigo: Int?,
    val orgaoJulgadorNome: String?,
    val nivelSigilo: Int?,
    val dataAjuizamento: Instant?,
    val syncStatus: ProcessoSyncStatus,
    val capturadoEm: Instant,
    val atualizadoEm: Instant,
    val dataJudTentativasRestantes: Int = 0,
    val participantes: List<ParticipanteProcesso> = emptyList(),
    // Campos expandidos
    val dataDistribuicao: Instant? = null,
    val comarcaSecao: String? = null,
    val juizo: String? = null,
    val prioridadeTramitacao: String? = null,
    val gratuidadeJustica: String? = null,
    val valorCausa: Double? = null,
    val faseProcessual: String? = null,
    val situacaoAtual: String? = null,
    val tutelaAntecipadaLiminar: String? = null,
    val advogadosAtivo: String? = null,
    val advogadosPassivo: String? = null,
    val defensoriaPublica: String? = null,
    val ministerioPublico: String? = null,
    val terceirosAuxiliares: String? = null,
    // Natureza para contagem de prazo (inferida da classe/assuntos CNJ,
    // corrigível pelo usuário); null = não declarada → tratada como cível.
    val natureza: NaturezaProcesso? = null,
)

data class ParticipanteProcesso(
    val idLocal: String,
    val numeroProcesso: String,
    val polo: String?,
    val nome: String?,
    val tipoPessoa: String?,
    val tipoParticipacao: String?,
    // Marcado pelo usuário como cliente que ele representa
    val ehCliente: Boolean = false,
    // Qualificação completa
    val cpfCnpj: String? = null,
    val estadoCivil: String? = null,
    val profissao: String? = null,
    val endereco: String? = null,
    val contatos: String? = null,
    // Endereço estruturado
    val cep: String? = null,
    val logradouro: String? = null,
    val numeroEndereco: String? = null,
    // Contatos estruturados
    val telefone: String? = null,
    val email: String? = null,
)

data class MovimentoProcesso(
    val idLocal: String,
    val numeroProcesso: String,
    val codigo: Int?,
    val nome: String?,
    val dataHora: Instant?,
    val complementosJson: String?,
)
