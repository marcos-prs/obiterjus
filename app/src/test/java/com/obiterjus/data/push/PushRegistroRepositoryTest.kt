package com.obiterjus.data.push

import com.obiterjus.core.config.AppConfig
import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.data.push.dto.PushConsultaRequestDto
import com.obiterjus.data.settings.PerfilPreferences
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.domain.model.OabCadastro
import com.obiterjus.domain.model.SincronizacaoStatus
import com.obiterjus.domain.repository.CadastroOabRepository
import com.obiterjus.ui.theme.TipoTema
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PushRegistroRepositoryTest {

    @Test
    fun flagDesligadaNaoInscreveEmNada() = runTest {
        val fcm = FcmFake()
        val repo = repositorio(fcm = fcm, pushDiarioEnabled = false)

        assertEquals(PushRegistroRepository.Resultado.DESLIGADO, repo.sincronizar())
        assertTrue(fcm.inscritos.isEmpty())
        assertEquals(0, fcm.registros)
    }

    @Test
    fun semPreChecagemInscreveSoNoTopicoCegoENaoChamaOServidor() = runTest {
        val fcm = FcmFake()
        val dataSource = DataSourceFake()
        val repo = repositorio(fcm = fcm, dataSource = dataSource)

        val resultado = repo.sincronizar()

        assertEquals(PushRegistroRepository.Resultado.REGISTRADO, resultado)
        // Fase 1: o servidor não sabe de nós. Nenhuma chamada de rede.
        assertNull(dataSource.ultimaConsulta)
        assertEquals(setOf("sync-diaria-h07-utcm180"), fcm.inscritos)
        assertEquals(1, fcm.registros)
    }

    @Test
    fun comPreChecagemInscreveNosDoisTopicosEMandaAMarcaDagua() = runTest {
        val fcm = FcmFake()
        val dataSource = DataSourceFake()
        val repo = repositorio(
            fcm = fcm,
            dataSource = dataSource,
            preferencias = preferenciasPadrao.copy(pushPreChecagemAutorizada = true),
            maiorIdConhecido = 987_654_321L,
        )

        val resultado = repo.sincronizar()

        assertEquals(PushRegistroRepository.Resultado.REGISTRADO_COM_CONSULTA, resultado)
        assertEquals(2, fcm.inscritos.size)
        assertTrue(fcm.inscritos.contains("sync-diaria-h07-utcm180"))
        assertTrue(fcm.inscritos.any { it.startsWith("oab-") })

        val consulta = requireNonNull(dataSource.ultimaConsulta)
        assertEquals("123456", consulta.numeroOab)
        assertEquals("RS", consulta.ufOab)
        assertEquals(987_654_321L, consulta.marcaDagua)
        // A OAB não pode aparecer no nome do tópico: ele transita nos
        // metadados do Google.
        assertTrue(consulta.topico.startsWith("oab-"))
        assertTrue(!consulta.topico.contains("123456"))
    }

    @Test
    fun revogarPreChecagemSaiDoTopicoDaOabEApagaNoServidor() = runTest {
        val fcm = FcmFake()
        val dataSource = DataSourceFake()
        val memoria = MemoriaFake()

        // Primeiro com autorização...
        repositorio(
            fcm = fcm,
            dataSource = dataSource,
            memoria = memoria,
            preferencias = preferenciasPadrao.copy(pushPreChecagemAutorizada = true),
        ).sincronizar()
        val topicoOab = requireNonNull(fcm.inscritos.firstOrNull { it.startsWith("oab-") })

        // ...e depois sem.
        val resultado = repositorio(
            fcm = fcm,
            dataSource = dataSource,
            memoria = memoria,
            preferencias = preferenciasPadrao.copy(pushPreChecagemAutorizada = false),
        ).sincronizar()

        assertEquals(PushRegistroRepository.Resultado.REGISTRADO, resultado)
        assertTrue(fcm.desinscritos.contains(topicoOab))
        assertEquals(setOf("sync-diaria-h07-utcm180"), fcm.inscritos)
        assertEquals(1, dataSource.cancelamentos)
    }

    @Test
    fun mudancaDeFusoSaiDoTopicoAntigo() = runTest {
        val fcm = FcmFake()
        val memoria = MemoriaFake()

        repositorio(fcm = fcm, memoria = memoria, zona = ZoneId.of("America/Sao_Paulo")).sincronizar()
        repositorio(fcm = fcm, memoria = memoria, zona = ZoneId.of("America/Rio_Branco")).sincronizar()

        // Sem sair do tópico antigo o aparelho seria acordado nos dois horários.
        assertTrue(fcm.desinscritos.contains("sync-diaria-h07-utcm180"))
        assertEquals(setOf("sync-diaria-h07-utcm300"), fcm.inscritos)
    }

    @Test
    fun sincronizacaoAutomaticaDesligadaCancelaTudo() = runTest {
        val fcm = FcmFake()
        val memoria = MemoriaFake()

        repositorio(
            fcm = fcm,
            memoria = memoria,
            preferencias = preferenciasPadrao.copy(pushPreChecagemAutorizada = true),
        ).sincronizar()

        val resultado = repositorio(
            fcm = fcm,
            memoria = memoria,
            preferencias = preferenciasPadrao.copy(sincronizacaoAutomatica = false),
        ).sincronizar()

        assertEquals(PushRegistroRepository.Resultado.CANCELADO, resultado)
        assertTrue(fcm.inscritos.isEmpty())
        assertTrue(memoria.topicos().isEmpty())
    }

    @Test
    fun falhaDeRedeNaoPropaga() = runTest {
        val dataSource = object : PushRegistroDataSource {
            override suspend fun registrarConsulta(
                chaveApp: String,
                pedido: PushConsultaRequestDto,
            ) = throw IllegalStateException("servidor fora do ar")

            override suspend fun cancelarConsulta(chaveApp: String, instalacao: String) = Unit
        }

        val repo = repositorio(
            dataSource = dataSource,
            preferencias = preferenciasPadrao.copy(pushPreChecagemAutorizada = true),
        )

        // A abertura do app não pode quebrar porque o servidor caiu.
        assertEquals(PushRegistroRepository.Resultado.FALHOU, repo.sincronizar())
    }

    // -----------------------------------------------------------------------

    private val preferenciasPadrao = PerfilPreferences(
        sincronizacaoAutomatica = true,
        pushPreChecagemAutorizada = false,
        tema = TipoTema.SISTEMA,
    )

    private fun repositorio(
        fcm: FcmFake = FcmFake(),
        dataSource: PushRegistroDataSource = DataSourceFake(),
        memoria: MemoriaPush = MemoriaFake(),
        preferencias: PerfilPreferences = preferenciasPadrao,
        pushDiarioEnabled: Boolean = true,
        maiorIdConhecido: Long? = 100L,
        zona: ZoneId = ZoneId.of("America/Sao_Paulo"),
    ): PushRegistroRepository = PushRegistroRepository(
        dataSource = dataSource,
        memoria = memoria,
        appConfigRepository = AppConfigRepositoryFake(pushDiarioEnabled),
        perfilPreferencesRepository = PreferenciasFake(preferencias),
        repositorioCadastroOab = CadastroFake(),
        marcaDagua = { maiorIdConhecido },
        chaveApp = "chave-de-teste",
        clock = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), zona),
        fcm = fcm,
    )

    private fun <T : Any> requireNonNull(valor: T?): T =
        valor ?: throw AssertionError("esperava valor não nulo")

    private class FcmFake : FcmInscricoes {
        var registros = 0
        val inscritos = linkedSetOf<String>()
        val desinscritos = mutableListOf<String>()

        override suspend fun registrar() {
            registros++
        }

        override suspend fun inscrever(topico: String) {
            inscritos += topico
        }

        override suspend fun desinscrever(topico: String) {
            desinscritos += topico
            inscritos -= topico
        }
    }

    private class DataSourceFake : PushRegistroDataSource {
        var ultimaConsulta: PushConsultaRequestDto? = null
        var cancelamentos = 0

        override suspend fun registrarConsulta(
            chaveApp: String,
            pedido: PushConsultaRequestDto,
        ) {
            ultimaConsulta = pedido
        }

        override suspend fun cancelarConsulta(chaveApp: String, instalacao: String) {
            cancelamentos++
        }
    }

    private class MemoriaFake : MemoriaPush {
        private var instalacao: String? = null
        private var topicos: Set<String> = emptySet()

        override fun instalacao(): String? = instalacao

        override fun guardarInstalacao(id: String) {
            instalacao = id
        }

        override fun topicos(): Set<String> = topicos

        override fun guardarTopicos(topicos: Set<String>) {
            this.topicos = topicos
        }
    }

    private class AppConfigRepositoryFake(
        private val pushDiarioEnabled: Boolean,
    ) : AppConfigRepository {
        private val estado = MutableStateFlow(AppConfig(pushDiarioEnabled = pushDiarioEnabled))
        override val config: Flow<AppConfig> = estado
        override suspend fun current(): AppConfig = estado.value
        override suspend fun refresh(): AppConfig = estado.value
    }

    private class PreferenciasFake(
        preferencias: PerfilPreferences,
    ) : PerfilPreferencesRepository {
        private val estado = MutableStateFlow(preferencias)
        override val preferencias: Flow<PerfilPreferences> = estado

        override suspend fun saveIntervaloBuscaDias(dias: Int) = Unit
        override suspend fun saveSincronizacaoAutomatica(ativo: Boolean) = Unit
        override suspend fun saveNotificarPublicacoes(ativo: Boolean) = Unit
        override suspend fun saveNotificarPrazosUrgentes(ativo: Boolean) = Unit
        override suspend fun saveNotificarMovimentacoes(ativo: Boolean) = Unit
        override suspend fun saveTema(tema: TipoTema) = Unit
        override suspend fun saveApenasPorNome(ativo: Boolean) = Unit
        override suspend fun saveSincronizacaoNuvemAutorizada(ativo: Boolean) = Unit
        override suspend fun savePushPreChecagemAutorizada(ativo: Boolean) = Unit
    }

    private class CadastroFake : CadastroOabRepository {
        override val cadastro: Flow<OabCadastro> = MutableStateFlow(
            OabCadastro(numero = "123456", uf = "RS", nomeAdvogado = "Advogada Teste"),
        )
        override val status: Flow<SincronizacaoStatus> = MutableStateFlow(SincronizacaoStatus())

        override suspend fun salvarCadastro(
            numero: String,
            uf: String,
            nomeAdvogado: String?,
            tipoInscricao: String?,
            nomeEscritorio: String?,
            areasAtuacao: List<String>?,
            dataInicio: LocalDate?,
            dataFim: LocalDate?,
        ) = Unit

        override suspend fun registrarSucesso(executadoEm: Instant, novasPublicacoes: Int) = Unit
        override suspend fun registrarFalha(executadoEm: Instant, mensagem: String) = Unit
    }
}
