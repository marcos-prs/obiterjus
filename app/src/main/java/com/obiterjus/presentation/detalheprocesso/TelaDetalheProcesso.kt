package com.obiterjus.presentation.detalheprocesso

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.cards.CardPublicacao
import com.obiterjus.presentation.componentes.cards.PrioridadeStripe
import com.obiterjus.presentation.componentes.chips.BadgeTipoAto
import com.obiterjus.presentation.componentes.chips.VarianteBadge
import com.obiterjus.presentation.componentes.timeline.CorPontoTimeline
import com.obiterjus.presentation.componentes.timeline.ItemTimeline
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun TelaDetalheProcesso(
    viewModel: ModeloDetalheProcesso,
    numeroProcesso: String,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(numeroProcesso) {
        viewModel.aoCarregar(numeroProcesso)
    }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = dimens.topAppBarPaddingH,
                    vertical = dimens.cardPaddingV,
                ),
                verticalArrangement = Arrangement.spacedBy(dimens.space1),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.IconButton(onClick = onVoltar) {
                        androidx.compose.material3.Icon(
                            imageVector = ObiterIcones.Voltar,
                            contentDescription = stringResource(R.string.cd_voltar),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Text(
                        text = estado.numeroProcesso.truncarNumero(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    text = listOf(estado.tribunal, estado.orgaoJulgador)
                        .filter(String::isNotBlank)
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenMargin)
                .padding(top = dimens.cardGap),
            verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
        ) {
            HeaderDetalhe(estado = estado)

            TabRow(
                selectedTabIndex = estado.abaSelecionada,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimens.cardRadius)),
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {
                    HorizontalDivider(
                        color = colors.divider,
                        thickness = dimens.borderWidth,
                    )
                },
                indicator = { tabs ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabs[estado.abaSelecionada]),
                        color = colors.accent,
                    )
                },
            ) {
                estado.abas.forEachIndexed { index, aba ->
                    Tab(
                        selected = estado.abaSelecionada == index,
                        onClick = { viewModel.aoSelecionarAba(index) },
                        text = {
                            Text(
                                text = aba.rotulo(),
                                style = if (estado.abaSelecionada == index) {
                                    MaterialTheme.typography.labelLarge
                                } else {
                                    MaterialTheme.typography.labelLarge
                                },
                                color = if (estado.abaSelecionada == index) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    colors.textMuted
                                },
                            )
                        },
                    )
                }
            }
        }

        when (estado.abaSelecionada) {
            0 -> ConteudoTimeline(
                timeline = estado.timeline,
                modifier = Modifier.weight(1f),
            )
            1 -> ConteudoPublicacoes(
                publicacoes = estado.publicacoes,
                modifier = Modifier.weight(1f),
            )
            2 -> ConteudoPrazos(
                prazos = estado.prazos,
                modifier = Modifier.weight(1f),
            )
            else -> ConteudoInformacoes(
                informacoes = estado.informacoes,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeaderDetalhe(estado: EstadoDetalheProcesso) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors

    Surface(
        shape = RoundedCornerShape(dimens.cardRadius),
        color = colors.surfacePergaminho,
        border = BorderStroke(dimens.borderWidth, colors.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(dimens.cardPaddingH),
            verticalArrangement = Arrangement.spacedBy(dimens.space1),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap)) {
                BadgeTipoAto(
                    texto = estado.tribunal.ifBlank { stringResource(R.string.processos_nao_informado) },
                    variante = VarianteBadge.TRIBUNAL,
                )
                BadgeTipoAto(
                    texto = estado.classeProcessual.ifBlank { stringResource(R.string.processos_nao_informado) },
                    variante = VarianteBadge.DESPACHO,
                )
                BadgeTipoAto(
                    texto = estado.status,
                    variante = VarianteBadge.FAVORAVEL,
                )
            }
            Text(
                text = estado.orgaoJulgador.ifBlank { stringResource(R.string.processos_nao_informado) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = estado.partes ?: stringResource(R.string.detalhe_partes_indisponiveis),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun ConteudoTimeline(
    timeline: List<com.obiterjus.domain.model.TimelineProcessoItem>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ObiterTheme.dimens.screenMargin,
            end = ObiterTheme.dimens.screenMargin,
            bottom = ObiterTheme.dimens.screenMargin,
        ),
        verticalArrangement = Arrangement.spacedBy(ObiterTheme.dimens.cardGap),
    ) {
        items(timeline, key = { it.id }) { item ->
            ItemTimeline(
                data = item.dataHora?.let(FormatadorData::formatarDataHora)
                    ?: stringResource(R.string.processos_nao_informado),
                titulo = item.titulo,
                detalhe = item.descricao,
                corPonto = when {
                    item.isSigiloso -> CorPontoTimeline.DANGER
                    item.isImportante -> CorPontoTimeline.PRIMARY
                    item.tipo == com.obiterjus.domain.model.TimelineProcessoTipo.MOVIMENTO_DATAJUD ->
                        CorPontoTimeline.ACCENT
                    else -> CorPontoTimeline.MUTED
                },
                mostrarLinha = true,
            )
        }
        if (timeline.isEmpty()) {
            item {
                EstadoVazioObiter(
                    titulo = stringResource(R.string.detalhe_aba_timeline),
                    corpo = stringResource(R.string.detalhe_empty_timeline),
                    icone = ObiterIcones.Historico,
                    modifier = Modifier.padding(top = ObiterTheme.dimens.cardPaddingV),
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.detalhe_fonte_timeline),
                style = MaterialTheme.typography.bodySmall,
                color = ObiterTheme.colors.textMuted,
                modifier = Modifier.padding(top = ObiterTheme.dimens.sectionGap),
            )
        }
    }
}

@Composable
private fun ConteudoPublicacoes(
    publicacoes: List<com.obiterjus.domain.model.Publicacao>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ObiterTheme.dimens.screenMargin,
            end = ObiterTheme.dimens.screenMargin,
            bottom = ObiterTheme.dimens.screenMargin,
        ),
        verticalArrangement = Arrangement.spacedBy(ObiterTheme.dimens.cardGap),
    ) {
        items(publicacoes, key = { it.id }) { publicacao ->
            CardPublicacao(
                tituloAto = publicacao.tipoComunicacao ?: stringResource(R.string.publicacoes_sem_tipo),
                tipoAto = publicacao.tipoComunicacao ?: stringResource(R.string.publicacoes_sem_tipo),
                data = publicacao.dataDisponibilizacao?.let(FormatadorData::formatarData)
                    ?: stringResource(R.string.publicacoes_sem_data),
                numeroProcesso = publicacao.numeroProcesso
                    ?: stringResource(R.string.publicacoes_sem_numero_processo),
                prazoDias = publicacao.prazo?.quantidade?.let { dias ->
                    stringResource(R.string.prazos_badge_dias, dias)
                },
                trechoTexto = publicacao.textoLimpo,
                prioridade = publicacao.prioridadeStripe(),
                aoClicar = {},
            )
        }
        if (publicacoes.isEmpty()) {
            item {
                EstadoVazioObiter(
                    titulo = stringResource(R.string.detalhe_aba_publicacoes),
                    corpo = stringResource(R.string.detalhe_empty_publicacoes),
                    icone = ObiterIcones.PublicacoesInativo,
                )
            }
        }
    }
}

@Composable
private fun ConteudoPrazos(
    prazos: List<com.obiterjus.domain.model.PrazoAgendaItem>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ObiterTheme.dimens.screenMargin,
            end = ObiterTheme.dimens.screenMargin,
            bottom = ObiterTheme.dimens.screenMargin,
        ),
        verticalArrangement = Arrangement.spacedBy(ObiterTheme.dimens.cardGap),
    ) {
        items(prazos, key = { it.publicacao.id }) { prazo ->
            CardPublicacao(
                tituloAto = prazo.prazo.dataLimiteEstimada?.let { data ->
                    stringResource(R.string.prazos_data_vencimento, FormatadorData.formatarData(data))
                } ?: stringResource(R.string.agenda_item_sem_data),
                tipoAto = prazo.publicacao.tipoComunicacao ?: stringResource(R.string.publicacoes_sem_tipo),
                data = prazo.publicacao.dataDisponibilizacao?.let(FormatadorData::formatarData)
                    ?: stringResource(R.string.publicacoes_sem_data),
                numeroProcesso = prazo.publicacao.numeroProcesso
                    ?: stringResource(R.string.prazos_sem_processo),
                prazoDias = prazo.diasBadge(),
                trechoTexto = prazo.prazo.textoOriginal,
                prioridade = prioridadePrazo(prazo),
                aoClicar = {},
            )
        }
        if (prazos.isEmpty()) {
            item {
                EstadoVazioObiter(
                    titulo = stringResource(R.string.detalhe_aba_prazos),
                    corpo = stringResource(R.string.detalhe_empty_prazos),
                    icone = ObiterIcones.PrazosInativo,
                )
            }
        }
    }
}

@Composable
private fun ConteudoInformacoes(
    informacoes: InformacoesProcesso?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ObiterTheme.dimens.screenMargin,
            end = ObiterTheme.dimens.screenMargin,
            bottom = ObiterTheme.dimens.screenMargin,
        ),
        verticalArrangement = Arrangement.spacedBy(ObiterTheme.dimens.cardGap),
    ) {
        item {
            Text(
                text = stringResource(R.string.detalhe_info_numero),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = informacoes?.numeroProcesso.orEmpty())
            Text(
                text = stringResource(R.string.detalhe_info_classe),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = informacoes?.classeProcessual.orEmpty())
            Text(
                text = stringResource(R.string.detalhe_info_grau),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = informacoes?.grau.orEmpty())
            Text(
                text = stringResource(R.string.detalhe_info_status),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = informacoes?.status.orEmpty())
            Text(
                text = stringResource(R.string.detalhe_info_partes),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = informacoes?.partes?.joinToString(" · ").orEmpty())
            Text(
                text = stringResource(R.string.detalhe_info_fonte),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = informacoes?.fonte.orEmpty())
            Text(
                text = stringResource(R.string.detalhe_info_atualizacao),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = informacoes?.ultimaAtualizacao?.let(FormatadorData::formatarDataHora).orEmpty())
        }
    }
}

@Composable
private fun AbaDetalhe.rotulo(): String =
    when (this) {
        AbaDetalhe.TIMELINE -> stringResource(R.string.detalhe_aba_timeline)
        AbaDetalhe.PUBLICACOES -> stringResource(R.string.detalhe_aba_publicacoes)
        AbaDetalhe.PRAZOS -> stringResource(R.string.detalhe_aba_prazos)
        AbaDetalhe.INFORMACOES -> stringResource(R.string.detalhe_aba_informacoes)
    }

private fun String.truncarNumero(): String =
    if (length > 14) take(14) + "..." else this

private fun com.obiterjus.domain.model.Publicacao.prioridadeStripe(): PrioridadeStripe =
    when {
        isSigiloso -> PrioridadeStripe.CRITICA
        tipoComunicacao?.contains("sent", ignoreCase = true) == true -> PrioridadeStripe.SENTENCA
        tipoComunicacao?.contains("decis", ignoreCase = true) == true -> PrioridadeStripe.DECISAO
        else -> PrioridadeStripe.ROTINEIRO
    }

@Composable
private fun com.obiterjus.domain.model.PrazoAgendaItem.diasBadge(): String =
    prazo.dataLimiteEstimada?.let { data ->
        val dias = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), data).toInt()
        when {
            dias < 0 -> stringResource(R.string.prazos_badge_expirado)
            dias == 0 -> stringResource(R.string.prazos_badge_hoje)
            dias == 1 -> stringResource(R.string.prazos_badge_amanha)
            else -> stringResource(R.string.prazos_badge_dias, dias)
        }
    } ?: stringResource(R.string.prazos_badge_sem_data)

private fun prioridadePrazo(prazo: com.obiterjus.domain.model.PrazoAgendaItem): PrioridadeStripe =
    when {
        prazo.prazo.dataLimiteEstimada == null -> PrioridadeStripe.ROTINEIRO
        prazo.prazo.dataLimiteEstimada.isBefore(java.time.LocalDate.now()) -> PrioridadeStripe.ROTINEIRO
        else -> PrioridadeStripe.FAVORAVEL
    }
