package com.obiterjus.presentation.publicacoes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.domain.logic.NormalizadorPublicacoes
import com.obiterjus.domain.model.ConfiancaMatch
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.TipoAto
import com.obiterjus.domain.model.tipoAto
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraBusca
import com.obiterjus.presentation.componentes.cards.CardPublicacaoResumo
import com.obiterjus.presentation.componentes.chips.ChipFiltroRow
import com.obiterjus.presentation.componentes.filtros.DropdownTribunal
import com.obiterjus.ui.theme.ObiterTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

private const val LOTE_PAGINACAO = 20

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PublicacoesScreen(
    estado: EstadoPublicacoes,
    aoAlterarFiltroTexto: (String) -> Unit,
    aoAlterarFiltroTribunal: (String) -> Unit,
    aoAlterarFiltroTipo: (String) -> Unit,
    aoAlterarFiltroTipoAto: (TipoAto?) -> Unit,
    aoAlterarFiltroDataInicio: (String) -> Unit,
    aoAlterarFiltroDataFim: (String) -> Unit,
    aoAlternarSomenteSigilosas: () -> Unit,
    aoAlterarFiltroConfianca: (ConfiancaMatch?) -> Unit,
    aoAlternarFiltroDuplicatas: () -> Unit,
    aoAbrirDuplicatas: (Long) -> Unit,
    aoFecharDuplicatas: () -> Unit,
    aoLimparFiltros: () -> Unit,
    aoSelecionarPublicacao: (Long) -> Unit,
    aoAbrirPublicacao: (Long) -> Unit,
    aoFecharDetalhe: () -> Unit,
    aoAbrirCertidao: (Publicacao) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val listState = rememberLazyListState()
    var limiteVisivel by remember { mutableIntStateOf(LOTE_PAGINACAO) }
    val publicacoesVisiveis = estado.publicacoes.take(limiteVisivel)
    val agrupadasVisiveis = NormalizadorPublicacoes.agruparPorData(publicacoesVisiveis)
    val tribunalTodos = stringResource(R.string.filtro_todos)
    val tiposAto = TipoAto.entries.map { tipo -> tipo to stringResource(tipo.rotuloRes) }

    LaunchedEffect(estado.publicacoes.size) {
        limiteVisivel = LOTE_PAGINACAO
    }
    LaunchedEffect(listState, estado.publicacoes.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { indice ->
                indice >= publicacoesVisiveis.lastIndex - 4
            }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (limiteVisivel < estado.publicacoes.size) {
                    limiteVisivel = (limiteVisivel + LOTE_PAGINACAO).coerceAtMost(estado.publicacoes.size)
                }
            }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = dimens.screenMargin),
        verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
    ) {
        item {
            BarraBusca(
                consulta = estado.filtros.texto,
                aoMudarConsulta = aoAlterarFiltroTexto,
                placeholder = stringResource(R.string.busca_publicacoes_placeholder),
            )
        }
        // Fase 5: Filtros por data via chips e gênero + tribunal
        item {
            // Chips de data: Hoje, Últimos 7 dias, Último mês
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val hoje = java.time.LocalDate.now()
            val hojeStr = hoje.format(formatter)
            val seteStr = hoje.minusDays(7).format(formatter)
            val trintaStr = hoje.minusDays(30).format(formatter)
            val dataInicioAtual = estado.filtros.dataInicio
            val dataFimAtual = estado.filtros.dataFim
            val hojeAtivo = dataInicioAtual == hojeStr && dataFimAtual == hojeStr
            val seteAtivo = dataInicioAtual == seteStr && dataFimAtual == hojeStr
            val trintaAtivo = dataInicioAtual == trintaStr && dataFimAtual == hojeStr
            // Context for date picker (captured from Composable context to avoid non-Composable callbacks)
            val datePickerContext = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.cardGap),
            ) {
                DateChip(label = stringResource(R.string.publicacoes_filtro_hoje), active = hojeAtivo) {
                    aoAlterarFiltroDataInicio(hojeStr)
                    aoAlterarFiltroDataFim(hojeStr)
                }
                DateChip(label = stringResource(R.string.publicacoes_filtro_ultimos_7_dias), active = seteAtivo) {
                    aoAlterarFiltroDataInicio(seteStr)
                    aoAlterarFiltroDataFim(hojeStr)
                }
                DateChip(label = stringResource(R.string.publicacoes_filtro_ultimo_mes), active = trintaAtivo) {
                    aoAlterarFiltroDataInicio(trintaStr)
                    aoAlterarFiltroDataFim(hojeStr)
                }
                // Calendário para período personalizado
                IconButton(
                    onClick = {
                        // Abre date picker range
                        val ctx = datePickerContext
                        val fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        val hojeLocal = java.time.LocalDate.now()
                        val startDialog = android.app.DatePickerDialog(ctx, { _, y, m, d ->
                            val start = java.time.LocalDate.of(y, m + 1, d)
                            val endDialog = android.app.DatePickerDialog(ctx, { _, y2, m2, d2 ->
                                val end = java.time.LocalDate.of(y2, m2 + 1, d2)
                                aoAlterarFiltroDataInicio(start.format(fmt))
                                aoAlterarFiltroDataFim(end.format(fmt))
                            }, hojeLocal.year, hojeLocal.monthValue - 1, hojeLocal.dayOfMonth)
                            endDialog.show()
                        }, hojeLocal.year, hojeLocal.monthValue - 1, hojeLocal.dayOfMonth)
                        startDialog.show()
                    }
                ) {
                    Icon(
                        imageVector = ObiterIcones.Evento,
                        contentDescription = stringResource(R.string.publicacoes_filtro_periodo_personalizado),
                    )
                }
            }
        }
        item {
            DropdownTribunal(
                tribunalSelecionado = estado.filtros.tribunal,
                tribunaisPorGenero = estado.tribunaisPorGenero,
                aoSelecionarTribunal = aoAlterarFiltroTribunal,
                modifier = Modifier.padding(horizontal = dimens.screenMargin),
            )
        }
        item {
            ChipFiltroRow(
                chips = listOf(tribunalTodos) + tiposAto.map { (_, rotulo) -> rotulo },
                chipAtivo = tiposAto.firstOrNull { (tipo, _) -> tipo == estado.filtros.tipoAto }?.second
                    ?: tribunalTodos,
                aoSelecionar = { chip ->
                    val tipo = tiposAto.firstOrNull { (_, rotulo) -> rotulo == chip }?.first
                    aoAlterarFiltroTipoAto(tipo)
                },
            )
        }
        item {
            val rotuloTodas = stringResource(R.string.confianca_filtro_todas)
            val rotuloDuplicatas = stringResource(R.string.publicacoes_filtro_duplicatas)
            val rotulosConfianca = listOf(
                ConfiancaMatch.ALTA to stringResource(R.string.confianca_alta),
                ConfiancaMatch.MEDIA to stringResource(R.string.confianca_media),
                ConfiancaMatch.BAIXA to stringResource(R.string.confianca_baixa),
            )
            val chipAtivo = when {
                estado.filtros.mostrarApenasDuplicatas -> rotuloDuplicatas
                estado.filtros.confiancaSelecionada != null ->
                    rotulosConfianca.first { (nivel, _) -> nivel == estado.filtros.confiancaSelecionada }.second
                else -> rotuloTodas
            }
            ChipFiltroRow(
                chips = listOf(rotuloTodas) +
                    rotulosConfianca.map { (_, rotulo) -> rotulo } +
                    listOf(rotuloDuplicatas),
                chipAtivo = chipAtivo,
                aoSelecionar = { chip ->
                    when (chip) {
                        rotuloDuplicatas -> aoAlternarFiltroDuplicatas()
                        else -> {
                            val nivel = rotulosConfianca
                                .firstOrNull { (_, rotulo) -> rotulo == chip }?.first
                            aoAlterarFiltroConfianca(nivel)
                        }
                    }
                },
            )
        }

        if (publicacoesVisiveis.isEmpty()) {
            item {
                EstadoVazioObiter(
                    titulo = stringResource(R.string.publicacoes_empty_title),
                    corpo = stringResource(R.string.publicacoes_empty_body),
                    icone = ObiterIcones.PublicacoesInativo,
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                )
            }
        } else {
            agrupadasVisiveis.forEach { (data, publicacoesNoDia) ->
                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 8.dp, horizontal = dimens.screenMargin)
                    ) {
                        Text(
                            text = FormatadorData.formatarData(data).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(
                    items = publicacoesNoDia,
                    key = { it.id },
                ) { pubItem ->
                    val pubId = pubItem.id
                    val metadados = estado.metadadosOrdem[pubId]
                    val badgeOrdem = metadados?.first?.let { ordem ->
                        stringResource(R.string.publicacoes_badge_ordem, ordem)
                    }
                    val badgeDuplicatas = if (pubItem.possuiDuplicatas) {
                        stringResource(R.string.publicacoes_badge_duplicatas, pubItem.totalDuplicatas)
                    } else {
                        null
                    }
                    CardPublicacaoResumo(
                        publicacao = pubItem,
                        onClick = {
                            aoSelecionarPublicacao(pubId)
                            aoAbrirPublicacao(pubId)
                        },
                        modifier = Modifier.padding(horizontal = dimens.screenMargin),
                        badgeOrdem = badgeOrdem,
                        badgeDuplicatas = badgeDuplicatas,
                        onBadgeDuplicatasClick = if (pubItem.possuiDuplicatas) {
                            { aoAbrirDuplicatas(pubId) }
                        } else {
                            null
                        },
                        onVerDetalhes = {
                            aoSelecionarPublicacao(pubId)
                            aoAbrirPublicacao(pubId)
                        },
                    )
                }
            }
        }

        if (limiteVisivel < estado.publicacoes.size) {
            item {
                Text(
                    text = stringResource(R.string.lista_carregando_mais),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObiterTheme.colors.textMuted,
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                )
            }
        }
    }

    val canonica = estado.canonicaDuplicatas
    if (canonica != null) {
        DuplicatasBottomSheet(
            canonica = canonica,
            duplicatas = estado.duplicatasSelecionadas,
            aoFechar = aoFecharDuplicatas,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuplicatasBottomSheet(
    canonica: Publicacao,
    duplicatas: List<Publicacao>,
    aoFechar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenMargin),
            contentPadding = PaddingValues(bottom = dimens.screenMargin),
            verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
        ) {
            item {
                Text(
                    text = stringResource(
                        R.string.publicacoes_duplicatas_titulo,
                        duplicatas.size,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.publicacoes_duplicatas_subtitulo),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.publicacoes_duplicatas_canonica),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                DuplicataLinha(publicacao = canonica)
            }
            item {
                Text(
                    text = stringResource(R.string.publicacoes_duplicatas_copias),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(items = duplicatas, key = { it.id }) { copia ->
                DuplicataLinha(publicacao = copia)
            }
        }
    }
}

@Composable
private fun DuplicataLinha(publicacao: Publicacao) {
    val colors = ObiterTheme.colors
    val capturadoEm = remember(publicacao.capturadoEm) {
        runCatching {
            java.time.format.DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(publicacao.capturadoEm)
        }.getOrDefault("")
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.publicacoes_duplicatas_linha_id, publicacao.id),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                R.string.publicacoes_duplicatas_linha_recebida,
                capturadoEm,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
        )
        publicacao.hash?.takeIf { it.isNotBlank() }?.let { hash ->
            Text(
                text = stringResource(R.string.publicacoes_duplicatas_linha_hash, hash),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun DateChip(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = ObiterTheme.colors
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .padding(end = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MaterialTheme.colorScheme.primary else colors.textMuted
        )
    }
}

// Prioridade calculada via Publicacao.prioridadeStripe() a partir da extensão pública

@Composable
fun ConteudoPublicacoes(
    viewModel: PublicacoesViewModel,
    aoAbrirPublicacao: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    PublicacoesScreen(
        estado = estado,
        aoAlterarFiltroTexto = viewModel::aoAlterarFiltroTexto,
        aoAlterarFiltroTribunal = viewModel::aoAlterarFiltroTribunal,
        aoAlterarFiltroTipo = viewModel::aoAlterarFiltroTipo,
        aoAlterarFiltroTipoAto = viewModel::aoAlterarFiltroTipoAto,
        aoAlterarFiltroDataInicio = viewModel::aoAlterarFiltroDataInicio,
        aoAlterarFiltroDataFim = viewModel::aoAlterarFiltroDataFim,
        aoAlternarSomenteSigilosas = viewModel::aoAlternarSomenteSigilosas,
        aoAlterarFiltroConfianca = viewModel::aoAlterarFiltroConfianca,
        aoAlternarFiltroDuplicatas = viewModel::aoAlternarFiltroDuplicatas,
        aoAbrirDuplicatas = viewModel::aoAbrirDuplicatas,
        aoFecharDuplicatas = viewModel::aoFecharDuplicatas,
        aoLimparFiltros = viewModel::aoLimparFiltros,
        aoSelecionarPublicacao = viewModel::aoSelecionarPublicacao,
        aoAbrirPublicacao = aoAbrirPublicacao,
        aoFecharDetalhe = viewModel::aoFecharDetalhe,
        aoAbrirCertidao = viewModel::aoAbrirCertidao,
        modifier = modifier,
    )
}
