package com.obiterjus.presentation.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.obiterjus.R
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.presentation.componentes.CabecalhoListagem
import com.obiterjus.presentation.componentes.CartaoItemListagem
import com.obiterjus.presentation.componentes.ChipInformativoListagem
import com.obiterjus.presentation.componentes.ConteudoRolavelAba
import com.obiterjus.presentation.componentes.EstadoVazioListagem
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun TelaAgendaPrazos(
    estado: EstadoAgendaPrazos,
    aoSelecionarPublicacao: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens

    ConteudoRolavelAba(modifier = modifier) {
        CabecalhoListagem(
            titulo = stringResource(R.string.agenda_title),
            subtitulo = stringResource(
                R.string.agenda_subtitle,
                estado.total,
                estado.vencidos,
                estado.proximos,
            ),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.space2),
            verticalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            ChipInformativoListagem(stringResource(R.string.agenda_metric_total, estado.total))
            ChipInformativoListagem(stringResource(R.string.agenda_metric_vencidos, estado.vencidos))
            ChipInformativoListagem(stringResource(R.string.agenda_metric_proximos, estado.proximos))
            if (estado.semData > 0) {
                ChipInformativoListagem(stringResource(R.string.agenda_metric_sem_data, estado.semData))
            }
        }

        if (estado.itens.isEmpty()) {
            EstadoVazioListagem(
                titulo = stringResource(R.string.agenda_empty_title),
                corpo = stringResource(R.string.agenda_empty_body),
            )
        } else {
            estado.itens.forEach { uiItem ->
                ItemAgendaPrazo(
                    uiItem = uiItem,
                    aoSelecionarPublicacao = aoSelecionarPublicacao,
                )
            }
        }
    }
}

@Composable
private fun ItemAgendaPrazo(
    uiItem: AgendaPrazoUiItem,
    aoSelecionarPublicacao: (Long) -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val item = uiItem.item
    val publicacao = item.publicacao
    val prazo = item.prazo

    CartaoItemListagem(
        onClick = { aoSelecionarPublicacao(publicacao.id) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.space1),
            ) {
                Text(
                    text = prazo.dataLimiteEstimada?.let { data ->
                        stringResource(R.string.agenda_item_vencimento, FormatadorData.formatarData(data))
                    } ?: stringResource(R.string.agenda_item_sem_data),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = publicacao.numeroProcesso
                        ?: stringResource(R.string.publicacoes_sem_numero_processo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.space2),
            verticalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            ChipInformativoListagem(
                texto = stringResource(uiItem.status.rotuloResId()),
                icone = Icons.Default.Event,
            )
            ChipInformativoListagem(prazo.formatarResumo())
            ChipInformativoListagem(publicacao.fonte, icone = Icons.Default.Source)
            publicacao.tribunal?.let { tribunal ->
                ChipInformativoListagem(tribunal)
            }
        }

        publicacao.nomeOrgao?.let { orgao ->
            Text(
                text = orgao,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = prazo.textoOriginal,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (!prazo.isConfirmado) {
            Spacer(modifier = Modifier.height(dimens.space2))
            Button(
                onClick = { /* ViewModel chamará o ConfirmarPrazoUC */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(dimens.space1))
                Text(stringResource(R.string.agenda_confirmar_prazo))
            }
        } else {
            Spacer(modifier = Modifier.height(dimens.space2))
            Text(
                text = prazo.provedorCalendario
                    ?.let { stringResource(R.string.agenda_prazo_confirmado, it) }
                    ?: stringResource(R.string.agenda_prazo_confirmado_local),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun PublicacaoPrazo.formatarResumo(): String =
    buildString {
        append(quantidade)
        append(' ')
        append(unidade)
        if (diasUteis) append(" úteis")
    }

private fun AgendaPrazoStatus.rotuloResId(): Int =
    when (this) {
        AgendaPrazoStatus.VENCIDO -> R.string.agenda_status_vencido
        AgendaPrazoStatus.PROXIMO -> R.string.agenda_status_proximo
        AgendaPrazoStatus.FUTURO -> R.string.agenda_status_futuro
        AgendaPrazoStatus.SEM_DATA -> R.string.agenda_status_sem_data
    }
