package com.obiterjus.core.worker

import android.content.Context
import com.obiterjus.domain.repository.AgendadorSincronizacao

/**
 * Implementação de [AgendadorSincronizacao] em cima do [DjenSyncScheduler].
 *
 * O scheduler continua sendo `object` porque o [DjenSyncWorker] se re-ancora de
 * dentro da própria execução, onde não há injeção disponível.
 */
class AgendadorSincronizacaoWorkManager(
    private val context: Context,
) : AgendadorSincronizacao {

    override fun garantirDiaria() = DjenSyncScheduler.ensureScheduled(context)

    override fun agendarDiaria() = DjenSyncScheduler.schedulePeriodic(context)

    override fun cancelarDiaria() = DjenSyncScheduler.cancel(context)
}
