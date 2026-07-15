package com.obiterjus.data.djen

import com.obiterjus.core.parser.NumeroProcessoNormalizer
import com.obiterjus.data.datajud.local.ParticipanteEntity
import com.obiterjus.data.processo.ProcessoDadosResolver
import com.obiterjus.data.processo.local.LocalProcessoRepository
import com.obiterjus.data.processo.local.ProcessoEntity
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.PublicacaoParticipante
import com.obiterjus.domain.repository.PublicacoesRepository
import java.security.MessageDigest
import kotlinx.coroutines.flow.first

class DjenPartesResolver(
    private val publicacoesRepository: PublicacoesRepository,
    private val localProcessoRepository: LocalProcessoRepository,
) {
    suspend fun atualizarPartesDosProcessos(numerosProcesso: Collection<String>): ResumoResolucaoPartes {
        val processosNormalizados = numerosProcesso
            .mapNotNull(NumeroProcessoNormalizer::normalize)
            .distinct()

        var processosAtualizados = 0
        var participantesInseridos = 0
        var processosSemeados = 0

        processosNormalizados.forEach { numeroProcesso ->
            val publicacoes = publicacoesRepository.observarPublicacoesProcesso(numeroProcesso).first()
            if (publicacoes.isEmpty()) return@forEach

            if (semearOuEnriquecerProcesso(numeroProcesso, publicacoes)) {
                processosSemeados += 1
            }

            val candidatos = publicacoes
                .asSequence()
                .flatMap { it.participantes.asSequence() }
                .mapNotNull { participante -> participante.toParticipanteEntity(numeroProcesso) }
                .toList()

            if (candidatos.isEmpty()) return@forEach

            val existentes = localProcessoRepository.getParticipantes(numeroProcesso)
            val merged = ProcessoDadosResolver.mesclarParticipantes(
                existentes = existentes,
                novos = candidatos,
            )

            if (merged.toSet() != existentes.toSet()) {
                localProcessoRepository.replaceParticipantes(numeroProcesso, merged)
                processosAtualizados += 1
                participantesInseridos += (merged.size - existentes.size).coerceAtLeast(0)
            }
        }

        return ResumoResolucaoPartes(
            processosAtualizados = processosAtualizados,
            participantesInseridos = participantesInseridos,
            processosSemeados = processosSemeados,
        )
    }

    /**
     * Garante que todo processo citado em publicação exista localmente com o
     * que o DJEN já informa (tribunal e órgão), antes ou independentemente do
     * DataJud responder. O processo é criado mesmo sem tribunal/órgão
     * conhecidos — fica PENDING para o DataJud enriquecer depois.
     * Retorna true quando criou ou completou o registro.
     */
    private suspend fun semearOuEnriquecerProcesso(
        numeroProcesso: String,
        publicacoes: List<Publicacao>,
    ): Boolean {
        val tribunal = publicacoes.firstNotNullOfOrNull { it.tribunal?.trim()?.takeIf(String::isNotEmpty) }
        val nomeOrgao = publicacoes.firstNotNullOfOrNull { it.nomeOrgao?.trim()?.takeIf(String::isNotEmpty) }

        val existente = localProcessoRepository.getProcesso(numeroProcesso)
        if (existente == null) {
            val capturadoEm = publicacoes.minOf { it.capturadoEm }
            localProcessoRepository.upsertProcesso(
                ProcessoEntity(
                    numeroProcesso = numeroProcesso,
                    tribunal = tribunal,
                    grau = null,
                    classeCodigo = null,
                    classeNome = null,
                    assuntosJson = null,
                    orgaoJulgadorCodigo = null,
                    orgaoJulgadorNome = nomeOrgao,
                    nivelSigilo = null,
                    dataAjuizamento = null,
                    syncStatus = ProcessoSyncStatus.PENDING,
                    capturadoEm = capturadoEm,
                    atualizadoEm = capturadoEm,
                ),
            )
            return true
        }

        if (tribunal == null && nomeOrgao == null) return false

        val precisaTribunal = existente.tribunal == null && tribunal != null
        val precisaOrgao = existente.orgaoJulgadorNome == null && nomeOrgao != null
        if (!precisaTribunal && !precisaOrgao) return false

        localProcessoRepository.upsertProcesso(
            existente.copy(
                tribunal = existente.tribunal ?: tribunal,
                orgaoJulgadorNome = existente.orgaoJulgadorNome ?: nomeOrgao,
            ),
        )
        return true
    }

    private fun PublicacaoParticipante.toParticipanteEntity(numeroProcesso: String): ParticipanteEntity? {
        val nome = nome.trim().takeIf { it.isNotBlank() } ?: return null
        val tipoNormalizado = tipo.trim().takeIf { it.isNotBlank() } ?: return null
        val polo = ProcessoDadosResolver.normalizarPolo(tipoNormalizado)
            ?.takeIf { it == "ATIVO" || it == "PASSIVO" }
            ?: tipoNormalizado.toPoloProcessual()

        if (polo == null && !tipoNormalizado.isParticipacaoRelevante()) {
            return null
        }

        return ParticipanteEntity(
            idLocal = participanteId(
                numeroProcesso = numeroProcesso,
                polo = polo,
                tipoParticipacao = tipoNormalizado,
                nome = nome,
            ),
            numeroProcesso = numeroProcesso,
            polo = polo,
            nome = nome,
            tipoPessoa = null,
            tipoParticipacao = tipoNormalizado,
        )
    }

    private fun String.toPoloProcessual(): String? =
        when (lowercase().trim()) {
            "autor", "autora", "requerente", "exequente", "impetrante", "agravante", "apelante", "ativo", "ativa", "polo ativo" -> "ATIVO"
            "réu", "reu", "requerido", "executado", "impetrado", "agravado", "apelado", "passivo", "passiva", "polo passivo" -> "PASSIVO"
            else -> null
        }

    private fun String.isParticipacaoRelevante(): Boolean =
        lowercase().trim() in setOf(
            "advogado",
            "destinatário",
            "destinatario",
            "parte",
            "partes",
            "autor",
            "autora",
            "réu",
            "reu",
            "requerente",
            "requerido",
            "exequente",
            "executado",
            "impetrante",
            "impetrado",
            "agravante",
            "agravado",
            "apelante",
            "apelado",
            "ativo",
            "ativa",
            "polo ativo",
            "passivo",
            "passiva",
            "polo passivo",
        )

    private fun participanteId(
        numeroProcesso: String,
        polo: String?,
        tipoParticipacao: String,
        nome: String,
    ): String {
        val raw = listOf(
            numeroProcesso,
            polo.orEmpty().uppercase(),
            tipoParticipacao.trim().lowercase(),
            nome.trim().lowercase(),
        ).joinToString("|")

        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}

data class ResumoResolucaoPartes(
    val processosAtualizados: Int,
    val participantesInseridos: Int,
    val processosSemeados: Int = 0,
)
