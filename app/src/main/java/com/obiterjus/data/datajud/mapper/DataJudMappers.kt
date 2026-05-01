package com.obiterjus.data.datajud.mapper

import com.obiterjus.core.parser.CnjDateParser
import com.obiterjus.core.parser.NumeroProcessoNormalizer
import com.obiterjus.data.datajud.local.MovimentoEntity
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.datajud.remote.dto.DataJudMovimentoDto
import com.obiterjus.data.datajud.remote.dto.DataJudProcessoDto
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.data.publicacao.local.toAssuntosJsonOrNull
import com.obiterjus.domain.model.ProcessoSyncStatus
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private val json = Json {
    explicitNulls = false
}

fun DataJudProcessoDto.toProcessoEntity(
    fallbackNumeroProcesso: String,
    fallbackTribunal: String?,
    syncedAt: Instant,
    syncStatus: ProcessoSyncStatus,
): ProcessoEntity {
    val numeroNormalizado = NumeroProcessoNormalizer.normalize(numeroProcesso)
        ?: fallbackNumeroProcesso

    return ProcessoEntity(
        numeroProcesso = numeroNormalizado,
        tribunal = tribunal?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackTribunal,
        grau = grau?.trim()?.takeIf { it.isNotEmpty() },
        classeCodigo = classe?.codigo,
        classeNome = classe?.nome?.trim()?.takeIf { it.isNotEmpty() },
        assuntosJson = assuntos.mapNotNull { assunto ->
            assunto.nome?.trim()?.takeIf { it.isNotEmpty() }
        }.toAssuntosJsonOrNull(),
        orgaoJulgadorCodigo = orgaoJulgador?.codigo,
        orgaoJulgadorNome = orgaoJulgador?.nome?.trim()?.takeIf { it.isNotEmpty() },
        nivelSigilo = nivelSigilo,
        dataAjuizamento = CnjDateParser.parseInstant(dataAjuizamento),
        syncStatus = syncStatus,
        capturadoEm = syncedAt,
        atualizadoEm = syncedAt,
    )
}

fun DataJudProcessoDto.toMovimentoEntities(numeroProcesso: String): List<MovimentoEntity> =
    movimentos.mapIndexed { index, movimento ->
        movimento.toMovimentoEntity(
            numeroProcesso = numeroProcesso,
            fallbackIndex = index,
        )
    }

private fun DataJudMovimentoDto.toMovimentoEntity(
    numeroProcesso: String,
    fallbackIndex: Int,
): MovimentoEntity {
    val dataHoraInstant = CnjDateParser.parseInstant(dataHora)
    val cleanedName = nome?.trim()?.takeIf { it.isNotEmpty() }

    return MovimentoEntity(
        idLocal = movimentoId(
            numeroProcesso = numeroProcesso,
            codigo = codigo,
            dataHora = dataHoraInstant,
            nome = cleanedName,
            fallbackIndex = fallbackIndex,
        ),
        numeroProcesso = numeroProcesso,
        codigo = codigo,
        nome = cleanedName,
        dataHora = dataHoraInstant,
        complementosJson = complementosTabelados.toJsonOrNull(),
    )
}

private fun movimentoId(
    numeroProcesso: String,
    codigo: Int?,
    dataHora: Instant?,
    nome: String?,
    fallbackIndex: Int,
): String {
    val raw = listOf(
        numeroProcesso,
        codigo?.toString().orEmpty(),
        dataHora?.toString().orEmpty(),
        nome.orEmpty(),
        fallbackIndex.toString(),
    ).joinToString("|")

    return MessageDigest.getInstance("SHA-256")
        .digest(raw.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private fun List<JsonElement>.toJsonOrNull(): String? =
    takeIf { it.isNotEmpty() }
        ?.let { json.encodeToString(ListSerializer(JsonElement.serializer()), it) }

fun DataJudProcessoDto.toParticipanteEntities(numeroProcesso: String): List<ParticipanteEntity> {
    return polos.flatMap { poloDto ->
        poloDto.partes.mapIndexed { index, parte ->
            val nome = parte.pessoa?.nome?.trim()?.takeIf { it.isNotEmpty() }
            val poloNome = poloDto.polo?.trim()?.takeIf { it.isNotEmpty() }
            
            val rawId = listOf(
                numeroProcesso,
                poloNome.orEmpty(),
                nome.orEmpty(),
                index.toString()
            ).joinToString("|")

            val idLocal = MessageDigest.getInstance("SHA-256")
                .digest(rawId.toByteArray())
                .joinToString(separator = "") { byte -> "%02x".format(byte) }

            ParticipanteEntity(
                idLocal = idLocal,
                numeroProcesso = numeroProcesso,
                polo = poloNome,
                nome = nome,
                tipoPessoa = parte.pessoa?.tipoPessoa?.trim()?.takeIf { it.isNotEmpty() },
                tipoParticipacao = parte.tipoParticipacao?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }
}
