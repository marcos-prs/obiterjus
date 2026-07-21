package com.obiterjus.data.cliente.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.obiterjus.data.processo.local.ProcessoEntity
import java.time.Instant

/**
 * Vínculo N:N entre cliente e processo — é ele que torna possível a pergunta
 * que hoje não tem resposta: "quais processos são deste cliente?".
 *
 * O papel do cliente naquele processo (polo, tipo de participação) não é
 * copiado para cá: [participanteIdLocal] aponta para o participante que
 * originou o vínculo, e é de lá que esses dados são lidos.
 */
@Entity(
    tableName = "clientes_processos",
    primaryKeys = ["clienteId", "numeroProcesso"],
    foreignKeys = [
        ForeignKey(
            entity = ClienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProcessoEntity::class,
            parentColumns = ["numeroProcesso"],
            childColumns = ["numeroProcesso"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("numeroProcesso"),
    ],
)
data class ClienteProcessoEntity(
    val clienteId: String,
    val numeroProcesso: String,
    /** Participante que originou o vínculo; nulo se vinculado manualmente. */
    val participanteIdLocal: String? = null,
    val vinculadoEm: Instant,
)
