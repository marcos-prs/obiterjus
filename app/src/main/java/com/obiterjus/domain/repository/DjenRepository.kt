package com.obiterjus.domain.repository

import com.obiterjus.domain.model.MonitorarDjenParams
import com.obiterjus.domain.model.MonitorarDjenResumo

interface DjenRepository {
    suspend fun monitorar(params: MonitorarDjenParams): MonitorarDjenResumo
}
