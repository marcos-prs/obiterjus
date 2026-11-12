package com.obiterjus.data.push

import android.util.Log
import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.data.push.dto.PushConsultaRequestDto
import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.domain.repository.CadastroOabRepository
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Mantém este aparelho alcançável pelo push diário do servidor.
 *
 * Quem acorda o app às 7h é o servidor. WorkManager periódico não garante
 * horário: o job chega ao JobScheduler sem deadline e com a constraint
 * FLEXIBILITY, e em bucket Rare ou Restricted a rede em segundo plano é
 * desligada. FCM de alta prioridade é a única exceção a Doze e a bucket.
 * Desenho completo em `docs/plans/2026-11-11-push-das-7h.md`.
 *
 * Duas fases, e a diferença entre elas é só quanto o servidor sabe:
 *
 * - **Fase 1**, sempre ativa: o aparelho se inscreve num tópico do FCM por
 *   horário e fuso. O servidor empurra para o tópico no momento certo sem saber
 *   nada de quem escuta — nenhum registro nosso, nenhuma chamada de rede aqui.
 * - **Fase 2**, só com [PerfilPreferences.pushPreChecagemAutorizada]: o
 *   servidor passa a consultar o DJEN pela OAB para empurrar apenas quando
 *   houver algo. Custa registrar a OAB no servidor.
 *
 * [sincronizar] é idempotente e pensada para rodar a cada abertura do app:
 * reconcilia as inscrições com o estado atual e falha em silêncio.
 */
class PushRegistroRepository(
    private val dataSource: PushRegistroDataSource,
    private val memoria: MemoriaPush,
    private val appConfigRepository: AppConfigRepository,
    private val perfilPreferencesRepository: PerfilPreferencesRepository,
    private val repositorioCadastroOab: CadastroOabRepository,
    /** Maior `id` de comunicação do DJEN já conhecido localmente. */
    private val marcaDagua: suspend () -> Long?,
    private val chaveApp: String,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val fcm: FcmInscricoes = FirebaseFcmInscricoes(),
) {

    enum class Resultado {
        /** Flag do Remote Config desligada: o servidor ainda não está no ar. */
        DESLIGADO,

        /** Sincronização automática desligada pelo usuário: inscrições removidas. */
        CANCELADO,

        /** Inscrito para acordar cego no horário. O servidor não sabe de nós. */
        REGISTRADO,

        /** Inscrito e com pré-checagem: o servidor consulta o DJEN pela OAB. */
        REGISTRADO_COM_CONSULTA,

        /** Falhou. A próxima abertura tenta de novo. */
        FALHOU,
    }

    suspend fun sincronizar(): Resultado = try {
        executarSincronizacao()
    } catch (erro: CancellationException) {
        throw erro
    } catch (erro: Exception) {
        Log.w(TAG, "Registro de push falhou; tenta na próxima abertura", erro)
        Resultado.FALHOU
    }

    /**
     * Desfaz tudo: sai dos tópicos e apaga a pré-checagem no servidor. Chamada
     * ao desligar a sincronização automática e na exclusão de conta — sem isso
     * o servidor seguiria acordando um aparelho que pediu para parar.
     */
    suspend fun cancelar(): Boolean = runCatching {
        reconciliarTopicos(desejados = emptySet())
        apagarConsultaSeHavia(tinhaConsulta = true)
        true
    }.getOrElse { erro ->
        if (erro is CancellationException) throw erro
        Log.w(TAG, "Cancelamento do push falhou", erro)
        false
    }

    private suspend fun executarSincronizacao(): Resultado {
        if (!appConfigRepository.current().pushDiarioEnabled) return Resultado.DESLIGADO

        val preferencias = perfilPreferencesRepository.preferencias.first()
        if (!preferencias.sincronizacaoAutomatica) {
            cancelar()
            return Resultado.CANCELADO
        }

        fcm.registrar()

        val offsetMinutos = offsetMinutos()
        val topicoCego = PushTopicos.acordaCego(HORA_ALVO, offsetMinutos)
        val consulta = if (preferencias.pushPreChecagemAutorizada) {
            montarConsulta(preferencias.apenasPorNome, offsetMinutos)
        } else {
            null
        }

        val tinhaConsulta = memoria.topicos().any { it.startsWith(PREFIXO_TOPICO_OAB) }
        reconciliarTopicos(desejados = setOfNotNull(topicoCego, consulta?.topico))

        if (consulta == null) {
            apagarConsultaSeHavia(tinhaConsulta)
            return Resultado.REGISTRADO
        }

        dataSource.registrarConsulta(chaveApp, consulta)
        return Resultado.REGISTRADO_COM_CONSULTA
    }

    private suspend fun montarConsulta(
        apenasPorNome: Boolean,
        offsetMinutos: Int,
    ): PushConsultaRequestDto? {
        if (chaveApp.isBlank()) return null
        val cadastro = repositorioCadastroOab.cadastro.first()
        if (!cadastro.isValid) return null

        return PushConsultaRequestDto(
            instalacao = instalacao(),
            topico = PushTopicos.porOab(cadastro.numero, cadastro.uf, chaveApp),
            horaLocal = HORA_ALVO,
            offsetMinutos = offsetMinutos,
            numeroOab = cadastro.numero.trim(),
            ufOab = cadastro.uf.trim().uppercase(),
            nomeAdvogado = cadastro.nomeAdvogado.trim().takeIf { it.isNotBlank() },
            apenasPorNome = apenasPorNome,
            marcaDagua = marcaDagua() ?: 0L,
        )
    }

    /**
     * Entra nos tópicos que faltam e sai dos que sobraram.
     *
     * A reconciliação importa: o deslocamento de fuso muda em viagem, e a OAB
     * pode ser corrigida no cadastro. Sem sair do tópico antigo o aparelho
     * continuaria sendo acordado no horário errado, para sempre.
     */
    private suspend fun reconciliarTopicos(desejados: Set<String>) {
        val atuais = memoria.topicos()

        (atuais - desejados).forEach { topico ->
            runCatching { fcm.desinscrever(topico) }
                .onFailure { erro ->
                    if (erro is CancellationException) throw erro
                    Log.w(TAG, "Falha ao sair do tópico $topico", erro)
                }
        }
        (desejados - atuais).forEach { topico -> fcm.inscrever(topico) }

        memoria.guardarTopicos(desejados)
    }

    private suspend fun apagarConsultaSeHavia(tinhaConsulta: Boolean) {
        if (!tinhaConsulta || chaveApp.isBlank()) return
        runCatching { dataSource.cancelarConsulta(chaveApp, instalacao()) }
            .onFailure { erro ->
                if (erro is CancellationException) throw erro
                Log.w(TAG, "Falha ao apagar a pré-checagem no servidor", erro)
            }
    }

    /**
     * Identificador opaco deste aparelho: chave da linha no servidor e
     * capacidade para alterá-la. Nasce aqui porque o registro não pode depender
     * de conta — o app não tem gate de login de propósito.
     */
    private fun instalacao(): String {
        memoria.instalacao()?.let { return it }

        val nova = UUID.randomUUID().toString()
        memoria.guardarInstalacao(nova)
        return nova
    }

    /**
     * O emissor varre os fusos de 15 em 15 minutos em UTC e precisa do
     * deslocamento para saber quando são 7h aqui. O Brasil não é um fuso só:
     * Acre e parte do Amazonas não são UTC-3.
     */
    private fun offsetMinutos(): Int =
        clock.zone.rules.getOffset(clock.instant()).totalSeconds / SEGUNDOS_POR_MINUTO

    private companion object {
        const val TAG = "PushRegistro"
        const val HORA_ALVO = 7
        const val SEGUNDOS_POR_MINUTO = 60
        const val PREFIXO_TOPICO_OAB = "oab-"
    }
}
