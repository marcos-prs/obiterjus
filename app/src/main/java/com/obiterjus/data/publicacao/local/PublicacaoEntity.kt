package com.obiterjus.data.publicacao.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.obiterjus.domain.model.ConfiancaMatch
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "publicacoes",
    indices = [
        Index(value = ["hash"], unique = true),
        Index("numeroProcesso"),
        Index("dataDisponibilizacao"),
        Index("tribunal"),
        Index("tipoComunicacao"),
        Index("isSigiloso"),
        Index("confiancaMatch"),
        Index("duplicataDe"),
    ],
)
data class PublicacaoEntity(
    @PrimaryKey
    val id: Long,
    val hash: String?,
    val numeroProcesso: String?,
    val participantesJson: String?,
    val prazoQuantidade: Int?,
    val prazoUnidade: String?,
    val prazoDiasUteis: Boolean,
    val prazoTexto: String?,
    val prazoDataLimite: LocalDate?,
    val dataDisponibilizacao: LocalDate?,
    val tribunal: String?,
    val tipoComunicacao: String?,
    val nomeOrgao: String?,
    val idOrgao: Long?,
    val textoRaw: String?,
    val textoLimpo: String?,
    val textoPossuiHtml: Boolean,
    val textoPossuiErroTemplate: Boolean,
    val isSigiloso: Boolean,
    val ativo: Boolean,
    val fonte: String,
    val capturadoEm: Instant,
    val atualizadoEm: Instant,
    val confiancaMatch: ConfiancaMatch = ConfiancaMatch.ALTA,
    val duplicataDe: Long? = null,
    val totalDuplicatas: Int = 0,
)
