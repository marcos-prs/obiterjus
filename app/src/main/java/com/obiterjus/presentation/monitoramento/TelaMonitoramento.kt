package com.obiterjus.presentation.monitoramento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.obiterjus.R
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.domain.model.MonitorarCnjResumo
import com.obiterjus.domain.model.MonitorarDjenStopReason
import com.obiterjus.domain.model.SincronizacaoStatus
import com.obiterjus.presentation.componentes.CabecalhoListagem
import com.obiterjus.presentation.componentes.CartaoFormulario
import com.obiterjus.presentation.componentes.CartaoItemListagem
import com.obiterjus.presentation.componentes.CartaoStatus
import com.obiterjus.presentation.componentes.ConteudoRolavelAba
import com.obiterjus.ui.theme.ObiterTheme

private const val PESO_CAMPO_DATA_INICIO = 1f

@Composable
fun TelaMonitoramento(
    uiState: MonitoramentoUiState,
    onNumeroOabChange: (String) -> Unit,
    onUfOabChange: (String) -> Unit,
    onDataInicioChange: (String) -> Unit,
    onDataFimChange: (String) -> Unit,
    onSyncFrequencyChange: (Int) -> Unit,
    onSincronizarClick: () -> Unit,
    onExportarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        ConteudoRolavelAba {
            CabecalhoListagem(
                titulo = stringResource(R.string.app_name),
                subtitulo = stringResource(R.string.monitoramento_title),
            )

            FormularioMonitoramento(
                uiState = uiState,
                onNumeroOabChange = onNumeroOabChange,
                onUfOabChange = onUfOabChange,
                onDataInicioChange = onDataInicioChange,
                onDataFimChange = onDataFimChange,
                onSyncFrequencyChange = onSyncFrequencyChange,
                onSincronizarClick = onSincronizarClick,
                onExportarClick = onExportarClick,
            )

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.error?.let { error ->
                CartaoStatus(
                    titulo = stringResource(R.string.monitoramento_error_title),
                    mensagem = stringResource(error.messageResId()),
                    isErro = true,
                )
            }

            StatusSincronizacaoCard(status = uiState.syncStatus)

            uiState.lastResumo?.let { resumo ->
                ResumoCard(resumo = resumo)
            }
        }
    }
}

@Composable
private fun StatusSincronizacaoCard(status: SincronizacaoStatus) {
    val ultimaExecucao = status.ultimaExecucaoEm ?: return
    val falha = status.ultimaFalha
    CartaoStatus(
        titulo = stringResource(R.string.monitoramento_status_title),
        mensagem = if (falha == null) {
            stringResource(
                R.string.monitoramento_status_success,
                FormatadorData.formatarDataHora(status.ultimoSucessoEm ?: ultimaExecucao),
                status.novasPublicacoesUltimaExecucao,
            )
        } else {
            stringResource(
                R.string.monitoramento_status_error,
                FormatadorData.formatarDataHora(ultimaExecucao),
                falha,
            )
        },
        isErro = falha != null,
    )
}

@Composable
private fun FormularioMonitoramento(
    uiState: MonitoramentoUiState,
    onNumeroOabChange: (String) -> Unit,
    onUfOabChange: (String) -> Unit,
    onDataInicioChange: (String) -> Unit,
    onDataFimChange: (String) -> Unit,
    onSyncFrequencyChange: (Int) -> Unit,
    onSincronizarClick: () -> Unit,
    onExportarClick: () -> Unit,
) {
    val dimens = ObiterTheme.dimens

    CartaoFormulario {
        OutlinedTextField(
            value = uiState.numeroOab,
            onValueChange = onNumeroOabChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.monitoramento_label_numero_oab)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.space3),
        ) {
            OutlinedTextField(
                value = uiState.ufOab,
                onValueChange = onUfOabChange,
                modifier = Modifier.width(dimens.monitoramentoUfFieldWidth),
                label = { Text(stringResource(R.string.monitoramento_label_uf)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = uiState.dataInicio,
                onValueChange = onDataInicioChange,
                modifier = Modifier.weight(PESO_CAMPO_DATA_INICIO),
                label = { Text(stringResource(R.string.monitoramento_label_data_inicio)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            )
        }

        OutlinedTextField(
            value = uiState.dataFim,
            onValueChange = onDataFimChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.monitoramento_label_data_fim)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.monitoramento_label_frequencia),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.space2)
            ) {
                FilterChip(
                    selected = uiState.syncFrequencyHours == 4,
                    onClick = { onSyncFrequencyChange(4) },
                    label = { Text(stringResource(R.string.monitoramento_freq_4h)) }
                )
                FilterChip(
                    selected = uiState.syncFrequencyHours == 12,
                    onClick = { onSyncFrequencyChange(12) },
                    label = { Text(stringResource(R.string.monitoramento_freq_12h)) }
                )
                FilterChip(
                    selected = uiState.syncFrequencyHours == 24,
                    onClick = { onSyncFrequencyChange(24) },
                    label = { Text(stringResource(R.string.monitoramento_freq_24h)) }
                )
            }
        }

        Button(
            onClick = onSincronizarClick,
            enabled = uiState.canSubmit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimens.radiusSmall),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )
            Text(
                text = stringResource(
                    if (uiState.isLoading) {
                        R.string.monitoramento_action_buscar_loading
                    } else {
                        R.string.monitoramento_action_buscar
                    },
                ),
                modifier = Modifier.padding(start = dimens.space2),
            )
        }

        Button(
            onClick = onExportarClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimens.radiusSmall),
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
            )
            Text(
                text = stringResource(R.string.monitoramento_botao_exportar),
                modifier = Modifier.padding(start = dimens.space2),
            )
        }
    }
}

@Composable
private fun ResumoCard(
    resumo: MonitorarCnjResumo,
) {
    val dimens = ObiterTheme.dimens
    val dataJud = resumo.dataJud
    val dataJudFailures = dataJud?.falhas ?: 0

    CartaoItemListagem {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.monitoramento_resumo_title),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.space2),
            verticalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            Metric(stringResource(R.string.monitoramento_metric_remotas), resumo.djen.totalRemoto?.toString() ?: "-")
            Metric(stringResource(R.string.monitoramento_metric_recebidas), resumo.djen.totalRecebidas.toString())
            Metric(stringResource(R.string.monitoramento_metric_novas), resumo.djen.novas.toString())
            Metric(stringResource(R.string.monitoramento_metric_atualizadas), resumo.djen.atualizadas.toString())
            Metric(stringResource(R.string.monitoramento_metric_sigilosas), resumo.djen.sigilosas.toString())
            Metric(stringResource(R.string.monitoramento_metric_processos), resumo.djen.processosNovos.size.toString())
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(dimens.space1)) {
            Text(
                text = stringResource(
                    R.string.monitoramento_djen_resumo,
                    resumo.djen.paginasConsultadas,
                    djenStopReasonText(resumo.djen.motivoParada),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (dataJud == null) {
                    stringResource(R.string.monitoramento_datajud_sem_processos)
                } else {
                    stringResource(
                        R.string.monitoramento_datajud_resumo,
                        dataJud.encontrados,
                        dataJud.naoEncontrados,
                        dataJudFailures,
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (resumo.djen.falhas.isNotEmpty()) {
            CartaoStatus(
                titulo = stringResource(R.string.monitoramento_djen_falha_title),
                mensagem = stringResource(R.string.monitoramento_djen_falha_message),
                isErro = true,
            )
        }

        val firstFailure = dataJud?.resultados?.firstOrNull { it.mensagem != null }
        if (firstFailure != null) {
            CartaoStatus(
                titulo = stringResource(R.string.monitoramento_datajud_pendente_title),
                mensagem = stringResource(R.string.monitoramento_datajud_pendente_message),
                isErro = false,
            )
        }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
) {
    val dimens = ObiterTheme.dimens

    Surface(
        shape = RoundedCornerShape(dimens.radiusSmall),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = dimens.space3,
                vertical = dimens.space2,
            ),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonitoramentoUiError.messageResId(): Int =
    when (this) {
        MonitoramentoUiError.InvalidDate -> R.string.monitoramento_error_invalid_date
        MonitoramentoUiError.SyncFailed -> R.string.monitoramento_error_sync_failed
    }

@Composable
private fun djenStopReasonText(reason: MonitorarDjenStopReason): String =
    stringResource(
        when (reason) {
            MonitorarDjenStopReason.EMPTY_PAGE -> R.string.monitoramento_djen_stop_empty_page
            MonitorarDjenStopReason.PARTIAL_PAGE -> R.string.monitoramento_djen_stop_partial_page
            MonitorarDjenStopReason.COUNT_CONSUMED -> R.string.monitoramento_djen_stop_count_consumed
            MonitorarDjenStopReason.REPEATED_PAGE -> R.string.monitoramento_djen_stop_repeated_page
            MonitorarDjenStopReason.PAGE_LIMIT -> R.string.monitoramento_djen_stop_page_limit
            MonitorarDjenStopReason.UNKNOWN -> R.string.monitoramento_djen_stop_unknown
        },
    )
