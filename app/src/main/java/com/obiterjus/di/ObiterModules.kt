package com.obiterjus.di

import android.content.Context
import androidx.work.WorkerParameters
import com.obiterjus.BuildConfig
import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.core.database.ObiterDatabase
import com.obiterjus.core.database.ObiterDatabaseFactory
import com.obiterjus.core.notification.PublicacaoNotificationHelper
import com.obiterjus.core.worker.DjenSyncWorker
import com.obiterjus.core.worker.PrazosWorker
import com.obiterjus.data.auditoria.local.SyncLogDao
import com.obiterjus.presentation.auditoria.AuditoriaViewModel
import com.obiterjus.data.auth.FirebaseAuthRepository
import com.obiterjus.data.config.FirebaseRemoteAppConfigRepository
import com.obiterjus.data.time.BrasilApiDataSource
import com.obiterjus.data.time.BrasilApiRetrofitFactory
import com.obiterjus.data.time.FeriadoRepository
import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.core.parser.DjenPrazoExtractor
import com.obiterjus.domain.usecase.CalcularPrazoRegraUC
import com.obiterjus.data.djen.mapper.PublicacaoPrazoMapper
import com.obiterjus.data.djen.mapper.DjenMapper
import com.obiterjus.data.agenda.remote.CalendarRetrofitFactory
import com.obiterjus.data.agenda.remote.DataStoreGoogleCalendarTokenRepository
import com.obiterjus.data.agenda.remote.GoogleCalendarAuthorizationRepository
import com.obiterjus.data.agenda.remote.GoogleCalendarDataSource
import com.obiterjus.data.agenda.remote.OutlookCalendarDataSource
import com.obiterjus.data.agenda.remote.CalendarSyncRepositoryImpl
import com.obiterjus.data.agenda.remote.GoogleCalendarTokenRepository
import com.obiterjus.domain.repository.CalendarSyncRepository
import com.obiterjus.domain.usecase.ConfirmarPrazoUC
import com.obiterjus.data.agenda.worker.CalendarSyncWorker
import com.obiterjus.data.djen.CertidaoDjenRepositoryImpl
import com.obiterjus.data.djen.DjenSyncExecutor
import com.obiterjus.data.djen.DjenSyncExecutorImpl
import com.obiterjus.data.datajud.ConfiguredDataJudRepository
import com.obiterjus.data.djen.ConfiguredDjenRepository
import com.obiterjus.data.processo.local.LocalProcessoRepository
import com.obiterjus.data.publicacao.local.LocalPublicacaoRepository
import com.obiterjus.data.settings.PreferencesCadastroOabRepository
import com.obiterjus.data.settings.DataStorePerfilPreferencesRepository
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.data.sincronizacao.FirestoreSincronizacaoRepository
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.CertidaoDjenRepository
import com.obiterjus.domain.repository.DataJudRepository
import com.obiterjus.domain.repository.DjenRepository
import com.obiterjus.domain.repository.RepositorioCadastroOab
import com.obiterjus.domain.repository.RepositorioProcessos
import com.obiterjus.domain.repository.RepositorioPublicacoes
import com.obiterjus.domain.repository.RepositorioSincronizacao
import com.obiterjus.domain.usecase.AdicionarProcessoUseCase
import com.obiterjus.domain.usecase.ClassificarPublicacaoUC
import com.obiterjus.domain.usecase.ExcluirProcessoUseCase
import com.obiterjus.domain.usecase.ExportarRelatorioUC
import com.obiterjus.domain.usecase.MonitorarCnjUseCase
import com.obiterjus.domain.usecase.MonitorarDjenUseCase
import com.obiterjus.domain.usecase.ObservarAgendaPrazos
import com.obiterjus.domain.usecase.ObservarMovimentosProcesso
import com.obiterjus.domain.usecase.ObservarProcessos
import com.obiterjus.domain.usecase.ObservarPublicacoes
import com.obiterjus.domain.usecase.ObservarTimelineProcesso
import com.obiterjus.domain.usecase.ObterPublicacaoPorId
import com.obiterjus.domain.usecase.ObterCertidaoDjen
import com.obiterjus.domain.usecase.RessincronizarProcessoUseCase
import com.obiterjus.domain.usecase.SincronizarProcessosDataJudUseCase
import com.obiterjus.presentation.adicionarprocesso.ModeloAdicionarProcesso
import com.obiterjus.presentation.detalheprocesso.ModeloDetalheProcesso
import com.obiterjus.presentation.detalhepublicacao.ModeloDetalhePublicacao
import com.obiterjus.presentation.autenticacao.ModeloAutenticacao
import com.obiterjus.presentation.editarprocesso.ModeloEditarProcesso
import com.obiterjus.presentation.inicio.ModeloInicio
import com.obiterjus.presentation.monitoramento.MonitoramentoViewModel
import com.obiterjus.presentation.perfil.ModeloPerfil
import com.obiterjus.presentation.perfil.ModeloEditarPerfil
import com.obiterjus.presentation.processos.ModeloProcessos
import com.obiterjus.presentation.prazos.ModeloPrazos
import com.obiterjus.presentation.publicacoes.PublicacoesViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import java.time.Clock

private val coreModule = module {
    single { Clock.systemDefaultZone() }
    single<ObiterDatabase> { ObiterDatabaseFactory.create(androidContext()) }
    single { get<ObiterDatabase>().publicacaoDao() }
    single { get<ObiterDatabase>().processoDao() }
    single { get<ObiterDatabase>().movimentoDao() }
    single { get<ObiterDatabase>().participanteDao() }
    single { get<ObiterDatabase>().syncLogDao() }
    single { get<ObiterDatabase>().prazoSugeridoDao() }
    single { PublicacaoNotificationHelper(androidContext()) }

    // Feriados e Prazos
    single<BrasilApiDataSource> { BrasilApiRetrofitFactory.createApi() }
    single { FeriadoRepository(get()) }
    single { CalculadoraPrazos(get()) }
    single { DjenPrazoExtractor(get()) }
}

private val dataModule = module {
    single<AppConfigRepository> {
        FirebaseRemoteAppConfigRepository(
            fallbackDataJudApiKey = BuildConfig.DATAJUD_API_KEY,
        )
    }
    single<AuthRepository> { FirebaseAuthRepository() }
    single<RepositorioCadastroOab> { PreferencesCadastroOabRepository(androidContext()) }
    single<PerfilPreferencesRepository> { DataStorePerfilPreferencesRepository(androidContext()) }
    single { LocalPublicacaoRepository(get()) } bind RepositorioPublicacoes::class
    single<GoogleCalendarTokenRepository> { DataStoreGoogleCalendarTokenRepository(androidContext()) }
    single { GoogleCalendarAuthorizationRepository(androidContext(), get()) }
    single<GoogleCalendarDataSource> {
        CalendarRetrofitFactory.createGoogleApi(
            tokenProvider = { get<GoogleCalendarTokenRepository>().accessToken.value },
        )
    }

    single<OutlookCalendarDataSource> { CalendarRetrofitFactory.createOutlookApi() }

    single<CalendarSyncRepository> { CalendarSyncRepositoryImpl(get(), get(), get()) }

    single {
        LocalProcessoRepository(
            processoDao = get(),
            movimentoDao = get(),
            participanteDao = get(),
        )
    } bind RepositorioProcessos::class
    
    single { CalcularPrazoRegraUC(get(), get()) }
    single { PublicacaoPrazoMapper(get()) }
    single { DjenMapper(get()) }

    single<DjenRepository> {
        ConfiguredDjenRepository(
            appConfigRepository = get(),
            localPublicacaoRepository = get(),
            localProcessoRepository = get(),
            djenMapper = get(),
            publicacaoPrazoMapper = get(),
        )
    }
    single<CertidaoDjenRepository> {
        CertidaoDjenRepositoryImpl(
            context = androidContext(),
            appConfigRepository = get(),
        )
    }
    single<DataJudRepository> {
        ConfiguredDataJudRepository(
            appConfigRepository = get(),
            localProcessoRepository = get(),
        )
    }
    single<RepositorioSincronizacao> {
        FirestoreSincronizacaoRepository(
            localProcessoRepository = get(),
            localPublicacaoRepository = get(),
            repositorioCadastroOab = get(),
            perfilPreferencesRepository = get(),
        )
    }
    single<DjenSyncExecutor> {
        DjenSyncExecutorImpl(
            appConfigRepository = get(),
            perfilPreferencesRepository = get(),
            repositorioCadastroOab = get(),
            monitorarCnjUseCase = get(),
            syncLogDao = get(),
            clock = get(),
        )
    }
}

private val domainModule = module {
    factory { MonitorarDjenUseCase(get()) }
    factory { SincronizarProcessosDataJudUseCase(get()) }
    factory {
        MonitorarCnjUseCase(
            monitorarDjenUseCase = get(),
            sincronizarProcessosDataJudUseCase = get(),
            repositorioProcessos = get(),
        )
    }
    factory { ClassificarPublicacaoUC(get()) }
    factory { ExportarRelatorioUC(get()) }
    factory { ObservarPublicacoes(get()) }
    factory { ObterPublicacaoPorId(get()) }
    factory { ObservarAgendaPrazos(get(), get()) }
    factory { ObterCertidaoDjen(get()) }
    factory { ObservarProcessos(get()) }
    factory { ObservarMovimentosProcesso(get()) }
    factory {
        ObservarTimelineProcesso(
            repositorioProcessos = get(),
            repositorioPublicacoes = get(),
        )
    }
    factory { ConfirmarPrazoUC(get(), get()) }
    factory { AdicionarProcessoUseCase(get()) }
    factory { ExcluirProcessoUseCase(get()) }
    factory { RessincronizarProcessoUseCase(get()) }
}

private val presentationModule = module {
    viewModel {
        MonitoramentoViewModel(
            monitorarCnjUseCase = get(),
            authRepository = get(),
            repositorioSincronizacao = get(),
            repositorioCadastroOab = get(),
            exportarRelatorioUC = get(),
        )
    }
    viewModel { PublicacoesViewModel(get(), get()) }
    viewModel { ModeloPrazos(androidContext(), get(), get(), get()) }
    viewModel { ModeloProcessos(get(), get()) }
    viewModel { AuditoriaViewModel(get()) }
    viewModel { ModeloInicio(androidContext(), get(), get(), get(), get(), get()) }
    viewModel { ModeloPerfil(androidContext(), get(), get(), get(), get(), get()) }
    viewModel { ModeloAutenticacao(androidContext(), get(), get(), get(), get(), get()) }
    viewModel { ModeloEditarPerfil(get(), get(), get(), get(), androidContext()) }
    viewModel { ModeloDetalheProcesso(get(), get(), get(), get()) }
    viewModel { ModeloDetalhePublicacao(get(), get()) }
    viewModel { ModeloAdicionarProcesso(get()) }
    viewModel { ModeloEditarProcesso(get(), get(), get()) }
}

private val workerModule = module {
    worker { params ->
        DjenSyncWorker(
            appContext = params.get<Context>(),
            workerParams = params.get<WorkerParameters>(),
            djenSyncExecutor = get(),
            notificationHelper = get(),
        )
    }
    worker { params ->
        PrazosWorker(
            appContext = params.get<Context>(),
            workerParams = params.get<WorkerParameters>(),
            repositorioPublicacoes = get(),
            notificationHelper = get(),
            syncLogDao = get(),
        )
    }
    worker { params ->
        CalendarSyncWorker(
            appContext = params.get<Context>(),
            workerParams = params.get<WorkerParameters>(),
        )
    }
}

val obiterModules: List<Module> = listOf(
    coreModule,
    dataModule,
    domainModule,
    presentationModule,
    workerModule,
)
