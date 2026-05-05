package com.obiterjus.presentation.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.cards.CardEstatistica
import com.obiterjus.presentation.componentes.cards.CardPublicacao
import com.obiterjus.presentation.componentes.cards.PrioridadeStripe
import com.obiterjus.presentation.componentes.strips.AlertaStrip
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun TelaInicio(
    viewModel: ModeloInicio,
    aoNavegarParaPrazos: () -> Unit,
    aoVerTodasPublicacoes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimens.screenMargin),
        verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
    ) {
        val mensagemPrazoUrgente = estado.prazoUrgenteMensagem
        if (estado.temPrazoUrgente && !mensagemPrazoUrgente.isNullOrBlank()) {
            AlertaStrip(
                mensagem = mensagemPrazoUrgente,
                aoClicar = aoNavegarParaPrazos,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.cardGap),
        ) {
            CardEstatistica(
                valor = estado.totalPublicacoes.toString(),
                rotulo = stringResource(R.string.inicio_estatistica_publicacoes),
                corPonto = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(PESO_CARD_ESTATISTICA),
            )
            CardEstatistica(
                valor = (estado.prazosUrgentes + estado.prazosProximos).toString(),
                rotulo = stringResource(R.string.inicio_estatistica_prazos_ativos),
                corPonto = colors.accent,
                modifier = Modifier.weight(PESO_CARD_ESTATISTICA),
            )
            CardEstatistica(
                valor = estado.totalProcessos.toString(),
                rotulo = stringResource(R.string.inicio_estatistica_processos),
                corPonto = colors.success,
                modifier = Modifier.weight(PESO_CARD_ESTATISTICA),
            )
        }

        Text(
            text = stringResource(R.string.inicio_publicacoes_recentes),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = MaterialTheme.typography.labelSmall.fontSize * 0.07f,
            ),
            color = colors.textMuted,
            modifier = Modifier.padding(top = dimens.sectionGap),
        )

        if (estado.publicacoesRecentes.isEmpty()) {
            Text(
                text = stringResource(R.string.inicio_sem_publicacoes_recentes),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
        } else {
            estado.publicacoesRecentes.forEach { publicacao ->
                CardPublicacaoInicio(publicacao = publicacao)
            }
            TextButton(
                onClick = aoVerTodasPublicacoes,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = stringResource(R.string.inicio_ver_todas),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        if (estado.totalProcessos == 0 && estado.totalPublicacoes == 0) {
            EstadoVazioObiter(
                titulo = stringResource(R.string.inicio_empty_title),
                corpo = stringResource(R.string.inicio_empty_body),
                icone = ObiterIcones.InicioInativo,
                modifier = Modifier.padding(top = dimens.space4),
            )
        }
    }
}

@Composable
private fun CardPublicacaoInicio(
    publicacao: Publicacao,
) {
    CardPublicacao(
        tituloAto = publicacao.tipoComunicacao ?: stringResource(R.string.publicacoes_sem_tipo),
        tipoAto = publicacao.tipoComunicacao ?: stringResource(R.string.publicacoes_sem_tipo),
        data = publicacao.dataDisponibilizacao?.let(FormatadorData::formatarData)
            ?: stringResource(R.string.publicacoes_sem_data),
        numeroProcesso = publicacao.numeroProcesso
            ?: stringResource(R.string.publicacoes_sem_numero_processo),
        prazoDias = publicacao.prazo?.quantidade?.let { dias ->
            stringResource(R.string.prazos_badge_dias, dias)
        },
        trechoTexto = publicacao.textoLimpo,
        prioridade = PrioridadeStripe.ROTINEIRO,
        aoClicar = {},
    )
}

private const val PESO_CARD_ESTATISTICA = 1f
