package com.obiterjus.data.processo.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.obiterjus.domain.model.ProcessoSyncStatus
import java.time.Instant

@Entity(
    tableName = "processos",
    indices = [
        Index("tribunal"),
        Index("syncStatus"),
        Index("atualizadoEm"),
    ],
)
data class ProcessoEntity(
    @PrimaryKey
    val numeroProcesso: String,
    val tribunal: String?,
    val grau: String?,
    val classeCodigo: Int?,
    val classeNome: String?,
    val assuntosJson: String?,
    val orgaoJulgadorCodigo: Int?,
    val orgaoJulgadorNome: String?,
    val nivelSigilo: Int?,
    val dataAjuizamento: Instant?,
    val syncStatus: ProcessoSyncStatus,
    val capturadoEm: Instant,
    val atualizadoEm: Instant,
)
