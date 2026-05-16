package com.obiterjus

import android.app.Application
import com.obiterjus.core.worker.DjenSyncScheduler
import com.obiterjus.core.worker.PrazosWorkerScheduler
import com.obiterjus.di.obiterModules
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class ObiterJusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ObiterJusApplication)
            workManagerFactory()
            modules(obiterModules)
        }

        // Sincronização DJEN: uma vez por dia às 7h
        DjenSyncScheduler.schedulePeriodic(this)

        // Verificação diária de prazos (uma vez por dia, sem rede)
        PrazosWorkerScheduler.schedulePeriodic(this)
    }
}
