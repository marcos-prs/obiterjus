package com.obiterjus.presentation.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraSuperiorPrincipal
import com.obiterjus.presentation.componentes.cards.CardEstatistica
import com.obiterjus.presentation.componentes.cards.CardPublicacaoResumo
import com.obiterjus.presentation.componentes.strips.AlertaStrip
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun InicioScreen(
    viewModel: InicioViewModel,
    aoNavegarParaPrazos: () -> Unit,
    aoVerTodasPublicacoes: () -> Unit,
    aoAbrirPublicacao: (Long) -> Unit,
    aoNavegarParaProcessos: () -> Unit,
    aoAbrirCliente: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        BarraSuperiorPrincipal(
            nomeUsuario = estado.nomeUsuario,
            numeroOab = estado.oab,
            ufOab = estado.uf,
            ultimaSincronizacao = estado.ultimaSincronizacaoTexto,
        )

        Column(
            modifier = Modifier.padding(dimens.screenMargin),
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
                    modifier = Modifier.weight(PESO_CARD_ESTATISTICA).clickable { aoVerTodasPublicacoes() },
                )
                CardEstatistica(
                    valor = (estado.prazosUrgentes + estado.prazosProximos).toString(),
                    rotulo = stringResource(R.string.inicio_estatistica_prazos_ativos),
                    corPonto = colors.accent,
                    modifier = Modifier.weight(PESO_CARD_ESTATISTICA).clickable { aoNavegarParaPrazos() },
                )
                CardEstatistica(
                    valor = estado.totalProcessos.toString(),
                    rotulo = stringResource(R.string.inicio_estatistica_processos),
                    corPonto = colors.success,
                    modifier = Modifier.weight(PESO_CARD_ESTATISTICA).clickable { aoNavegarParaProcessos() },
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
                    val cliente = publicacao.numeroProcesso
                        ?.let { numero -> estado.clientesPorProcesso[numero] }
                    CardPublicacaoResumo(
                        publicacao = publicacao,
                        nomeCliente = cliente?.nome,
                        aoClicarCliente = cliente?.clienteId?.let { id ->
                            { aoAbrirCliente(id) }
                        },
                        onClick = { aoAbrirPublicacao(publicacao.id) },
                        badgeOrdem = estado.metadadosOrdemPublicacoes[publicacao.id]
                            ?.first
                            ?.let { ordem -> stringResource(R.string.publicacoes_badge_ordem, ordem) },
                    )
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
}

private const val PESO_CARD_ESTATISTICA = 1f
