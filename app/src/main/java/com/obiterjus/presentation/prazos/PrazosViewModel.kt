package com.obiterjus.presentation.prazos

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.R
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.core.texto.correspondeAoTermoBusca
import com.obiterjus.domain.model.ConfirmacaoPrazoResultado
import com.obiterjus.domain.model.GeneroTribunal
import com.obiterjus.domain.model.PrazoAgendaItem
import com.obiterjus.domain.model.ProvedorCalendario
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.usecase.ConfirmarPrazoUC
import com.obiterjus.domain.usecase.ObservarAgendaPrazos
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PrazosViewModel internal constructor(
    observarAgendaPrazos: ObservarAgendaPrazos,
    private val confirmarPrazoUC: ConfirmarPrazoUC,
    private val clock: Clock,
    private val textos: TextosPrazos,
) : ViewModel() {
    constructor(
        context: Context,
        observarAgendaPrazos: ObservarAgendaPrazos,
        confirmarPrazoUC: ConfirmarPrazoUC,
        clock: Clock,
    ) : this(
        observarAgendaPrazos = observarAgendaPrazos,
        confirmarPrazoUC = confirmarPrazoUC,
        clock = clock,
        textos = ContextTextosPrazos(context),
    )

    private val filtros = MutableStateFlow(FiltrosPrazos())
    private val abaSelecionada = MutableStateFlow(AbaPrazos.TODOS)
    private val confirmandoPrazoId = MutableStateFlow<Long?>(null)
    private val resultadoConfirmacao = MutableStateFlow<ConfirmacaoPrazoResultado?>(null)
    private val ultimaSolicitacaoConfirmacao = MutableStateFlow<SolicitacaoConfirmacaoPrazo?>(null)

    val estado: StateFlow<EstadoPrazos> = combine(
        observarAgendaPrazos(),
        filtros,
        abaSelecionada,
        confirmandoPrazoId,
        resultadoConfirmacao,
    ) { prazos, filtrosAtuais, abaAtiva, idConfirmando, resultado ->
        val hoje = LocalDate.now(clock)
        val tribunaisPorGenero = prazos.tribunaisPorGenero()
        val filtrados = prazos
            .filter { it.atende(filtrosAtuais) }
            .map { prazo ->
                PrazoUiItem(
                    item = prazo,
                    grupo = prazo.grupo(hoje),
                    diasRestantes = prazo.diasRestantes(hoje),
                )
            }

        EstadoPrazos(
            itens = filtrados,
            itensExibidos = filtrados.itensDaAba(abaAtiva),
            filtros = filtrosAtuais,
            tribunaisPorGenero = tribunaisPorGenero,
            abaSelecionada = abaAtiva,
            confirmandoPrazoId = idConfirmando,
            resultadoConfirmacao = resultado,
            urgente = filtrados.filter { it.grupo == GrupoPrazo.URGENTE },
            estaSemana = filtrados.filter { it.grupo == GrupoPrazo.ESTA_SEMANA },
            proximos = filtrados.filter { it.grupo == GrupoPrazo.PROXIMOS },
            expirados = filtrados.filter { it.grupo == GrupoPrazo.EXPIRADOS },
            semData = filtrados.filter { it.grupo == GrupoPrazo.SEM_DATA },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = EstadoPrazos(),
    )

    fun aoAlterarBusca(valor: String) {
        filtros.update { it.copy(busca = valor) }
    }

    fun aoSelecionarTribunal(valor: String?) {
        filtros.update { it.copy(tribunal = valor.orEmpty()) }
    }

    fun aoSelecionarAba(aba: AbaPrazos) {
        abaSelecionada.value = aba
    }

    fun aoConfirmarPrazo(
        publicacaoId: Long,
        provedor: ProvedorCalendario,
    ) {
        ultimaSolicitacaoConfirmacao.value = SolicitacaoConfirmacaoPrazo(
            publicacaoId = publicacaoId,
            provedor = provedor,
        )

        viewModelScope.launch {
            confirmandoPrazoId.value = publicacaoId
            val item = estado.value.itens.firstOrNull { it.item.publicacao.id == publicacaoId }
                ?: run {
                    confirmandoPrazoId.value = null
                    resultadoConfirmacao.value = ConfirmacaoPrazoResultado.Falha
                    return@launch
                }

            val prazo = item.item.prazo
            val pub = item.item.publicacao
            val numeroProcesso = pub.numeroProcesso
                ?: textos.get(R.string.prazos_sem_processo)
            val title = textos.get(R.string.prazos_confirmacao_titulo, numeroProcesso)
            val description = buildConfirmacaoDescricao(
                prazo = prazo,
                publicacao = pub,
                textos = textos,
            )

            val result = confirmarPrazoUC.invoke(
                publicacaoId = publicacaoId,
                prazo = prazo,
                title = title,
                description = description,
                provedor = provedor,
            )

            confirmandoPrazoId.value = null
            resultadoConfirmacao.value = result.getOrNull() ?: ConfirmacaoPrazoResultado.Falha
        }
    }

    fun aoRepetirUltimaConfirmacao() {
        ultimaSolicitacaoConfirmacao.value?.let { solicitacao ->
            aoConfirmarPrazo(
                publicacaoId = solicitacao.publicacaoId,
                provedor = solicitacao.provedor,
            )
        }
    }

    fun aoConsumirResultadoConfirmacao() {
        resultadoConfirmacao.value = null
    }

    private fun PrazoAgendaItem.atende(filtros: FiltrosPrazos): Boolean {
        val busca = filtros.busca.trim()
        val atendeBusca = when {
            busca.isBlank() -> true
            listOfNotNull(
                publicacao.numeroProcesso,
                publicacao.tribunal,
                publicacao.tipoComunicacao,
                publicacao.nomeOrgao,
                publicacao.textoLimpo,
                prazo.textoOriginal,
            ).correspondeAoTermoBusca(busca) -> true
            else -> publicacao.participantes.any { participante ->
                participante.camposBusca().correspondeAoTermoBusca(busca)
            }
        }
        val atendeTribunal = filtros.tribunal.isBlank() ||
            publicacao.tribunal?.contains(filtros.tribunal, ignoreCase = true) == true

        return atendeBusca && atendeTribunal
    }

    private fun PrazoAgendaItem.grupo(hoje: LocalDate): GrupoPrazo {
        val vencimento = prazo.dataLimiteEstimada ?: return GrupoPrazo.SEM_DATA
        val dias = ChronoUnit.DAYS.between(hoje, vencimento)
        return when {
            dias < 0 -> GrupoPrazo.EXPIRADOS
            dias <= LIMITE_URGENTE_DIAS -> GrupoPrazo.URGENTE
            dias <= LIMITE_SEMANA_DIAS -> GrupoPrazo.ESTA_SEMANA
            else -> GrupoPrazo.PROXIMOS
        }
    }

    private fun PrazoAgendaItem.diasRestantes(hoje: LocalDate): Int? =
        prazo.dataLimiteEstimada?.let { ChronoUnit.DAYS.between(hoje, it).toInt() }

    private fun List<PrazoUiItem>.itensDaAba(aba: AbaPrazos): List<PrazoUiItem> =
        when (aba) {
            AbaPrazos.TODOS -> this
            AbaPrazos.VENCIDOS -> filter { it.grupo == GrupoPrazo.EXPIRADOS }
            AbaPrazos.PROXIMOS -> filter {
                it.grupo == GrupoPrazo.URGENTE || it.grupo == GrupoPrazo.ESTA_SEMANA || it.grupo == GrupoPrazo.PROXIMOS
            }
            AbaPrazos.SEM_DATA -> filter { it.grupo == GrupoPrazo.SEM_DATA }
        }

    private companion object {
        const val LIMITE_URGENTE_DIAS = 2L
        const val LIMITE_SEMANA_DIAS = 5L
    }
}

data class EstadoPrazos(
    val itens: List<PrazoUiItem> = emptyList(),
    val itensExibidos: List<PrazoUiItem> = emptyList(),
    val filtros: FiltrosPrazos = FiltrosPrazos(),
    val tribunaisPorGenero: Map<GeneroTribunal, List<String>> = emptyMap(),
    val abaSelecionada: AbaPrazos = AbaPrazos.TODOS,
    val confirmandoPrazoId: Long? = null,
    val resultadoConfirmacao: ConfirmacaoPrazoResultado? = null,
    val urgente: List<PrazoUiItem> = emptyList(),
    val estaSemana: List<PrazoUiItem> = emptyList(),
    val proximos: List<PrazoUiItem> = emptyList(),
    val expirados: List<PrazoUiItem> = emptyList(),
    val semData: List<PrazoUiItem> = emptyList(),
)

data class FiltrosPrazos(
    val busca: String = "",
    val tribunal: String = "",
)

data class PrazoUiItem(
    val item: PrazoAgendaItem,
    val grupo: GrupoPrazo,
    val diasRestantes: Int?,
)

data class SolicitacaoConfirmacaoPrazo(
    val publicacaoId: Long,
    val provedor: ProvedorCalendario,
)

enum class GrupoPrazo {
    URGENTE,
    ESTA_SEMANA,
    PROXIMOS,
    EXPIRADOS,
    SEM_DATA,
}

enum class AbaPrazos {
    TODOS,
    VENCIDOS,
    PROXIMOS,
    SEM_DATA,
}

internal interface TextosPrazos {
    fun get(@StringRes resId: Int): String
    fun get(@StringRes resId: Int, vararg args: Any): String
}

private class ContextTextosPrazos(
    private val context: Context,
) : TextosPrazos {
    override fun get(resId: Int): String = context.getString(resId)

    override fun get(resId: Int, vararg args: Any): String = context.getString(resId, *args)
}

private fun List<PrazoAgendaItem>.tribunaisPorGenero(): Map<GeneroTribunal, List<String>> =
    mapNotNull { item -> item.publicacao.tribunal?.trim()?.takeIf { it.isNotBlank() } }
        .distinctBy { tribunal -> tribunal.uppercase() }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
        .groupBy { tribunal -> GeneroTribunal.classificar(tribunal) }

private fun com.obiterjus.domain.model.PublicacaoParticipante.camposBusca(): List<String?> {
    val combinado = listOfNotNull(nome, documento).joinToString(" ").trim()
    return listOfNotNull(
        nome,
        documento,
        combinado.takeIf { it.isNotBlank() },
    )
}

private fun buildConfirmacaoDescricao(
    prazo: PublicacaoPrazo,
    publicacao: Publicacao,
    textos: TextosPrazos,
): String {
    val sb = StringBuilder()
    sb.appendLine("Processo: ${publicacao.numeroProcesso ?: "Não informado"}")
    sb.appendLine("Prazo: ${prazo.quantidade} ${prazo.unidade} — ${prazo.textoOriginal}")
    val dataLimite = prazo.dataLimiteEstimada?.let { FormatadorData.formatarData(it) }
        ?: "Não estimada"
    sb.appendLine("Data limite: $dataLimite")
    val tipoAto = publicacao.tipoComunicacao ?: "Não informado"
    sb.appendLine("Tipo do ato: $tipoAto")
    val conteudo = publicacao.textoLimpo?.trim().orEmpty()
    if (conteudo.isNotBlank()) {
        sb.appendLine()
        sb.append(conteudo)
    }
    return sb.toString()
}
