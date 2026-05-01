package com.obiterjus.presentation.auditoria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.data.auditoria.local.SyncLogDao
import com.obiterjus.data.auditoria.local.SyncLogEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AuditoriaViewModel(
    syncLogDao: SyncLogDao,
) : ViewModel() {

    val estado: StateFlow<EstadoAuditoria> =
        syncLogDao.observeRecentes().map { logs ->
            EstadoAuditoria(logs = logs)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EstadoAuditoria(),
        )
}

data class EstadoAuditoria(
    val logs: List<SyncLogEntity> = emptyList(),
)
