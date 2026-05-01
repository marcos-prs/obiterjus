package com.obiterjus.data.agenda.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Response

@Serializable
data class GoogleEventDto(
    val summary: String,
    val description: String,
    val start: EventDateTime,
    val end: EventDateTime
)

@Serializable
data class EventDateTime(
    val dateTime: String? = null,
    val date: String? = null,
    val timeZone: String? = null
)

interface GoogleCalendarDataSource {
    @POST("calendar/v3/calendars/primary/events")
    suspend fun createEvent(@Body event: GoogleEventDto): Response<GoogleEventResponseDto>

    @DELETE("calendar/v3/calendars/primary/events/{eventId}")
    suspend fun deleteEvent(@Path("eventId") eventId: String): Response<Unit>
}

@Serializable
data class GoogleEventResponseDto(
    val id: String
)

interface OutlookCalendarDataSource {
    @POST("v1.0/me/events")
    suspend fun createEvent(@Body event: OutlookEventDto): Response<OutlookEventResponseDto>

    @DELETE("v1.0/me/events/{eventId}")
    suspend fun deleteEvent(@Path("eventId") eventId: String): Response<Unit>
}

@Serializable
data class OutlookEventDto(
    val subject: String,
    val body: OutlookBodyDto,
    val start: OutlookDateTimeTimeZone,
    val end: OutlookDateTimeTimeZone
)

@Serializable
data class OutlookBodyDto(
    val contentType: String = "HTML",
    val content: String
)

@Serializable
data class OutlookDateTimeTimeZone(
    val dateTime: String,
    val timeZone: String
)

@Serializable
data class OutlookEventResponseDto(
    val id: String
)
