package com.obiterjus.presentation.editarprocesso

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.obiterjus.R
import com.obiterjus.domain.model.Cliente
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.ui.theme.ObiterTheme

/**
 * Escolha entre reaproveitar um cliente da carteira ou cadastrar um novo, no
 * momento em que uma parte é marcada como cliente. É o ponto onde a carteira
 * deixa de acumular duplicatas do mesmo cliente a cada processo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetVincularCliente(
    sugestao: SugestaoCliente,
    aoVincular: (String) -> Unit,
    aoCriarNovo: () -> Unit,
    aoDispensar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = aoDispensar,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenMargin)
                .padding(bottom = dimens.space6),
            verticalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            Text(
                text = stringResource(R.string.vincular_cliente_titulo),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (sugestao.candidatos.isEmpty()) {
                    stringResource(R.string.vincular_cliente_sem_candidatos, sugestao.nomeParticipante)
                } else {
                    stringResource(R.string.vincular_cliente_com_candidatos, sugestao.nomeParticipante)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )

            sugestao.candidatos.forEach { candidato ->
                HorizontalDivider(color = colors.divider, thickness = dimens.borderWidth)
                LinhaCandidato(cliente = candidato, aoClicar = { aoVincular(candidato.id) })
            }

            HorizontalDivider(color = colors.divider, thickness = dimens.borderWidth)

            TextButton(
                onClick = aoCriarNovo,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = ObiterIcones.Adicionar,
                    contentDescription = null,
                    modifier = Modifier.size(dimens.iconStarSize),
                )
                Text(
                    text = stringResource(R.string.vincular_cliente_criar_novo),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = dimens.chipRowGap),
                )
            }
        }
    }
}

@Composable
private fun LinhaCandidato(
    cliente: Cliente,
    aoClicar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoClicar)
            .padding(vertical = dimens.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
    ) {
        Icon(
            imageVector = ObiterIcones.Cliente,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(dimens.iconStarSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cliente.nome,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = cliente.documento?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.clientes_sem_documento),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
        Text(
            text = stringResource(R.string.vincular_cliente_vincular),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
