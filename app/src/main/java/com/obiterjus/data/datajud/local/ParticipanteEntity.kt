package com.obiterjus.data.datajud.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "participantes",
    indices = [
        Index("numeroProcesso"),
    ],
)
data class ParticipanteEntity(
    @PrimaryKey
    val idLocal: String,
    val numeroProcesso: String,
    val polo: String?,
    val nome: String?,
    val tipoPessoa: String?,
    val tipoParticipacao: String?,
)
