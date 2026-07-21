package com.obiterjus.presentation.detalheprocesso

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.obiterjus.presentation.componentes.barras.BarraSuperiorSecundaria
import com.obiterjus.presentation.componentes.timeline.ItemTimeline
import com.obiterjus.ui.theme.ObiterTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.Locale

@Composable
fun DetalheProcessoScreen(
    viewModel: DetalheProcessoViewModel,
    numeroProcesso: String,
    onVoltar: () -> Unit,
    aoEditarProcesso: () -> Unit,
    aoExcluirProcesso: () -> Unit,
    aoAbrirPublicacao: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(numeroProcesso) {
        viewModel.aoCarregar(numeroProcesso)
    }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors

    var menuExpandido by remember { mutableStateOf(false) }
    var mostrarConfirmacaoExclusao by remember { mutableStateOf(false) }

    LaunchedEffect(estado.excluido) {
        if (estado.excluido) aoExcluirProcesso()
    }

    if (mostrarConfirmacaoExclusao) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoExclusao = false },
            title = { Text(stringResource(R.string.editar_processo_action_excluir)) },
            text = { Text(stringResource(R.string.excluir_processo_confirmacao, estado.numeroProcesso.formatarCnj())) },
            confirmButton = {
                TextButton(onClick = { mostrarConfirmacaoExclusao = false; viewModel.aoExcluir() }) {
                    Text(stringResource(R.string.excluir_processo_confirmar), color = colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacaoExclusao = false }) {
                    Text(stringResource(R.string.excluir_processo_cancelar))
                }
            },
        )
    }

    if (estado.erroExclusao) {
        AlertDialog(
            onDismissRequest = { viewModel.aoDescartarErroExclusao() },
            title = { Text(stringResource(R.string.editar_processo_action_excluir)) },
            text = { Text(stringResource(R.string.excluir_processo_falha)) },
            confirmButton = {
                TextButton(onClick = { viewModel.aoDescartarErroExclusao() }) {
                    Text(stringResource(R.string.excluir_processo_cancelar))
                }
            },
        )
    }
    val subtituloBarra = listOf(estado.tribunal, estado.orgaoJulgador)
        .filter(String::isNotBlank)
        .joinToString(" · ")
        .takeIf(String::isNotBlank)

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperiorSecundaria(
            titulo = estado.numeroProcesso.formatarCnj(),
            subtitulo = subtituloBarra,
            onVoltar = onVoltar,
            acoes = {
                Box {
                    IconButton(onClick = { menuExpandido = true }) {
                        Icon(
                            imageVector = ObiterIcones.MaisOpcoes,
                            contentDescription = stringResource(R.string.cd_mais_opcoes),
                            tint = colors.onTopAppBar.copy(alpha = 0.70f),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpandido,
                        onDismissRequest = { menuExpandido = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.detalhe_menu_editar)) },
                            onClick = { menuExpandido = false; aoEditarProcesso() },
                            leadingIcon = {
                                Icon(
                                    imageVector = ObiterIcones.Editar,
                                    contentDescription = null,
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.detalhe_menu_excluir),
                                    color = colors.danger,
                                )
                            },
                            onClick = { menuExpandido = false; mostrarConfirmacaoExclusao = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = ObiterIcones.Excluir,
                                    contentDescription = null,
                                    tint = colors.danger,
                                )
                            },
                        )
                    }
                }
            },
        )

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
                aoAbrirPublicacao = aoAbrirPublicacao,
                modifier = Modifier.weight(1f),
            )
            2 -> ConteudoPrazos(
                prazos = estado.prazos,
                aoAbrirPublicacao = aoAbrirPublicacao,
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
    var expandido by remember { mutableStateOf(false) }
    val rotacaoChevron by animateFloatAsState(
        targetValue = if (expandido) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "chevronRotacao",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.cardPaddingH),
        verticalArrangement = Arrangement.spacedBy(dimens.space1),
    ) {
        // ── Conteúdo sempre visível ──
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
        val clientes = estado.clientes.toSet()
        if (estado.poloAtivo.isNotEmpty()) {
            LinhaPolo(
                label = stringResource(R.string.detalhe_info_polo_ativo),
                especie = estado.especieAtiva,
                nomes = estado.poloAtivo,
                clientes = clientes,
            )
        }
        if (estado.poloPassivo.isNotEmpty()) {
            LinhaPolo(
                label = stringResource(R.string.detalhe_info_polo_passivo),
                especie = estado.especiePassiva,
                nomes = estado.poloPassivo,
                clientes = clientes,
            )
        }
        if (estado.advogados.isNotEmpty()) {
            Text(
                text = stringResource(R.string.detalhe_info_advogados) + ": " + estado.advogados.joinToString(" · "),
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

        // ── Toggle: divider + chevron ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandido = !expandido }
                .padding(top = dimens.space1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = colors.divider,
                thickness = dimens.borderWidth,
            )
            Icon(
                imageVector = ObiterIcones.ExpandirAbaixo,
                contentDescription = if (expandido)
                    stringResource(R.string.cd_recolher)
                else
                    stringResource(R.string.cd_expandir),
                tint = colors.textMuted,
                modifier = Modifier
                    .size(dimens.iconStarSize)
                    .rotate(rotacaoChevron),
            )
        }

        // ── Painel expansível com dados completos ──
        AnimatedVisibility(
            visible = expandido,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            val info = estado.informacoes
            Column(verticalArrangement = Arrangement.spacedBy(dimens.space1)) {
                Spacer(Modifier.height(dimens.space1))

                // IDENTIFICAÇÃO
                SecaoDropdown(stringResource(R.string.editar_processo_secao_identificacao), colors.accent)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_data_distribuicao), info?.dataDistribuicao?.formatarSoData())
                LinhaDetalhe(stringResource(R.string.editar_processo_label_assunto), info?.assuntoPrincipal)
                LinhaDetalhe(
                    stringResource(R.string.editar_processo_label_segredo),
                    info?.nivelSigilo?.let { if (it > 0) stringResource(R.string.opcao_sim) else stringResource(R.string.opcao_nao) },
                )

                // JUDICIÁRIO
                SecaoDropdown(stringResource(R.string.editar_processo_secao_judiciario), colors.accent)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_tribunal), info?.tribunal)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_comarca), info?.comarcaSecao)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_juizo), info?.juizo)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_classe), info?.classeProcessual)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_orgao), info?.orgaoJulgador)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_prioridade), info?.prioridadeTramitacao)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_gratuidade), info?.gratuidadeJustica)

                // REPRESENTAÇÃO
                SecaoDropdown(stringResource(R.string.editar_processo_secao_representacao), colors.accent)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_advogados_ativo), info?.advogadosAtivo)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_advogados_passivo), info?.advogadosPassivo)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_defensoria), info?.defensoriaPublica)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_mp), info?.ministerioPublico)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_terceiros), info?.terceirosAuxiliares)

                // FINANCEIRO
                SecaoDropdown(stringResource(R.string.editar_processo_secao_financeiro), colors.accent)
                LinhaDetalhe(
                    stringResource(R.string.editar_processo_label_valor_causa),
                    info?.valorCausa?.let { java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(it) },
                )
                LinhaDetalhe(stringResource(R.string.editar_processo_label_fase), info?.faseProcessual)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_situacao), info?.situacaoAtual)

                // URGÊNCIA
                SecaoDropdown(stringResource(R.string.editar_processo_secao_urgencia), colors.accent)
                LinhaDetalhe(stringResource(R.string.editar_processo_label_tutela), info?.tutelaAntecipadaLiminar)

                Spacer(Modifier.height(dimens.space1))
            }
        }
    }
}

@Composable
private fun LinhaPolo(
    label: String,
    especie: String?,
    nomes: List<String>,
    clientes: Set<String>,
) {
    val colors = ObiterTheme.colors
    val dimens = ObiterTheme.dimens
    val cabecalho = if (especie != null) "$label ($especie):" else "$label:"
    Column {
        Text(
            text = cabecalho,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
        )
        val temCliente = nomes.any { it in clientes }
        nomes.forEach { nome ->
            val ehCliente = nome in clientes
            Row(
                modifier = Modifier.padding(start = dimens.space1),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.space1),
            ) {
                if (temCliente) {
                    if (ehCliente) {
                        Icon(
                            imageVector = ObiterIcones.Cliente,
                            contentDescription = stringResource(R.string.detalhe_chip_cliente),
                            tint = colors.accent,
                            modifier = Modifier.size(dimens.iconStarSize),
                        )
                    } else {
                        Spacer(Modifier.size(dimens.iconStarSize))
                    }
                }
                Text(
                    text = nome,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun SecaoDropdown(titulo: String, corAccent: androidx.compose.ui.graphics.Color) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    Column {
        Spacer(Modifier.height(dimens.space1))
        Text(
            text = titulo.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = corAccent,
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 2.dp),
            color = colors.divider,
        )
    }
}

@Composable
private fun LinhaDetalhe(label: String, valor: String?) {
    if (valor.isNullOrBlank()) return
    val colors = ObiterTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f),
        )
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
    aoAbrirPublicacao: (Long) -> Unit,
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
                aoClicar = { aoAbrirPublicacao(publicacao.id) },
                onVerDetalhes = { aoAbrirPublicacao(publicacao.id) },
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
    aoAbrirPublicacao: (Long) -> Unit,
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
                aoClicar = { aoAbrirPublicacao(prazo.publicacao.id) },
                onVerDetalhes = { aoAbrirPublicacao(prazo.publicacao.id) },
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

private fun java.time.Instant.formatarSoData(): String =
    atZone(java.time.ZoneId.systemDefault()).toLocalDate().let(FormatadorData::formatarData)

private fun prioridadePrazo(prazo: com.obiterjus.domain.model.PrazoAgendaItem): PrioridadeStripe =
    when {
        prazo.prazo.dataLimiteEstimada == null -> PrioridadeStripe.ROTINEIRO
        prazo.prazo.dataLimiteEstimada.isBefore(java.time.LocalDate.now()) -> PrioridadeStripe.ROTINEIRO
        else -> PrioridadeStripe.FAVORAVEL
    }

