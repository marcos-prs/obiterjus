package com.obiterjus.presentation.detalheprocesso

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.core.texto.formatarCnj
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.cards.CardPublicacao
import com.obiterjus.presentation.componentes.cards.PrioridadeStripe
import com.obiterjus.presentation.componentes.chips.BadgeTipoAto
import com.obiterjus.presentation.componentes.chips.VarianteBadge
import com.obiterjus.presentation.componentes.timeline.ItemTimeline
import com.obiterjus.ui.theme.ObiterTheme
import com.obiterjus.ui.theme.Tiber

@Composable
fun TelaDetalheProcesso(
    viewModel: ModeloDetalheProcesso,
    numeroProcesso: String,
    onVoltar: () -> Unit,
    aoEditarProcesso: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(numeroProcesso) {
        viewModel.aoCarregar(numeroProcesso)
    }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val isDark = isSystemInDarkTheme()
    val barraCor = if (isDark) MaterialTheme.colorScheme.surface else Tiber
    val textoCor = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = barraCor,
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
                            tint = textoCor,
                        )
                    }
                    Text(
                        text = estado.numeroProcesso.formatarCnj(),
                        style = MaterialTheme.typography.titleMedium,
                        color = textoCor,
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.material3.IconButton(onClick = aoEditarProcesso) {
                        androidx.compose.material3.Icon(
                            imageVector = ObiterIcones.Editar,
                            contentDescription = stringResource(R.string.cd_editar_processo),
                            tint = textoCor.copy(alpha = 0.70f),
                        )
                    }
                }
                Text(
                    text = listOf(estado.tribunal, estado.orgaoJulgador)
                        .filter(String::isNotBlank)
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = textoCor.copy(alpha = 0.5f),
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

            SecondaryScrollableTabRow(
                selectedTabIndex = estado.abaSelecionada,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(dimens.cardRadius)),
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 0.dp,
                divider = {
                    HorizontalDivider(
                        color = colors.divider,
                        thickness = dimens.borderWidth,
                    )
                },
                indicator = {
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(estado.abaSelecionada),
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
                ultimaAtualizacao = estado.ultimaAtualizacao,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.cardPaddingH),
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
        if (estado.poloAtivo.isNotEmpty()) {
            Text(
                text = stringResource(R.string.detalhe_info_polo_ativo) + ": " + estado.poloAtivo.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
        if (estado.poloPassivo.isNotEmpty()) {
            Text(
                text = stringResource(R.string.detalhe_info_polo_passivo) + ": " + estado.poloPassivo.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
        if (estado.poloAtivo.isEmpty() && estado.poloPassivo.isEmpty()) {
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
    ultimaAtualizacao: java.time.Instant?,
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
                data = FormatadorData.formatarDataPorExtenso(item.dataHora),
                titulo = item.titulo,
                detalhe = item.descricao,
                corPonto = item.corPonto,
                mostrarLinha = item != timeline.last(),
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
            Surface(
                color = ObiterTheme.colors.surfacePergaminho.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(top = ObiterTheme.dimens.sectionGap),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Fonte: DataJud (CNJ) · Sync: ${FormatadorData.formatarDataHora(ultimaAtualizacao)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObiterTheme.colors.textMuted,
                    modifier = Modifier.padding(8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
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
                tribunal = publicacao.tribunal ?: stringResource(R.string.publicacoes_sem_tribunal),
                juizo = publicacao.nomeOrgao,
                numeroProcesso = publicacao.numeroProcesso
                    ?.formatarCnj() ?: stringResource(R.string.publicacoes_sem_numero_processo),
                prazoDias = publicacao.prazo?.quantidade?.let { dias ->
                    stringResource(R.string.prazos_badge_dias, dias)
                },
                trechoTexto = publicacao.textoLimpo,
                prioridade = publicacao.prioridadeStripe(),
                aoClicar = {},
                onVerDetalhes = {},
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
                tribunal = prazo.publicacao.tribunal ?: stringResource(R.string.publicacoes_sem_tribunal),
                juizo = prazo.publicacao.nomeOrgao,
                numeroProcesso = prazo.publicacao.numeroProcesso
                    ?.formatarCnj() ?: stringResource(R.string.prazos_sem_processo),
                prazoDias = prazo.diasBadge(),
                trechoTexto = prazo.prazo.textoOriginal,
                prioridade = prioridadePrazo(prazo),
                aoClicar = {},
                onVerDetalhes = {},
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
            Text(text = informacoes?.numeroProcesso.orEmpty().formatarCnj())
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
            if (informacoes?.poloAtivo?.isNotEmpty() == true) {
                Text(
                    text = stringResource(R.string.detalhe_info_polo_ativo),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(text = informacoes.poloAtivo.joinToString(" · "))
            }
            if (informacoes?.poloPassivo?.isNotEmpty() == true) {
                Text(
                    text = stringResource(R.string.detalhe_info_polo_passivo),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(text = informacoes.poloPassivo.joinToString(" · "))
            }
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
        AbaDetalhe.INFORMACOES -> stringResource(R.string.detalhe_aba_info)
    }

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
