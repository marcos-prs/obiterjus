package com.obiterjus.presentation.inicio

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.R
import com.obiterjus.domain.model.PrazoAgendaItem
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.repository.RepositorioCadastroOab
import com.obiterjus.domain.usecase.ObservarAgendaPrazos
import com.obiterjus.domain.usecase.ObservarProcessos
import com.obiterjus.domain.usecase.ObservarPublicacoes
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class EstadoInicio(
    val nomeUsuario: String = "",
    val oab: String = "",
    val uf: String = "",
    val totalProcessos: Int = 0,
    val totalPublicacoes: Int = 0,
    val prazosUrgentes: Int = 0,
    val prazosVencidos: Int = 0,
    val prazosProximos: Int = 0,
    val ultimaSincronizacao: Instant? = null,
    val ultimaSincronizacaoTexto: String? = null,
    val publicacoesRecentes: List<Publicacao> = emptyList(),
    val temPrazoUrgente: Boolean = false,
    val prazoUrgenteMensagem: String? = null,
)

class ModeloInicio(
    private val context: Context,
    observarProcessos: ObservarProcessos,
    observarPublicacoes: ObservarPublicacoes,
    observarAgendaPrazos: ObservarAgendaPrazos,
    repositorioCadastroOab: RepositorioCadastroOab,
    private val clock: Clock,
) : ViewModel() {

    val estado: StateFlow<EstadoInicio> = combine(
        repositorioCadastroOab.cadastro,
        repositorioCadastroOab.status,
        combine(
            observarProcessos(),
            observarPublicacoes(),
            observarAgendaPrazos(),
        ) { processos, publicacoes, prazos ->
            DadosInicio(processos.size, publicacoes, prazos)
        },
        tickerMinuto(),
    ) { cadastro, status, dados, agora ->
        val prazosComVencimento = dados.prazos.filter { it.prazo.dataLimiteEstimada != null }
        val prazosVencidos = prazosComVencimento.count { prazo ->
            ChronoUnit.DAYS.between(java.time.LocalDate.now(clock), prazo.prazo.dataLimiteEstimada) < 0
        }
        val prazosUrgentes = prazosComVencimento.filter { prazo ->
            val dias = ChronoUnit.DAYS.between(java.time.LocalDate.now(clock), prazo.prazo.dataLimiteEstimada)
            dias in DIAS_HOJE..DIAS_URGENTE
        }
        val primeiroPrazoUrgente = prazosUrgentes.minByOrNull {
            it.prazo.dataLimiteEstimada ?: java.time.LocalDate.MAX
        }

        EstadoInicio(
            nomeUsuario = cadastro.nomeAdvogado,
            oab = cadastro.numero,
            uf = cadastro.uf,
            totalProcessos = dados.totalProcessos,
            totalPublicacoes = dados.publicacoes.size,
            prazosUrgentes = prazosUrgentes.size,
            prazosVencidos = prazosVencidos,
            prazosProximos = prazosComVencimento.count { prazo ->
                val dias = ChronoUnit.DAYS.between(java.time.LocalDate.now(clock), prazo.prazo.dataLimiteEstimada)
                dias in (DIAS_URGENTE + 1)..DIAS_PROXIMO
            },
            ultimaSincronizacao = status.ultimoSucessoEm,
            ultimaSincronizacaoTexto = status.ultimoSucessoEm?.formatarTempoRelativo(agora),
            publicacoesRecentes = dados.publicacoes
                .sortedWith(
                    compareByDescending<Publicacao> { it.dataDisponibilizacao }
                        .thenByDescending { it.capturadoEm },
                )
                .take(QUANTIDADE_PUBLICACOES_RECENTES),
            temPrazoUrgente = prazosUrgentes.isNotEmpty(),
            prazoUrgenteMensagem = primeiroPrazoUrgente?.mensagemUrgencia(java.time.LocalDate.now(clock)),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoInicio(),
    )

    private fun tickerMinuto(): Flow<Instant> = flow {
        while (true) {
            emit(Instant.now(clock))
            delay(60.seconds)
        }
    }

    private fun Instant.formatarTempoRelativo(agora: Instant): String {
        val minutos = ChronoUnit.MINUTES.between(this, agora).coerceAtLeast(0)
        return when {
            minutos < 1 -> context.getString(R.string.sincronizado_agora)
            minutos < MINUTOS_POR_HORA -> context.getString(R.string.sincronizado_minutos, minutos)
            minutos < MINUTOS_POR_DIA -> context.getString(R.string.sincronizado_horas, minutos / MINUTOS_POR_HORA)
            else -> context.getString(R.string.sincronizado_dias, minutos / MINUTOS_POR_DIA)
        }
    }

    private fun PrazoAgendaItem.mensagemUrgencia(hoje: java.time.LocalDate): String {
        val vencimento = prazo.dataLimiteEstimada ?: hoje
        val dias = ChronoUnit.DAYS.between(hoje, vencimento).toInt().coerceAtLeast(0)
        val processo = publicacao.numeroProcesso ?: ""
        return when (dias) {
            0 -> context.getString(R.string.inicio_prazo_vence_hoje, UM, processo)
            1 -> context.getString(R.string.inicio_prazo_vence_amanha, UM, processo)
            else -> context.getString(R.string.inicio_prazo_vence_em_dias, UM, dias, processo)
        }
    }

    private companion object {
        const val QUANTIDADE_PUBLICACOES_RECENTES = 5
        const val DIAS_HOJE = 0L
        const val DIAS_URGENTE = 2L
        const val DIAS_PROXIMO = 7L
        const val MINUTOS_POR_HORA = 60
        const val MINUTOS_POR_DIA = 1_440
        const val UM = 1
    }
}

private data class DadosInicio(
    val totalProcessos: Int,
    val publicacoes: List<Publicacao>,
    val prazos: List<PrazoAgendaItem>,
)
