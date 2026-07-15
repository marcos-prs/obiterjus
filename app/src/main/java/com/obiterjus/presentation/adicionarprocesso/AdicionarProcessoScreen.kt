package com.obiterjus.presentation.adicionarprocesso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraSuperiorSecundaria
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun AdicionarProcessoScreen(
    viewModel: AdicionarProcessoViewModel,
    onVoltar: () -> Unit,
    onProcessoAdicionado: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperiorSecundaria(
            titulo = stringResource(R.string.adicionar_processo_title),
            subtitulo = stringResource(R.string.adicionar_processo_subtitle),
            onVoltar = onVoltar,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.screenMargin),
            verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
        ) {
            Spacer(modifier = Modifier.height(dimens.space2))

            Text(
                text = stringResource(R.string.adicionar_processo_label_numero),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = estado.numeroInput,
                onValueChange = { viewModel.aoAlterarNumero(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = {
                    Text(
                        text = stringResource(R.string.adicionar_processo_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                },
                shape = RoundedCornerShape(dimens.searchBarRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = colors.border,
                ),
                enabled = estado.status != StatusAdicao.BUSCANDO,
            )

            if (estado.status == StatusAdicao.NUMERO_INVALIDO) {
                Text(
                    text = stringResource(R.string.adicionar_processo_numero_invalido),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.danger,
                )
            }

            when (estado.status) {
                StatusAdicao.BUSCANDO -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(dimens.iconWarningSize),
                            strokeWidth = dimens.borderWidth * 2,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.adicionar_processo_buscando),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                        )
                    }
                }
                StatusAdicao.SUCESSO_PENDENTE -> {
                    Surface(
                        shape = RoundedCornerShape(dimens.alertStripRightRadius),
                        color = colors.warningPale,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.adicionar_processo_pendente),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.warning,
                            modifier = Modifier.padding(dimens.cardPaddingH, dimens.cardPaddingV),
                        )
                    }
                }
                StatusAdicao.SUCESSO -> {
                    Surface(
                        shape = RoundedCornerShape(dimens.alertStripRightRadius),
                        color = colors.successPale,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = estado.processoSyncResumo?.resultados
                                ?.firstOrNull()
                                ?.numeroProcesso
                                ?.let { stringResource(R.string.adicionar_processo_sucesso, it) }
                                ?: stringResource(R.string.adicionar_processo_sucesso, ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.success,
                            modifier = Modifier.padding(dimens.cardPaddingH, dimens.cardPaddingV),
                        )
                    }
                }
                StatusAdicao.NAO_ENCONTRADO -> {
                    Surface(
                        shape = RoundedCornerShape(dimens.alertStripRightRadius),
                        color = colors.warningPale,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.adicionar_processo_nao_encontrado),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.warning,
                            modifier = Modifier.padding(dimens.cardPaddingH, dimens.cardPaddingV),
                        )
                    }
                }
                StatusAdicao.ERRO_API,
                StatusAdicao.FALHA,
                -> {
                    val erroMsg = estado.mensagemErro
                    Surface(
                        shape = RoundedCornerShape(dimens.alertStripRightRadius),
                        color = colors.dangerPale,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(dimens.cardPaddingH, dimens.cardPaddingV),
                        ) {
                            Text(
                                text = stringResource(R.string.adicionar_processo_falha),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.danger,
                            )
                            if (erroMsg != null) {
                                Text(
                                    text = erroMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.danger.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }
                StatusAdicao.IDLE,
                StatusAdicao.NUMERO_INVALIDO,
                -> { /* feedback already shown */ }
            }

            Spacer(modifier = Modifier.weight(1f))

            val buscarEnabled = estado.numeroInput.isNotBlank() &&
                estado.status != StatusAdicao.BUSCANDO

            Button(
                onClick = { viewModel.aoBuscar() },
                modifier = Modifier.fillMaxWidth(),
                enabled = buscarEnabled,
                shape = RoundedCornerShape(dimens.searchBarRadius),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = colors.divider,
                    disabledContentColor = colors.textMuted,
                ),
            ) {
                Text(
                    text = stringResource(R.string.adicionar_processo_action_buscar),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            if (estado.status == StatusAdicao.SUCESSO || estado.status == StatusAdicao.SUCESSO_PENDENTE) {
                Button(
                    onClick = {
                        estado.processoSyncResumo?.resultados
                            ?.firstOrNull()
                            ?.numeroProcesso
                            ?.let(onProcessoAdicionado)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dimens.searchBarRadius),
                ) {
                    Text(
                        text = stringResource(R.string.cd_voltar),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
