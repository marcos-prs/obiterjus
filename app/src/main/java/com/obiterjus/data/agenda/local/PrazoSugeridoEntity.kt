package com.obiterjus.data.agenda.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.obiterjus.data.publicacao.local.PublicacaoEntity
import java.time.LocalDate

@Entity(
    tableName = "prazos_sugeridos",
    foreignKeys = [
        ForeignKey(
            entity = PublicacaoEntity::class,
            parentColumns = ["id"],
            childColumns = ["publicacaoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("publicacaoId", unique = true)
    ]
)
data class PrazoSugeridoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val publicacaoId: Long,
    val quantidade: Int,
    val unidade: String,
    val diasUteis: Boolean,
    val textoOriginal: String,
    val dataLimite: LocalDate?,
    val isConfirmado: Boolean = false,
    val idExternoCalendario: String? = null,
    val provedorCalendario: String? = null
)
