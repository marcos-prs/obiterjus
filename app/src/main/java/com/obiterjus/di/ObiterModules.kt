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
import com.obiterjus.data.minuta.remote.ObiterMinutaDataSource
import com.obiterjus.data.minuta.remote.ObiterMinutaRetrofitFactory
import com.obiterjus.data.time.CalendarioForenseDataSource
import com.obiterjus.data.time.CalendarioForenseRetrofitFactory
import com.obiterjus.data.viacep.ViaCepApi
import com.obiterjus.data.viacep.ViaCepRetrofitFactory
import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.core.parser.DjenPrazoExtractor
import com.obiterjus.domain.usecase.CalcularPrazoRegraUC
import com.obiterjus.domain.usecase.ResolverNaturezaProcessoUC
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
import com.obiterjus.domain.usecase.CadastrarPrazoManualUC
import com.obiterjus.domain.usecase.ConfirmarPrazoUC
import com.obiterjus.domain.usecase.MarcarPrazoCumpridoUC
import com.obiterjus.data.agenda.worker.CalendarSyncWorker
import com.obiterjus.data.djen.CertidaoDjenRepositoryImpl
import com.obiterjus.data.djen.DjenSyncExecutor
import com.obiterjus.data.djen.DjenSyncExecutorImpl
import com.obiterjus.data.djen.DjenPartesResolver
import com.obiterjus.data.datajud.ConfiguredDataJudRepository
import com.obiterjus.data.djen.ConfiguredDjenRepository
import com.obiterjus.data.cliente.local.LocalClienteRepository
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
import com.obiterjus.domain.repository.CadastroOabRepository
import com.obiterjus.domain.repository.ClientesRepository
import com.obiterjus.domain.repository.ProcessosRepository
import com.obiterjus.domain.repository.PublicacoesRepository
import com.obiterjus.domain.repository.SincronizacaoRepository
import com.obiterjus.domain.usecase.AdicionarProcessoUseCase
import com.obiterjus.domain.usecase.ClassificarPublicacaoUC
import com.obiterjus.domain.usecase.ExcluirProcessoUseCase
import com.obiterjus.domain.usecase.ExportarRelatorioUC
import com.obiterjus.domain.usecase.MonitorarCnjUseCase
import com.obiterjus.domain.usecase.MonitorarDjenUseCase
import com.obiterjus.domain.usecase.ObservarAgendaPrazos
import com.obiterjus.domain.usecase.BuscarClientesSemelhantes
import com.obiterjus.domain.usecase.ObservarClientes
import com.obiterjus.domain.usecase.ObservarClientesPorProcesso
import com.obiterjus.domain.usecase.SincronizarClientesDoProcesso
import com.obiterjus.domain.usecase.ObservarMovimentosProcesso
import com.obiterjus.domain.usecase.ObservarProcessos
import com.obiterjus.domain.usecase.ObservarPublicacoes
import com.obiterjus.domain.usecase.ObservarTimelineProcesso
import com.obiterjus.domain.usecase.ObterPublicacaoPorId
import com.obiterjus.domain.usecase.ObterCertidaoDjen
import com.obiterjus.domain.usecase.RessincronizarProcessoUseCase
import com.obiterjus.domain.usecase.SincronizarProcessosDataJudUseCase
import com.obiterjus.presentation.adicionarprocesso.AdicionarProcessoViewModel
import com.obiterjus.presentation.detalheprocesso.DetalheProcessoViewModel
import com.obiterjus.presentation.detalhepublicacao.DetalhePublicacaoViewModel
import com.obiterjus.presentation.autenticacao.AutenticacaoViewModel
import com.obiterjus.presentation.editarprocesso.EditarProcessoViewModel
import com.obiterjus.presentation.inicio.InicioViewModel
import com.obiterjus.presentation.monitoramento.MonitoramentoViewModel
import com.obiterjus.presentation.perfil.PerfilViewModel
import com.obiterjus.presentation.perfil.EditarPerfilViewModel
import com.obiterjus.presentation.clientes.ClientesViewModel
import com.obiterjus.presentation.detalhecliente.DetalheClienteViewModel
import com.obiterjus.presentation.editarcliente.EditarClienteViewModel
import com.obiterjus.presentation.processos.ProcessosViewModel
import com.obiterjus.presentation.prazos.PrazosViewModel
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
    single { get<ObiterDatabase>().clienteDao() }
    single { PublicacaoNotificationHelper(androidContext()) }

    // Prazos — API CalendárioForense é a única fonte de cálculo
    single<CalendarioForenseDataSource> { CalendarioForenseRetrofitFactory.createApi() }
    single<ViaCepApi> { ViaCepRetrofitFactory.createApi() }
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
    single<ObiterMinutaDataSource> {
        ObiterMinutaRetrofitFactory.createApi(
            tokenProvider = { get<AuthRepository>().getIdToken() },
        )
    }
    single<CadastroOabRepository> { PreferencesCadastroOabRepository(androidContext()) }
    single<PerfilPreferencesRepository> { DataStorePerfilPreferencesRepository(androidContext()) }
    single { LocalPublicacaoRepository(get()) } bind PublicacoesRepository::class
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
    } bind ProcessosRepository::class

    single { LocalClienteRepository(clienteDao = get()) } bind ClientesRepository::class
    
    single { ResolverNaturezaProcessoUC(get()) }
    single { CalcularPrazoRegraUC(get(), get()) }
    single { PublicacaoPrazoMapper(get(), get()) }
    single { DjenMapper() }
    single { DjenPartesResolver(get(), get()) }

    single<DjenRepository> {
        ConfiguredDjenRepository(
            appConfigRepository = get(),
            localPublicacaoRepository = get(),
            localProcessoRepository = get(),
            djenMapper = get(),
            publicacaoPrazoMapper = get(),
            partesResolver = get(),
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
    single<SincronizacaoRepository> {
        FirestoreSincronizacaoRepository(
            localProcessoRepository = get(),
            localPublicacaoRepository = get(),
            repositorioCadastroOab = get(),
            perfilPreferencesRepository = get(),
            partesResolver = get(),
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
    factory { ClassificarPublicacaoUC(get(), get()) }
    factory { ExportarRelatorioUC(get()) }
    factory { ObservarPublicacoes(get()) }
    factory { ObterPublicacaoPorId(get()) }
    factory { ObservarClientesPorProcesso(get(), get()) }
    factory { ObservarAgendaPrazos(get(), get(), get(), get()) }
    factory { ObterCertidaoDjen(get()) }
    factory { ObservarProcessos(get()) }
    factory { ObservarClientes(get()) }
    factory { BuscarClientesSemelhantes(get()) }
    factory { SincronizarClientesDoProcesso(get()) }
    factory { ObservarMovimentosProcesso(get()) }
    factory {
        ObservarTimelineProcesso(
            repositorioProcessos = get(),
            repositorioPublicacoes = get(),
        )
    }
    factory { ConfirmarPrazoUC(get(), get()) }
    factory { MarcarPrazoCumpridoUC(get()) }
    factory { CadastrarPrazoManualUC(get(), get(), get(), get()) }
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
    viewModel { PublicacoesViewModel(get(), get(), get()) }
    viewModel { PrazosViewModel(androidContext(), get(), get(), get(), get()) }
    viewModel { ProcessosViewModel(get(), get()) }
    viewModel { ClientesViewModel(get()) }
    viewModel { DetalheClienteViewModel(get()) }
    viewModel { EditarClienteViewModel(get()) }
    viewModel { AuditoriaViewModel(get()) }
    viewModel { InicioViewModel(androidContext(), get(), get(), get(), get(), get(), get()) }
    viewModel { PerfilViewModel(androidContext(), get(), get(), get(), get(), get()) }
    viewModel { AutenticacaoViewModel(androidContext(), get(), get(), get(), get(), get()) }
    viewModel { EditarPerfilViewModel(get(), get(), get(), get(), androidContext()) }
    viewModel { DetalheProcessoViewModel(get(), get(), get(), get(), get()) }
    viewModel {
        DetalhePublicacaoViewModel(
            androidContext(),
            get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { AdicionarProcessoViewModel(get()) }
    viewModel { EditarProcessoViewModel(get(), get(), get(), get(), get(), get(), get()) }
}

private val workerModule = module {
    // O KoinWorkerFactory injeta apenas o WorkerParameters como parâmetro dinâmico;
    // o Context deve vir do escopo (androidContext()), nunca de params.get<Context>().
    worker { params ->
        DjenSyncWorker(
            appContext = androidContext(),
            workerParams = params.get<WorkerParameters>(),
            djenSyncExecutor = get(),
            notificationHelper = get(),
            perfilPreferencesRepository = get(),
            syncLogDao = get(),
        )
    }
    worker { params ->
        PrazosWorker(
            appContext = androidContext(),
            workerParams = params.get<WorkerParameters>(),
            repositorioPublicacoes = get(),
            notificationHelper = get(),
            syncLogDao = get(),
        )
    }
    worker { params ->
        CalendarSyncWorker(
            appContext = androidContext(),
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
