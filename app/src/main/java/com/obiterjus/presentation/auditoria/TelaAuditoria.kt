package com.obiterjus.presentation.auditoria

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.data.auditoria.local.SyncLogEntity
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.chips.BadgeTipoAto
import com.obiterjus.presentation.componentes.chips.VarianteBadge
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun TelaAuditoria(
    viewModel: AuditoriaViewModel,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    TelaAuditoria(
        estado = estado,
        modifier = modifier,
    )
}

@Composable
fun TelaAuditoria(
    estado: EstadoAuditoria,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(dimens.screenMargin),
        verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
    ) {
        if (estado.logs.isEmpty()) {
            item {
                EstadoVazioObiter(
                    titulo = stringResource(R.string.auditoria_empty_title),
                    corpo = stringResource(R.string.auditoria_empty_body),
                    icone = ObiterIcones.Historico,
                )
            }
        } else {
            items(
                items = estado.logs,
                key = { log -> log.id },
            ) { log ->
                ItemSyncLog(log = log)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemSyncLog(log: SyncLogEntity) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.cardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(dimens.borderWidth, colors.border),
    ) {
        Column(
            modifier = Modifier.padding(dimens.cardPaddingH),
            verticalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                verticalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
            ) {
                BadgeTipoAto(
                    texto = log.fonte,
                    variante = VarianteBadge.TRIBUNAL,
                )
                BadgeTipoAto(
                    texto = if (log.sucesso) {
                        stringResource(R.string.auditoria_status_sucesso)
                    } else {
                        stringResource(R.string.auditoria_status_falha)
                    },
                    variante = if (log.sucesso) VarianteBadge.FAVORAVEL else VarianteBadge.URGENTE,
                )
            }

            CampoAuditoria(
                rotulo = stringResource(R.string.auditoria_label_executado_em),
                valor = FormatadorData.formatarDataHora(log.executadoEm),
            )
            CampoAuditoria(
                rotulo = stringResource(R.string.auditoria_label_novas),
                valor = log.novasPublicacoes.toString(),
            )
            CampoAuditoria(
                rotulo = stringResource(R.string.auditoria_label_processos),
                valor = log.processosSincronizados.toString(),
            )
            log.mensagemErro?.let { erro ->
                Text(
                    text = erro,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CampoAuditoria(
    rotulo: String,
    valor: String,
) {
    Text(
        text = rotulo,
        style = MaterialTheme.typography.labelMedium,
        color = ObiterTheme.colors.textMuted,
    )
    Text(
        text = valor,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
