package com.obiterjus.presentation.processos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.obiterjus.R
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.ProcessoSyncStatus
import com.obiterjus.domain.model.TimelineProcessoItem
import com.obiterjus.domain.model.TimelineProcessoTipo
import com.obiterjus.presentation.componentes.CabecalhoDetalhe
import com.obiterjus.presentation.componentes.CabecalhoListagem
import com.obiterjus.presentation.componentes.CampoDetalheListagem
import com.obiterjus.presentation.componentes.CartaoDetalhe
import com.obiterjus.presentation.componentes.CartaoFiltro
import com.obiterjus.presentation.componentes.CartaoItemListagem
import com.obiterjus.presentation.componentes.ChipInformativoListagem
import com.obiterjus.presentation.componentes.ConteudoRolavelAba
import com.obiterjus.presentation.componentes.EstadoVazioListagem
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun TelaProcessos(
    estado: EstadoProcessos,
    aoAlterarFiltroTexto: (String) -> Unit,
    aoAlterarFiltroParticipante: (String) -> Unit,
    aoAlterarFiltroSyncStatus: (String) -> Unit,
    aoAlterarOrdenacao: (OrdenacaoProcessos) -> Unit,
    aoLimparFiltros: () -> Unit,
    aoSelecionarProcesso: (String) -> Unit,
    aoFecharDetalhe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens

    ConteudoRolavelAba(modifier = modifier) {
        CabecalhoListagem(
            titulo = stringResource(R.string.processos_title),
            subtitulo = stringResource(
                R.string.processos_subtitle,
                estado.processos.size,
                estado.totalPersistidos,
            ),
        )

        CartaoFiltro {
            OutlinedTextField(
                value = estado.filtros.texto,
                onValueChange = aoAlterarFiltroTexto,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.processos_label_filtro)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
            )

            OutlinedTextField(
                value = estado.filtros.participante,
                onValueChange = aoAlterarFiltroParticipante,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.processos_filtro_participante)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            )

            OutlinedTextField(
                value = estado.filtros.syncStatus,
                onValueChange = aoAlterarFiltroSyncStatus,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.processos_filtro_status)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
            )

            Text(
                text = stringResource(R.string.processos_ordenacao_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.space2),
                verticalArrangement = Arrangement.spacedBy(dimens.space2),
            ) {
                OrdenacaoProcessos.entries.forEach { ordem ->
                    FilterChip(
                        selected = estado.filtros.ordenacao == ordem,
                        onClick = { aoAlterarOrdenacao(ordem) },
                        label = { Text(stringResource(ordem.rotuloResId())) },
                    )
                }
                OutlinedButton(
                    onClick = aoLimparFiltros,
                    enabled = estado.filtros.possuiFiltrosAtivos,
                ) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                    Text(stringResource(R.string.processos_filtro_limpar))
                }
            }
        }

        estado.processoSelecionado?.let { processo ->
            DetalheProcesso(
                processo = processo,
                timeline = estado.timelineSelecionada,
                aoFechar = aoFecharDetalhe,
            )
        }

        if (estado.processos.isEmpty()) {
            EstadoVazioListagem(
                titulo = stringResource(R.string.processos_empty_title),
                corpo = stringResource(R.string.processos_empty_body),
            )
        } else {
            estado.processos.forEach { processo ->
                ItemProcesso(
                    processo = processo,
                    aoSelecionar = aoSelecionarProcesso,
                )
            }
        }
    }
}

@Composable
private fun ItemProcesso(
    processo: ProcessoMonitorado,
    aoSelecionar: (String) -> Unit,
) {
    val dimens = ObiterTheme.dimens

    CartaoItemListagem(
        onClick = { aoSelecionar(processo.numeroProcesso) },
    ) {
            Text(
                text = processo.numeroProcesso,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.space2),
                verticalArrangement = Arrangement.spacedBy(dimens.space2),
            ) {
                ChipInformativoListagem(processo.tribunal ?: stringResource(R.string.processos_nao_informado))
                ChipInformativoListagem(stringResource(processo.syncStatus.rotuloResId()))
            }

            processo.classeNome?.let { classe ->
                Text(
                    text = classe,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            processo.assuntos.firstOrNull()?.let { assunto ->
                Text(
                    text = assunto,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(
                    R.string.processos_atualizado_em,
                    FormatadorData.formatarDataHora(processo.atualizadoEm),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
private fun DetalheProcesso(
    processo: ProcessoMonitorado,
    timeline: List<TimelineProcessoItem>,
    aoFechar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens

    CartaoDetalhe {
        CabecalhoDetalhe(
            titulo = stringResource(R.string.processos_detalhe_title),
            subtitulo = processo.numeroProcesso,
            fecharDescricao = stringResource(R.string.processos_detalhe_fechar),
            aoFechar = aoFechar,
        )

            CampoDetalheListagem(
                rotulo = stringResource(R.string.processos_detalhe_status),
                valor = stringResource(processo.syncStatus.rotuloResId()),
            )
            CampoDetalheListagem(
                rotulo = stringResource(R.string.processos_detalhe_tribunal),
                valor = processo.tribunal ?: stringResource(R.string.processos_nao_informado),
            )
            CampoDetalheListagem(
                rotulo = stringResource(R.string.processos_detalhe_grau),
                valor = processo.grau ?: stringResource(R.string.processos_nao_informado),
            )
            CampoDetalheListagem(
                rotulo = stringResource(R.string.processos_detalhe_classe),
                valor = processo.classeNome ?: stringResource(R.string.processos_nao_informado),
            )
            CampoDetalheListagem(
                rotulo = stringResource(R.string.processos_detalhe_assuntos),
                valor = processo.assuntos.takeIf { it.isNotEmpty() }?.joinToString("\n")
                    ?: stringResource(R.string.processos_nao_informado),
            )
            CampoDetalheListagem(
                rotulo = stringResource(R.string.processos_detalhe_orgao),
                valor = processo.orgaoJulgadorNome ?: stringResource(R.string.processos_nao_informado),
            )
            CampoDetalheListagem(
                rotulo = stringResource(R.string.processos_detalhe_ajuizamento),
                valor = processo.dataAjuizamento?.let(FormatadorData::formatarDataHora)
                    ?: stringResource(R.string.processos_nao_informado),
            )

            if (processo.participantes.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.processos_detalhe_participantes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                processo.participantes.forEach { participante ->
                    CampoDetalheListagem(
                        rotulo = participante.polo ?: stringResource(R.string.processos_nao_informado),
                        valor = "${participante.nome} (${participante.tipoParticipacao ?: "Parte"})"
                    )
                }
            }

            Text(
                text = stringResource(R.string.processos_timeline_title, timeline.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (timeline.isEmpty()) {
                Text(
                    text = stringResource(R.string.processos_timeline_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                timeline.forEach { item ->
                    ItemTimeline(item = item)
                }
            }
    }
}

@Composable
private fun ItemTimeline(
    item: TimelineProcessoItem,
) {
    val dimens = ObiterTheme.dimens

    CartaoItemListagem(
        isHighlighted = item.isImportante,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimens.space1),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.space2),
                verticalArrangement = Arrangement.spacedBy(dimens.space2),
            ) {
                ChipInformativoListagem(
                    texto = stringResource(item.tipo.rotuloResId()),
                    icone = Icons.Default.Source,
                )
                ChipInformativoListagem(item.fonte)
                if (item.isSigiloso) {
                    ChipInformativoListagem(stringResource(R.string.processos_timeline_sigiloso))
                }
            }
            Text(
                text = item.titulo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.dataHora?.let(FormatadorData::formatarDataHora)
                    ?: stringResource(R.string.processos_nao_informado),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.descricao?.takeIf { it.isNotBlank() }?.let { descricao ->
                Text(
                    text = descricao,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.isSigiloso) {
                Text(
                    text = stringResource(R.string.processos_timeline_texto_sigiloso),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun TimelineProcessoTipo.rotuloResId(): Int =
    when (this) {
        TimelineProcessoTipo.PUBLICACAO_DJEN -> R.string.processos_timeline_tipo_publicacao
        TimelineProcessoTipo.MOVIMENTO_DATAJUD -> R.string.processos_timeline_tipo_movimento
    }

private fun ProcessoSyncStatus.rotuloResId(): Int =
    when (this) {
        ProcessoSyncStatus.PENDING -> R.string.processos_status_pending
        ProcessoSyncStatus.SYNCED -> R.string.processos_status_synced
        ProcessoSyncStatus.NOT_FOUND -> R.string.processos_status_not_found
        ProcessoSyncStatus.FAILED -> R.string.processos_status_failed
        ProcessoSyncStatus.STALE -> R.string.processos_status_stale
    }

private fun OrdenacaoProcessos.rotuloResId(): Int =
    when (this) {
        OrdenacaoProcessos.MAIS_RECENTES -> R.string.processos_ordem_recentes
        OrdenacaoProcessos.MAIS_ANTIGOS -> R.string.processos_ordem_antigos
        OrdenacaoProcessos.TRIBUNAL -> R.string.processos_ordem_tribunal
        OrdenacaoProcessos.NUMERO -> R.string.processos_ordem_numero
    }
