package com.obiterjus.data.agenda.remote

import android.content.Context
import android.util.Log
import com.obiterjus.data.agenda.worker.CalendarSyncScheduler
import com.obiterjus.domain.model.ProvedorCalendario
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.repository.CalendarSyncRepository
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import retrofit2.Response

class CalendarSyncRepositoryImpl(
    private val context: Context,
    private val googleDataSource: GoogleCalendarDataSource,
    private val outlookDataSource: OutlookCalendarDataSource,
    private val googleCalendarTokenRepository: GoogleCalendarTokenRepository,
) : CalendarSyncRepository {

    override suspend fun syncPrazo(
        prazo: PublicacaoPrazo,
        title: String,
        description: String,
        provedor: ProvedorCalendario,
    ): Result<String> = try {
        val dateStr = prazo.dataLimiteEstimada?.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ?: return Result.failure(Exception("Prazo sem data limite"))

        when (provedor) {
            ProvedorCalendario.GOOGLE -> {
                val event = GoogleEventDto(
                    summary = title,
                    description = description,
                    start = EventDateTime(date = dateStr),
                    end = EventDateTime(date = dateStr),
                )
                val response = googleDataSource.createEvent(event)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.id)
                } else {
                    Result.failure(tratarFalhaGoogle("createEvent Google", response))
                }
            }
            ProvedorCalendario.OUTLOOK -> {
                val event = OutlookEventDto(
                    subject = title,
                    body = OutlookBodyDto(content = description),
                    start = OutlookDateTimeTimeZone(dateTime = "${dateStr}T00:00:00", timeZone = "UTC"),
                    end = OutlookDateTimeTimeZone(dateTime = "${dateStr}T23:59:59", timeZone = "UTC")
                )
                val response = outlookDataSource.createEvent(event)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.id)
                } else {
                    logErro("createEvent Outlook", response, null)
                    Result.failure(Exception("Erro Outlook: ${response.code()}"))
                }
            }
            ProvedorCalendario.LOCAL -> Result.failure(Exception("Provedor local não envia para calendário"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "syncPrazo falhou (provedor=$provedor)", e)
        CalendarSyncScheduler.agendar(context)
        Result.failure(e)
    }

    override suspend fun cancelPrazo(idExterno: String, provedor: ProvedorCalendario): Result<Unit> = try {
        when (provedor) {
            ProvedorCalendario.GOOGLE -> {
                val response = googleDataSource.deleteEvent(idExterno)
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(tratarFalhaGoogle("deleteEvent Google", response, reagendar = false))
            }
            ProvedorCalendario.OUTLOOK -> {
                val response = outlookDataSource.deleteEvent(idExterno)
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Erro Outlook: ${response.code()}"))
            }
            ProvedorCalendario.LOCAL -> Result.failure(Exception("Provedor local não envia para calendário"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "cancelPrazo falhou (provedor=$provedor)", e)
        Result.failure(e)
    }

    /**
     * Um 403 do Google Agenda quase nunca quer dizer "token ruim".
     *
     * A versão anterior apagava o access token em qualquer 401/403, então o caso
     * mais comum — a Calendar API não estar habilitada no projeto do Google Cloud,
     * que responde 403 `accessNotConfigured` — descartava um token perfeitamente
     * válido e mandava o usuário reautorizar para sempre, sem nunca dizer o motivo.
     * Agora só o que é de fato problema de credencial derruba o token.
     */
    private fun tratarFalhaGoogle(
        operacao: String,
        response: Response<*>,
        reagendar: Boolean = true,
    ): Exception {
        val corpo = response.errorBody()
            ?.let { body -> runCatching { body.string().take(1000) }.getOrNull() }
        logErro(operacao, response, corpo)

        val motivo = corpo?.let { REGEX_REASON.find(it)?.groupValues?.getOrNull(1) }
        val codigo = response.code()
        val credencialInvalida = codigo == 401 ||
            (codigo == 403 && motivo in MOTIVOS_DE_CREDENCIAL)

        if (credencialInvalida) {
            googleCalendarTokenRepository.clearAccessToken()
        } else if (reagendar) {
            // Falha transitória: o prazo fica pendente e o worker tenta de novo.
            CalendarSyncScheduler.agendar(context)
        }

        val explicacao = when {
            codigo == 401 ->
                "A autorização do Google Agenda expirou. Autorize novamente."
            motivo == "accessNotConfigured" ->
                "A Google Calendar API não está habilitada no projeto do Google Cloud " +
                    "(obiterjus). Habilite-a no console e tente de novo."
            motivo == "insufficientPermissions" ->
                "A autorização concedida não cobre o Google Agenda. Autorize novamente " +
                    "e aceite o acesso à agenda."
            codigo == 403 ->
                "O Google recusou a gravação na agenda (403${motivo?.let { ", $it" }.orEmpty()})."
            codigo == 429 ->
                "O Google Agenda está limitando as requisições. Tentaremos de novo mais tarde."
            else -> "Erro Google: $codigo"
        }
        return Exception(explicacao)
    }

    private fun logErro(operacao: String, response: Response<*>, corpo: String?) {
        Log.w(TAG, "$operacao HTTP ${response.code()}" + (corpo?.let { " corpo=$it" } ?: ""))
    }

    private companion object {
        const val TAG = "CalendarSync"
        val REGEX_REASON = """"reason"\s*:\s*"([^"]+)"""".toRegex()
        val MOTIVOS_DE_CREDENCIAL = setOf(
            "authError",
            "insufficientPermissions",
            "forbidden",
        )
    }
}
