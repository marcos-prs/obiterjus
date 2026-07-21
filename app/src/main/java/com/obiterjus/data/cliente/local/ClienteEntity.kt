package com.obiterjus.data.cliente.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Cliente como entidade própria — a qualificação vive aqui, não replicada em
 * cada processo. [nomeNormalizado] e [documentoNormalizado] existem para o
 * dedupe no momento em que o usuário marca uma parte como cliente.
 */
@Entity(
    tableName = "clientes",
    indices = [
        // Vários clientes podem não ter documento informado: no SQLite cada
        // NULL é distinto, então o índice único não atrapalha esse caso.
        Index(value = ["documentoNormalizado"], unique = true),
        Index("nomeNormalizado"),
    ],
)
data class ClienteEntity(
    @PrimaryKey
    val id: String,
    val tipoPessoa: String,
    /** Nome civil da pessoa física ou razão social da jurídica. */
    val nome: String,
    /** [nome] sem acentos, em caixa alta — casa com a chave do participante. */
    val nomeNormalizado: String,
    /** CPF/CNPJ como o usuário digitou, para exibição. */
    val documento: String? = null,
    /** Somente dígitos — é a chave natural de deduplicação. */
    val documentoNormalizado: String? = null,
    // Qualificação da pessoa física; numa PJ estes campos ficam no representante.
    val nacionalidade: String? = null,
    val estadoCivil: String? = null,
    val profissao: String? = null,
    // Endereço estruturado completo — a minuta precisa dele por extenso, então
    // aqui há mais campos do que o participante captura hoje.
    val cep: String? = null,
    val logradouro: String? = null,
    val numeroEndereco: String? = null,
    val complemento: String? = null,
    val bairro: String? = null,
    val municipio: String? = null,
    val uf: String? = null,
    // Contato
    val telefone: String? = null,
    val email: String? = null,
    val observacoes: String? = null,
    /** Preenchido apenas em pessoa jurídica; nulo quando todos os campos o são. */
    @Embedded(prefix = "rep_")
    val representante: RepresentanteLegalEmbutido? = null,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
)

/**
 * Representante legal de uma pessoa jurídica. Carrega qualificação completa
 * porque a minuta a exige por extenso: "…neste ato representada por Fulano,
 * brasileiro, casado, administrador, portador do CPF…".
 *
 * Um único representante por cliente: o caso de mais de um ("representada por
 * seus sócios A e B") é raro o bastante para não pagar uma tabela agora.
 */
data class RepresentanteLegalEmbutido(
    val nome: String? = null,
    val documento: String? = null,
    val nacionalidade: String? = null,
    val estadoCivil: String? = null,
    val profissao: String? = null,
    val cargo: String? = null,
)
