package com.obiterjus.data.auditoria.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Registro de uma execução de sincronização — persiste histórico de todas as tentativas
 * do [DjenSyncWorker] e do [PrazosWorker] para a tela de Auditoria.
 */
@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Instante em que o worker foi iniciado. */
    val executadoEm: Instant,
    /** Duração em milissegundos. Pode ser nula se o worker falhou antes de completar. */
    val duracaoMs: Long?,
    /** Fonte principal: "DJEN", "DataJud", "PrazosWorker" etc. */
    val fonte: String,
    /** Quantidade de novas publicações salvas nesta execução (pode ser 0). */
    val novasPublicacoes: Int,
    /** Quantidade de processos enriquecidos pelo DataJud (pode ser 0). */
    val processosSincronizados: Int,
    /** true = execução bem-sucedida, false = falha. */
    val sucesso: Boolean,
    /** Mensagem de erro, se houver. */
    val mensagemErro: String?,
)
