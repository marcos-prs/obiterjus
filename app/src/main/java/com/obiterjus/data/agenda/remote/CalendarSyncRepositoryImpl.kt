package com.obiterjus.data.agenda.remote

import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.repository.CalendarSyncRepository
import java.time.format.DateTimeFormatter

class CalendarSyncRepositoryImpl(
    private val googleDataSource: GoogleCalendarDataSource,
    private val outlookDataSource: OutlookCalendarDataSource
) : CalendarSyncRepository {

    override suspend fun syncPrazo(
        prazo: PublicacaoPrazo,
        title: String,
        description: String,
        provedor: String
    ): Result<String> = try {
        val dateStr = prazo.dataLimiteEstimada?.format(DateTimeFormatter.ISO_LOCAL_DATE) 
            ?: return Result.failure(Exception("Prazo sem data limite"))

        when (provedor.uppercase()) {
            "GOOGLE" -> {
                val event = GoogleEventDto(
                    summary = title,
                    description = description,
                    start = EventDateTime(date = dateStr),
                    end = EventDateTime(date = dateStr) // All day event uses 'date' instead of 'dateTime'
                )
                val response = googleDataSource.createEvent(event)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.id)
                } else {
                    Result.failure(Exception("Erro Google: ${response.code()}"))
                }
            }
            "OUTLOOK" -> {
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
                    Result.failure(Exception("Erro Outlook: ${response.code()}"))
                }
            }
            else -> Result.failure(Exception("Provedor desconhecido: $provedor"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun cancelPrazo(idExterno: String, provedor: String): Result<Unit> = try {
        when (provedor.uppercase()) {
            "GOOGLE" -> {
                val response = googleDataSource.deleteEvent(idExterno)
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Erro Google: ${response.code()}"))
            }
            "OUTLOOK" -> {
                val response = outlookDataSource.deleteEvent(idExterno)
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Erro Outlook: ${response.code()}"))
            }
            else -> Result.failure(Exception("Provedor desconhecido: $provedor"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
