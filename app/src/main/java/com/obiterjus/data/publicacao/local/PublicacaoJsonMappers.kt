package com.obiterjus.data.publicacao.local

import com.obiterjus.domain.model.PublicacaoParticipante
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal val publicacaoJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

internal fun List<PublicacaoParticipante>.toParticipantesJsonOrNull(): String? =
    takeIf { it.isNotEmpty() }
        ?.let { participantes ->
            publicacaoJson.encodeToString(
                ListSerializer(PublicacaoParticipante.serializer()),
                participantes,
            )
        }

internal fun String?.toParticipantes(): List<PublicacaoParticipante> =
    takeIf { !it.isNullOrBlank() }
        ?.let { raw ->
            runCatching {
                publicacaoJson.decodeFromString(
                    ListSerializer(PublicacaoParticipante.serializer()),
                    raw,
                )
            }.getOrNull()
        }.orEmpty()

internal fun List<String>.toAssuntosJsonOrNull(): String? =
    mapNotNull { assunto -> assunto.trim().takeIf { it.isNotEmpty() } }
        .distinct()
        .takeIf { it.isNotEmpty() }
        ?.let { assuntos ->
            publicacaoJson.encodeToString(
                ListSerializer(String.serializer()),
                assuntos,
            )
        }

internal fun String?.toAssuntos(): List<String> =
    takeIf { !it.isNullOrBlank() }
        ?.let { raw ->
            runCatching {
                publicacaoJson.decodeFromString(
                    ListSerializer(String.serializer()),
                    raw,
                )
            }.getOrNull()
        }.orEmpty()
