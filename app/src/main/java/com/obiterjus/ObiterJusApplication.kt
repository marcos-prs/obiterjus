package com.obiterjus

import android.app.Application
import com.obiterjus.core.worker.DjenSyncScheduler
import com.obiterjus.core.worker.PrazosWorkerScheduler
import com.obiterjus.data.settings.SyncPreferencesRepository
import com.obiterjus.di.obiterModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject

class ObiterJusApplication : Application() {
    private val syncPrefs: SyncPreferencesRepository by inject(SyncPreferencesRepository::class.java)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ObiterJusApplication)
            workManagerFactory()
            modules(obiterModules)
        }

        // Agendamento dinâmico do worker DJEN de acordo com a preferência do usuário
        CoroutineScope(Dispatchers.IO).launch {
            syncPrefs.syncFrequencyHours.collect { hours ->
                DjenSyncScheduler.schedulePeriodic(this@ObiterJusApplication, hours)
            }
        }

        // Agendamento diário do worker de prazos (uma vez por dia, sem rede)
        PrazosWorkerScheduler.schedulePeriodic(this)
    }
}
