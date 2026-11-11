package com.obiterjus

import android.app.Application
import com.obiterjus.core.worker.DjenSyncScheduler
import com.obiterjus.core.worker.PrazosWorkerScheduler
import com.obiterjus.data.djen.CertidaoDjenRepositoryImpl
import com.obiterjus.data.push.PushRegistroRepository
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.di.obiterModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class ObiterJusApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val koinApp = startKoin {
            androidContext(this@ObiterJusApplication)
            workManagerFactory()
            modules(obiterModules)
        }

        // Sincronização DJEN: uma vez por dia às 7h, respeitando a preferência do usuário
        val perfilPreferencesRepository = koinApp.koin.get<PerfilPreferencesRepository>()
        applicationScope.launch {
            val preferencias = perfilPreferencesRepository.preferencias.first()
            if (preferencias.sincronizacaoAutomatica) {
                DjenSyncScheduler.ensureScheduled(this@ObiterJusApplication)
            } else {
                DjenSyncScheduler.cancel(this@ObiterJusApplication)
            }
        }

        // Quem acorda o app às 7h é o push do servidor; o WorkManager periódico
        // acima é a rede de segurança. O registro é reenviado a cada abertura
        // porque o token do FCM rotaciona — é idempotente, e a falha é
        // silenciosa. Fica inerte até `push_diario_enabled` ligar no Remote
        // Config, o que só acontece quando o cron estiver no ar.
        val pushRegistro = koinApp.koin.get<PushRegistroRepository>()
        applicationScope.launch { pushRegistro.sincronizar() }

        // Verificação diária de prazos (uma vez por dia, sem rede)
        PrazosWorkerScheduler.schedulePeriodic(this)

        // Certidão é peça processual em claro no cache; expira em 24h.
        applicationScope.launch(Dispatchers.IO) {
            CertidaoDjenRepositoryImpl.limparCache(this@ObiterJusApplication)
        }
    }
}
