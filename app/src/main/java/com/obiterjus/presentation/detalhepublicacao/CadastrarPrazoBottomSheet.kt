package com.obiterjus.presentation.detalhepublicacao

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obiterjus.R
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.domain.model.ConfiancaCalculo
import com.obiterjus.domain.model.ProvedorCalendario
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.presentation.componentes.seletores.RoletaSeletor
import com.obiterjus.ui.theme.ObiterTheme

private const val DIAS_MAXIMO = 120

/**
 * Bottom sheet do cadastro manual de prazo: roleta de dias + tipo de contagem,
 * cálculo pela API CalendárioForense, confirmação da data e escolha da agenda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastrarPrazoBottomSheet(
    fluxo: FluxoCadastroPrazo,
    prazoAtual: PublicacaoPrazo?,
    dataExpediente: java.time.LocalDate?,
    aoAlterarSelecao: (quantidadeDias: Int, diasUteis: Boolean) -> Unit,
    aoCalcular: () -> Unit,
    aoConfirmarData: () -> Unit,
    aoVoltarParaSelecao: () -> Unit,
    aoConfirmarProvedor: (ProvedorCalendario) -> Unit,
    aoFechar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (fluxo is FluxoCadastroPrazo.Fechado) return

    // skipPartiallyExpanded evita o conflito entre o drag do sheet e a roleta
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dimens = ObiterTheme.dimens

    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenMargin)
                .padding(bottom = dimens.screenMargin),
            verticalArrangement = Arrangement.spacedBy(dimens.space3),
        ) {
            Text(
                text = stringResource(
                    if (prazoAtual != null) {
                        R.string.prazo_manual_sheet_titulo_editar
                    } else {
                        R.string.prazo_manual_sheet_titulo
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            when (fluxo) {
                is FluxoCadastroPrazo.Selecionando -> ConteudoSelecao(
                    quantidadeDias = fluxo.quantidadeDias,
                    diasUteis = fluxo.diasUteis,
                    prazoAtual = prazoAtual,
                    calculando = false,
                    aoAlterarSelecao = aoAlterarSelecao,
                    aoCalcular = aoCalcular,
                )

                is FluxoCadastroPrazo.Calculando -> ConteudoSelecao(
                    quantidadeDias = fluxo.quantidadeDias,
                    diasUteis = fluxo.diasUteis,
                    prazoAtual = prazoAtual,
                    calculando = true,
                    aoAlterarSelecao = { _, _ -> },
                    aoCalcular = {},
                )

                is FluxoCadastroPrazo.Resultado -> ConteudoResultado(
                    fluxo = fluxo,
                    dataExpediente = dataExpediente,
                    aoVoltar = aoVoltarParaSelecao,
                    aoConfirmar = aoConfirmarData,
                )

                is FluxoCadastroPrazo.ErroCalculo -> ConteudoErro(
                    fluxo = fluxo,
                    aoTentarNovamente = aoCalcular,
                    aoVoltar = aoVoltarParaSelecao,
                )

                is FluxoCadastroPrazo.EscolhendoProvedor -> ConteudoProvedor(
                    dataCalculada = fluxo.dataCalculada,
                    salvando = false,
                    aoConfirmarProvedor = aoConfirmarProvedor,
                    aoVoltar = aoVoltarParaSelecao,
                )

                is FluxoCadastroPrazo.Salvando -> ConteudoProvedor(
                    dataCalculada = fluxo.dataCalculada,
                    salvando = true,
                    aoConfirmarProvedor = {},
                    aoVoltar = {},
                )

                FluxoCadastroPrazo.Fechado -> Unit
            }
        }
    }
}

@Composable
private fun ConteudoSelecao(
    quantidadeDias: Int,
    diasUteis: Boolean,
    prazoAtual: PublicacaoPrazo?,
    calculando: Boolean,
    aoAlterarSelecao: (Int, Boolean) -> Unit,
    aoCalcular: () -> Unit,
) {
    val dimens = ObiterTheme.dimens

    Column(verticalArrangement = Arrangement.spacedBy(dimens.space3)) {
        if (prazoAtual != null) {
            AvisoSubstituicao(prazoAtual = prazoAtual)
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = diasUteis,
                onClick = { aoAlterarSelecao(quantidadeDias, true) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text(
                    text = stringResource(R.string.prazo_manual_dias_uteis),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            SegmentedButton(
                selected = !diasUteis,
                onClick = { aoAlterarSelecao(quantidadeDias, false) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text(
                    text = stringResource(R.string.prazo_manual_dias_corridos),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }

        Text(
            text = stringResource(R.string.prazo_manual_rotulo_dias),
            style = MaterialTheme.typography.labelLarge,
            color = ObiterTheme.colors.textMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        RoletaSeletor(
            itens = (1..DIAS_MAXIMO).map(Int::toString),
            indiceSelecionado = quantidadeDias - 1,
            aoSelecionar = { indice -> aoAlterarSelecao(indice + 1, diasUteis) },
            modifier = Modifier
                .width(140.dp)
                .align(Alignment.CenterHorizontally),
        )

        BotaoPrincipal(
            texto = stringResource(
                if (calculando) R.string.prazo_manual_calculando else R.string.prazo_manual_calcular,
            ),
            carregando = calculando,
            onClick = aoCalcular,
        )
    }
}

@Composable
private fun ConteudoResultado(
    fluxo: FluxoCadastroPrazo.Resultado,
    dataExpediente: java.time.LocalDate?,
    aoVoltar: () -> Unit,
    aoConfirmar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens

    Column(verticalArrangement = Arrangement.spacedBy(dimens.space3)) {
        Text(
            text = stringResource(
                R.string.prazo_manual_resultado_pergunta,
                FormatadorData.formatarData(fluxo.dataCalculada),
            ),
            style = MaterialTheme.typography.headlineSmall,
        )
        if (dataExpediente != null) {
            Text(
                text = stringResource(
                    R.string.prazo_manual_resultado_detalhe,
                    fluxo.quantidadeDias,
                    stringResource(
                        if (fluxo.diasUteis) {
                            R.string.prazo_manual_dias_uteis
                        } else {
                            R.string.prazo_manual_dias_corridos
                        },
                    ).lowercase(),
                    FormatadorData.formatarData(dataExpediente),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = ObiterTheme.colors.textMuted,
            )
        }

        if (fluxo.confianca == ConfiancaCalculo.INCERTO) {
            AvisoDestacado(texto = stringResource(R.string.prazo_manual_aviso_incerto))
        }

        LinhaVoltarEAcao(
            textoAcao = stringResource(R.string.prazo_manual_confirmar),
            aoVoltar = aoVoltar,
            aoConfirmar = aoConfirmar,
        )
    }
}

@Composable
private fun ConteudoErro(
    fluxo: FluxoCadastroPrazo.ErroCalculo,
    aoTentarNovamente: () -> Unit,
    aoVoltar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens

    Column(verticalArrangement = Arrangement.spacedBy(dimens.space3)) {
        AvisoDestacado(
            texto = stringResource(
                when {
                    fluxo.tribunalAusente -> R.string.prazo_manual_erro_tribunal
                    fluxo.bloqueadoPelaApi -> R.string.prazo_manual_erro_bloqueado
                    else -> R.string.prazo_manual_erro_rede
                },
            ),
            cor = ObiterTheme.colors.danger,
        )

        if (fluxo.tribunalAusente || fluxo.bloqueadoPelaApi) {
            BotaoPrincipal(
                texto = stringResource(R.string.prazo_manual_voltar),
                carregando = false,
                onClick = aoVoltar,
            )
        } else {
            LinhaVoltarEAcao(
                textoAcao = stringResource(R.string.acao_tentar_novamente),
                aoVoltar = aoVoltar,
                aoConfirmar = aoTentarNovamente,
            )
        }
    }
}

@Composable
private fun ConteudoProvedor(
    dataCalculada: java.time.LocalDate,
    salvando: Boolean,
    aoConfirmarProvedor: (ProvedorCalendario) -> Unit,
    aoVoltar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    var provedorSelecionado by rememberSaveable { mutableStateOf(ProvedorCalendario.GOOGLE) }
    val provedores = listOf(
        ProvedorCalendario.GOOGLE to stringResource(R.string.prazos_provedor_google),
        ProvedorCalendario.OUTLOOK to stringResource(R.string.prazos_provedor_outlook),
        ProvedorCalendario.LOCAL to stringResource(R.string.prazos_provedor_local),
    )

    Column(verticalArrangement = Arrangement.spacedBy(dimens.space2)) {
        Text(
            text = stringResource(
                R.string.prazo_manual_vencimento_atual,
                FormatadorData.formatarData(dataCalculada),
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.prazo_manual_escolher_agenda),
            style = MaterialTheme.typography.bodyMedium,
            color = ObiterTheme.colors.textMuted,
        )

        provedores.forEach { (provedor, rotulo) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !salvando) { provedorSelecionado = provedor }
                    .padding(vertical = dimens.space1),
            ) {
                RadioButton(
                    selected = provedorSelecionado == provedor,
                    onClick = { provedorSelecionado = provedor },
                    enabled = !salvando,
                )
                Spacer(modifier = Modifier.width(dimens.space2))
                Text(text = rotulo)
            }
        }

        LinhaVoltarEAcao(
            textoAcao = stringResource(
                if (salvando) R.string.prazo_manual_salvando else R.string.prazo_manual_salvar,
            ),
            carregando = salvando,
            aoVoltar = aoVoltar,
            aoConfirmar = { aoConfirmarProvedor(provedorSelecionado) },
        )
    }
}

@Composable
private fun AvisoSubstituicao(prazoAtual: PublicacaoPrazo) {
    val dataAtual = prazoAtual.dataLimiteEstimada
    val mensagem = if (dataAtual != null) {
        stringResource(
            R.string.prazo_manual_aviso_substituicao,
            FormatadorData.formatarData(dataAtual),
        )
    } else {
        stringResource(R.string.prazo_manual_aviso_substituicao_sem_data)
    }
    val textoCompleto = if (prazoAtual.idExternoCalendario != null) {
        "$mensagem ${stringResource(R.string.prazo_manual_aviso_evento_removido)}"
    } else {
        mensagem
    }
    AvisoDestacado(texto = textoCompleto)
}

@Composable
private fun AvisoDestacado(
    texto: String,
    cor: androidx.compose.ui.graphics.Color = ObiterTheme.colors.warning,
) {
    val dimens = ObiterTheme.dimens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.cardRadius),
        color = if (cor == ObiterTheme.colors.warning) {
            ObiterTheme.colors.warningPale
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(dimens.borderWidth, cor),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = cor,
            modifier = Modifier.padding(dimens.cardPaddingH),
        )
    }
}

@Composable
private fun BotaoPrincipal(
    texto: String,
    carregando: Boolean,
    onClick: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    Button(
        onClick = onClick,
        enabled = !carregando,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (carregando) {
            CircularProgressIndicator(
                modifier = Modifier.width(dimens.iconSearchSize),
                strokeWidth = dimens.borderWidth,
            )
            Spacer(modifier = Modifier.width(dimens.space1))
        }
        Text(text = texto)
    }
}

@Composable
private fun LinhaVoltarEAcao(
    textoAcao: String,
    aoVoltar: () -> Unit,
    aoConfirmar: () -> Unit,
    carregando: Boolean = false,
) {
    val dimens = ObiterTheme.dimens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = aoVoltar,
            enabled = !carregando,
        ) {
            Text(text = stringResource(R.string.prazo_manual_voltar))
        }
        Button(
            onClick = aoConfirmar,
            enabled = !carregando,
            modifier = Modifier.weight(1f),
        ) {
            if (carregando) {
                CircularProgressIndicator(
                    modifier = Modifier.width(dimens.iconSearchSize),
                    strokeWidth = dimens.borderWidth,
                )
                Spacer(modifier = Modifier.width(dimens.space1))
            }
            Text(text = textoAcao)
        }
    }
}
