package com.obiterjus.presentation.processos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraBusca
import com.obiterjus.presentation.componentes.cards.CardProcesso
import com.obiterjus.presentation.componentes.chips.ChipFiltroRow
import com.obiterjus.presentation.componentes.secoes.CabecalhoComarca
import com.obiterjus.presentation.componentes.secoes.CabecalhoTribunal
import com.obiterjus.presentation.componentes.filtros.DropdownTribunal
import com.obiterjus.ui.theme.ObiterTheme
import com.obiterjus.presentation.participantes.resolverPartesProcesso
import com.obiterjus.presentation.participantes.formatarConfronto

@Composable
fun ProcessosScreen(
    estado: EstadoProcessos,
    aoAlterarFiltroTexto: (String) -> Unit,
    aoAlterarFiltroParticipante: (String) -> Unit,
    aoAlterarFiltroSyncStatus: (String) -> Unit,
    aoAlterarStatus: (FiltroStatusProcesso) -> Unit,
    aoLimparFiltros: () -> Unit,
    aoSelecionarProcesso: (String) -> Unit,
    aoAlternarTribunal: (String) -> Unit,
    aoAlterarFiltroTribunal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val statusTodos = stringResource(R.string.filtro_todos)
    val statusAtivos = stringResource(R.string.filtro_ativos)
    val statusArquivados = stringResource(R.string.filtro_arquivados)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = dimens.screenMargin),
        verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
    ) {
        item {
            BarraBusca(
                consulta = estado.filtros.texto,
                aoMudarConsulta = aoAlterarFiltroTexto,
                placeholder = stringResource(R.string.busca_processos_placeholder),
            )
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
                chips = listOf(
                    statusTodos,
                    statusAtivos,
                    statusArquivados,
                ),
                chipAtivo = stringResource(estado.filtros.status.rotuloResId()),
                aoSelecionar = { chip ->
                    aoAlterarStatus(
                        when (chip) {
                            statusAtivos -> FiltroStatusProcesso.ATIVOS
                            statusArquivados -> FiltroStatusProcesso.ARQUIVADOS
                            else -> FiltroStatusProcesso.GERAL
                        },
                    )
                },
            )
        }

        if (estado.gruposTribunais.isEmpty()) {
            item {
                EstadoVazioObiter(
                    titulo = stringResource(R.string.processos_empty_title),
                    corpo = stringResource(R.string.processos_empty_body),
                    icone = ObiterIcones.ProcessosInativo,
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                )
            }
        } else {
            items(
                items = estado.gruposTribunais,
                key = { it.tribunal },
            ) { grupo ->
                CabecalhoTribunal(
                    siglaTribunal = grupo.tribunal.ifBlank { stringResource(R.string.processos_nao_informado) },
                    contagem = grupo.quantidade,
                    expandido = grupo.expandido,
                    corFundoBadge = grupo.tribunal.corTribunal(ObiterTheme.colors),
                    aoAlternar = { aoAlternarTribunal(grupo.tribunal) },
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                )
                AnimatedVisibility(
                    visible = grupo.expandido,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(dimens.cardGap)) {
                        grupo.comarcas.forEach { comarca ->
                            CabecalhoComarca(
                                nomeComarca = comarca.comarca.ifBlank { stringResource(R.string.processos_nao_informado) },
                                modifier = Modifier.padding(horizontal = dimens.screenMargin),
                            )
                            comarca.processos.forEach { processo ->
                                val partesResolvidas = processo.participantes.resolverPartesProcesso()
                                CardProcesso(
                                    numeroProcesso = processo.numeroProcesso,
                                    partes = partesResolvidas.formatarConfronto(),
                                    cliente = partesResolvidas.clientes.joinToString(" · ").ifBlank { null },
                                    badges = processo.badges(),
                                    temPrazoAtivo = processo.syncStatus == ProcessoSyncStatus.PENDING,
                                    ultimaMovimentacao = processo.atualizadoEm.let(FormatadorData::formatarDataHora),
                                    fonte = processo.tribunal ?: stringResource(R.string.processos_nao_informado),
                                    aoClicar = { aoSelecionarProcesso(processo.numeroProcesso) },
                                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun FiltroStatusProcesso.rotuloResId(): Int =
    when (this) {
        FiltroStatusProcesso.GERAL -> R.string.filtro_todos
        FiltroStatusProcesso.ATIVOS -> R.string.filtro_ativos
        FiltroStatusProcesso.ARQUIVADOS -> R.string.filtro_arquivados
    }

@Composable
private fun String.corTribunal(colors: com.obiterjus.ui.theme.ObiterExtendedColors): Color = when {
    contains("TJMG", ignoreCase = true) -> MaterialTheme.colorScheme.primary
    contains("TRT", ignoreCase = true) -> colors.mulledWine
    contains("TRF", ignoreCase = true) -> colors.primaryDark
    else -> MaterialTheme.colorScheme.primary
}

private fun ProcessoMonitorado.badges(): List<String> =
    listOfNotNull(
        grau,
        classeNome,
        orgaoJulgadorNome,
    )

@Composable
fun ConteudoProcessos(
    viewModel: ProcessosViewModel,
    aoAbrirDetalhe: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ProcessosScreen(
        estado = estado,
        aoAlterarFiltroTexto = viewModel::aoAlterarFiltroTexto,
        aoAlterarFiltroParticipante = viewModel::aoAlterarFiltroParticipante,
        aoAlterarFiltroSyncStatus = viewModel::aoAlterarFiltroSyncStatus,
        aoAlterarStatus = viewModel::aoAlterarStatus,
        aoLimparFiltros = viewModel::aoLimparFiltros,
        aoSelecionarProcesso = aoAbrirDetalhe,
        aoAlternarTribunal = viewModel::aoAlternarTribunal,
        aoAlterarFiltroTribunal = viewModel::aoAlterarFiltroTribunal,
        modifier = modifier,
    )
}
