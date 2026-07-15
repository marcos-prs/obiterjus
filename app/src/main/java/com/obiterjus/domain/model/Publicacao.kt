package com.obiterjus.domain.model

import com.obiterjus.core.time.ResultadoCalculoPrazo
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.Serializable

enum class ConfiancaCalculo { CONFIAVEL, INCERTO, PENDENTE }

fun ResultadoCalculoPrazo.toConfianca(): ConfiancaCalculo = when (this) {
    is ResultadoCalculoPrazo.Confiavel -> ConfiancaCalculo.CONFIAVEL
    is ResultadoCalculoPrazo.Incerto -> ConfiancaCalculo.INCERTO
    is ResultadoCalculoPrazo.Pendente -> ConfiancaCalculo.PENDENTE
}

data class Publicacao(
    val id: Long,
    val hash: String?,
    val numeroProcesso: String?,
    val participantes: List<PublicacaoParticipante>,
    val prazo: PublicacaoPrazo?,
    val dataDisponibilizacao: LocalDate?,
    val tribunal: String?,
    val tipoComunicacao: String?,
    val nomeOrgao: String?,
    val textoLimpo: String?,
    val isSigiloso: Boolean,
    val fonte: String,
    val capturadoEm: Instant,
    val atualizadoEm: Instant,
    val confiancaMatch: ConfiancaMatch = ConfiancaMatch.ALTA,
    val duplicataDe: Long? = null,
    val totalDuplicatas: Int = 0,
) {
    val isDuplicata: Boolean get() = duplicataDe != null
    val possuiDuplicatas: Boolean get() = totalDuplicatas > 0
}

@Serializable
data class PublicacaoParticipante(
    val tipo: String,
    val nome: String,
    val documento: String? = null,
)

data class PublicacaoPrazo(
    val quantidade: Int,
    val unidade: String,
    val diasUteis: Boolean,
    val textoOriginal: String,
    val dataLimiteEstimada: LocalDate? = null,
    val isConfirmado: Boolean = false,
    val idExternoCalendario: String? = null,
    val provedorCalendario: String? = null,
    val confiancaCalculo: ConfiancaCalculo = ConfiancaCalculo.PENDENTE,
)
