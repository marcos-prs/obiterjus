package com.obiterjus.presentation.auditoria

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.obiterjus.R
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.data.auditoria.local.SyncLogEntity
import com.obiterjus.presentation.componentes.CabecalhoListagem
import com.obiterjus.presentation.componentes.CampoDetalheListagem
import com.obiterjus.presentation.componentes.CartaoItemListagem
import com.obiterjus.presentation.componentes.ChipInformativoListagem
import com.obiterjus.presentation.componentes.ConteudoRolavelAba
import com.obiterjus.presentation.componentes.EstadoVazioListagem
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun TelaAuditoria(
    estado: EstadoAuditoria,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens

    ConteudoRolavelAba(modifier = modifier) {
        CabecalhoListagem(
            titulo = stringResource(R.string.auditoria_title),
            subtitulo = stringResource(R.string.auditoria_subtitle, estado.logs.size),
        )

        if (estado.logs.isEmpty()) {
            EstadoVazioListagem(
                titulo = stringResource(R.string.auditoria_empty_title),
                corpo = stringResource(R.string.auditoria_empty_body),
            )
        } else {
            estado.logs.forEach { log ->
                ItemSyncLog(log = log)
            }
        }
    }
}

@Composable
private fun ItemSyncLog(log: SyncLogEntity) {
    val dimens = ObiterTheme.dimens

    CartaoItemListagem(
        isHighlighted = !log.sucesso,
        onClick = {},
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.space2),
            verticalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            ChipInformativoListagem(
                texto = log.fonte,
            )
            ChipInformativoListagem(
                texto = if (log.sucesso) {
                    stringResource(R.string.auditoria_status_sucesso)
                } else {
                    stringResource(R.string.auditoria_status_falha)
                },
                icone = if (log.sucesso) Icons.Default.Check else Icons.Default.Warning,
            )
            log.duracaoMs?.let { ms ->
                ChipInformativoListagem(texto = "${ms}ms")
            }
        }

        CampoDetalheListagem(
            rotulo = stringResource(R.string.auditoria_label_executado_em),
            valor = FormatadorData.formatarDataHora(log.executadoEm),
        )

        if (log.novasPublicacoes > 0) {
            CampoDetalheListagem(
                rotulo = stringResource(R.string.auditoria_label_novas),
                valor = log.novasPublicacoes.toString(),
            )
        }

        if (log.processosSincronizados > 0) {
            CampoDetalheListagem(
                rotulo = stringResource(R.string.auditoria_label_processos),
                valor = log.processosSincronizados.toString(),
            )
        }

        log.mensagemErro?.let { erro ->
            Text(
                text = erro,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
