package com.obiterjus.data.datajud.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.obiterjus.data.processo.local.ProcessoEntity
import java.time.Instant

@Entity(
    tableName = "movimentos",
    foreignKeys = [
        ForeignKey(
            entity = ProcessoEntity::class,
            parentColumns = ["numeroProcesso"],
            childColumns = ["numeroProcesso"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("numeroProcesso"),
        Index("dataHora"),
        Index(value = ["numeroProcesso", "dataHora"]),
    ],
)
data class MovimentoEntity(
    @PrimaryKey
    val idLocal: String,
    val numeroProcesso: String,
    val codigo: Int?,
    val nome: String?,
    val dataHora: Instant?,
    val complementosJson: String?,
)
