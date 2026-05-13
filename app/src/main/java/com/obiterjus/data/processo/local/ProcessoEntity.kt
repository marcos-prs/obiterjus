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
    val dataJudTentativasRestantes: Int = 0,
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
)
