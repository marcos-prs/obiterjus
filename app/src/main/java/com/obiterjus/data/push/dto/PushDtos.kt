package com.obiterjus.data.push.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Registro da pré-checagem no servidor (Fase 2 do push diário).
 *
 * Não existe DTO para a Fase 1: lá o aparelho só se inscreve num tópico do FCM
 * e o servidor empurra para o tópico no horário. Nenhum registro, nenhuma
 * chamada, nenhum dado nosso no servidor.
 *
 * Aqui também **não há token**: o `firebase-messaging` 26 depreciou
 * `getToken()`, e o aparelho é endereçado pelo [topico] em que ele mesmo se
 * inscreveu. O servidor nunca guarda credencial de entrega.
 */
@Serializable
data class PushConsultaRequestDto(
    /**
     * Identificador opaco deste aparelho. É a chave da linha no servidor e a
     * capacidade para alterá-la: sem ele, quem soubesse a OAB poderia desligar
     * a pré-checagem de outro advogado.
     */
    @SerialName("instalacao")
    val instalacao: String,
    /**
     * Tópico do FCM para onde o servidor empurra. Derivado da OAB por hash com
     * sal, para o número não transitar nos metadados do Google.
     */
    @SerialName("topico")
    val topico: String,
    @SerialName("hora_local")
    val horaLocal: Int,
    @SerialName("offset_minutos")
    val offsetMinutos: Int,
    @SerialName("numero_oab")
    val numeroOab: String,
    @SerialName("uf_oab")
    val ufOab: String,
    @SerialName("nome_advogado")
    val nomeAdvogado: String? = null,
    @SerialName("apenas_por_nome")
    val apenasPorNome: Boolean,
    /**
     * Maior `id` de comunicação do DJEN já conhecido por este aparelho. O
     * servidor guarda só este inteiro — nada do conteúdo.
     *
     * Dois aparelhos na mesma OAB mandam marcas diferentes e caem no mesmo
     * tópico; o servidor deve usar a **menor** das marcas da OAB, para que
     * nenhum dos dois perca publicação.
     */
    @SerialName("marca_dagua")
    val marcaDagua: Long,
)
