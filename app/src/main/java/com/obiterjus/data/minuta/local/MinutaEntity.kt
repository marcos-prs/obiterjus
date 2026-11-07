package com.obiterjus.data.minuta.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Sugestão de minuta gerada e persistida localmente. Guarda os campos escalares
 * necessários para a listagem e um [payloadJson] com o DTO completo serializado,
 * do qual as seções/pontos de atenção são reconstruídos na tela de resultado.
 *
 * Sem FK para `processos`: a minuta pode ser gerada sem processo vinculado
 * ([numeroProcesso] nulo) ou para um número ainda não sincronizado localmente.
 */
@Entity(
    tableName = "minutas",
    indices = [
        Index("numeroProcesso"),
        Index("geradoEm"),
    ],
)
data class MinutaEntity(
    @PrimaryKey
    val sugestaoId: String,
    val numeroProcesso: String?,
    /** Código de [com.obiterjus.domain.model.TipoMinuta]. */
    val tipoMinuta: String,
    val titulo: String,
    val confiancaGeral: Double,
    val requerRevisao: Boolean,
    val geradoEm: Instant,
    /** `MinutaSugestaoDto` serializado — fonte de verdade da reconstrução. */
    val payloadJson: String,
)
