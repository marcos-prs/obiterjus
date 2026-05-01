package com.obiterjus.presentation.publicacoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.obiterjus.R
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoParticipante
import com.obiterjus.domain.model.PublicacaoPrazo
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

private const val PESO_CONTEUDO_ITEM_PUBLICACAO = 1f

@Composable
fun TelaPublicacoes(
    estado: EstadoPublicacoes,
    aoAlterarFiltroTexto: (String) -> Unit,
    aoAlterarFiltroTribunal: (String) -> Unit,
    aoAlterarFiltroTipo: (String) -> Unit,
    aoAlterarFiltroDataInicio: (String) -> Unit,
    aoAlterarFiltroDataFim: (String) -> Unit,
    aoAlternarSomenteSigilosas: () -> Unit,
    aoLimparFiltros: () -> Unit,
    aoSelecionarPublicacao: (Long) -> Unit,
    aoFecharDetalhe: () -> Unit,
    aoAbrirCertidao: (Publicacao) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens

    ConteudoRolavelAba(modifier = modifier) {
        CabecalhoListagem(
            titulo = stringResource(R.string.publicacoes_title),
            subtitulo = stringResource(
                R.string.publicacoes_subtitle,
                estado.publicacoes.size,
                estado.totalPersistidas,
            ),
        )

        CartaoFiltro {
            OutlinedTextField(
                value = estado.filtros.texto,
                onValueChange = aoAlterarFiltroTexto,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.publicacoes_label_filtro)) },
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
                value = estado.filtros.tribunal,
                onValueChange = aoAlterarFiltroTribunal,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.publicacoes_label_tribunal)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            )

            OutlinedTextField(
                value = estado.filtros.tipoComunicacao,
                onValueChange = aoAlterarFiltroTipo,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.publicacoes_label_tipo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.space3),
            ) {
                OutlinedTextField(
                    value = estado.filtros.dataInicio,
                    onValueChange = aoAlterarFiltroDataInicio,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.publicacoes_label_data_inicio)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = estado.filtros.dataFim,
                    onValueChange = aoAlterarFiltroDataFim,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.publicacoes_label_data_fim)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.space2),
                verticalArrangement = Arrangement.spacedBy(dimens.space2),
            ) {
                FilterChip(
                    selected = estado.filtros.somenteSigilosas,
                    onClick = aoAlternarSomenteSigilosas,
                    label = { Text(stringResource(R.string.publicacoes_filtro_sigilosas)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                        )
                    },
                )
                OutlinedButton(
                    onClick = aoLimparFiltros,
                    enabled = estado.filtros.possuiFiltrosAtivos,
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                    )
                    Text(stringResource(R.string.publicacoes_filtro_limpar))
                }
            }
        }

        estado.publicacaoSelecionada?.let { publicacao ->
            DetalhePublicacao(
                publicacao = publicacao,
                aoFechar = aoFecharDetalhe,
                isCarregandoCertidao = estado.certidao.isLoading,
                certidaoComErro = estado.certidao.error,
                aoAbrirCertidao = aoAbrirCertidao,
            )
        }

        if (estado.publicacoes.isEmpty()) {
            EstadoVazioListagem(
                titulo = stringResource(R.string.publicacoes_empty_title),
                corpo = stringResource(R.string.publicacoes_empty_body),
            )
        } else {
            estado.publicacoes.forEach { publicacao ->
                ItemPublicacao(
                    publicacao = publicacao,
                    aoSelecionar = aoSelecionarPublicacao,
                )
            }
        }
    }
}

@Composable
private fun ItemPublicacao(
    publicacao: Publicacao,
    aoSelecionar: (Long) -> Unit,
) {
    val dimens = ObiterTheme.dimens

    CartaoItemListagem(
        onClick = { aoSelecionar(publicacao.id) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(PESO_CONTEUDO_ITEM_PUBLICACAO),
                verticalArrangement = Arrangement.spacedBy(dimens.space1),
            ) {
                Text(
                    text = publicacao.numeroProcesso
                        ?: stringResource(R.string.publicacoes_sem_numero_processo),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = publicacao.dataDisponibilizacao?.let(FormatadorData::formatarData)
                        ?: stringResource(R.string.publicacoes_sem_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (publicacao.isSigiloso) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.publicacoes_sigilo_content_description),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.space2),
            verticalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            ChipInformativoListagem(publicacao.tribunal ?: stringResource(R.string.publicacoes_sem_tribunal))
            ChipInformativoListagem(publicacao.tipoComunicacao ?: stringResource(R.string.publicacoes_sem_tipo))
            ChipInformativoListagem(publicacao.fonte, icone = Icons.Default.Source)
            publicacao.prazo?.let { prazo ->
                ChipInformativoListagem(prazo.formatarResumo())
            }
        }

        publicacao.nomeOrgao?.let { orgao ->
            Text(
                text = orgao,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        publicacao.participantes.firstOrNull()?.let { participante ->
            Text(
                text = "${participante.tipo}: ${participante.nome}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = if (publicacao.isSigiloso) {
                stringResource(R.string.publicacoes_texto_sigiloso)
            } else {
                publicacao.textoLimpo?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.publicacoes_sem_texto)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DetalhePublicacao(
    publicacao: Publicacao,
    aoFechar: () -> Unit,
    isCarregandoCertidao: Boolean,
    certidaoComErro: Boolean,
    aoAbrirCertidao: (Publicacao) -> Unit,
) {
    val dimens = ObiterTheme.dimens

    CartaoDetalhe {
        CabecalhoDetalhe(
            titulo = stringResource(R.string.publicacoes_detalhe_title),
            subtitulo = publicacao.numeroProcesso
                ?: stringResource(R.string.publicacoes_sem_numero_processo),
            fecharDescricao = stringResource(R.string.publicacoes_detalhe_fechar),
            aoFechar = aoFechar,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.space2),
            verticalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            ChipInformativoListagem(publicacao.tribunal ?: stringResource(R.string.publicacoes_sem_tribunal))
            ChipInformativoListagem(publicacao.tipoComunicacao ?: stringResource(R.string.publicacoes_sem_tipo))
            ChipInformativoListagem(publicacao.fonte, icone = Icons.Default.Source)
            if (publicacao.isSigiloso) {
                ChipInformativoListagem(stringResource(R.string.publicacoes_detalhe_sigilosa))
            }
        }

        if (isCarregandoCertidao) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = { aoAbrirCertidao(publicacao) },
            enabled = !isCarregandoCertidao && publicacao.hash?.isNotBlank() == true,
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
            )
            Text(stringResource(R.string.publicacoes_detalhe_certidao))
        }

        if (certidaoComErro) {
            Text(
                text = stringResource(R.string.publicacoes_detalhe_certidao_erro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        CampoDetalheListagem(
            rotulo = stringResource(R.string.publicacoes_detalhe_id),
            valor = publicacao.id.toString(),
        )
        CampoDetalheListagem(
            rotulo = stringResource(R.string.publicacoes_detalhe_data_disponibilizacao),
            valor = publicacao.dataDisponibilizacao?.let(FormatadorData::formatarData)
                ?: stringResource(R.string.publicacoes_sem_data),
        )
        CampoDetalheListagem(
            rotulo = stringResource(R.string.publicacoes_detalhe_orgao),
            valor = publicacao.nomeOrgao
                ?: stringResource(R.string.publicacoes_detalhe_nao_informado),
        )
        CampoDetalheListagem(
            rotulo = stringResource(R.string.publicacoes_detalhe_participantes),
            valor = publicacao.participantes.formatarParticipantes()
                ?: stringResource(R.string.publicacoes_detalhe_nao_informado),
        )
        CampoDetalheListagem(
            rotulo = stringResource(R.string.publicacoes_detalhe_prazo),
            valor = publicacao.prazo?.formatarDetalhe()
                ?: stringResource(R.string.publicacoes_detalhe_nao_informado),
        )
        CampoDetalheListagem(
            rotulo = stringResource(R.string.publicacoes_detalhe_capturado_em),
            valor = FormatadorData.formatarDataHora(publicacao.capturadoEm),
        )
        CampoDetalheListagem(
            rotulo = stringResource(R.string.publicacoes_detalhe_atualizado_em),
            valor = FormatadorData.formatarDataHora(publicacao.atualizadoEm),
        )
        CampoDetalheListagem(
            rotulo = stringResource(R.string.publicacoes_detalhe_texto),
            valor = if (publicacao.isSigiloso) {
                stringResource(R.string.publicacoes_texto_sigiloso)
            } else {
                publicacao.textoLimpo?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.publicacoes_sem_texto)
            },
        )
    }
}

private fun List<PublicacaoParticipante>.formatarParticipantes(): String? =
    takeIf { it.isNotEmpty() }?.joinToString(separator = "\n") { participante ->
        buildString {
            append(participante.tipo)
            append(": ")
            append(participante.nome)
            participante.documento?.let { documento ->
                append(" (")
                append(documento)
                append(")")
            }
        }
    }

private fun PublicacaoPrazo.formatarResumo(): String =
    buildString {
        append("Prazo: ")
        append(quantidade)
        append(' ')
        append(unidade)
        if (diasUteis) append(" úteis")
    }

private fun PublicacaoPrazo.formatarDetalhe(): String =
    buildString {
        append(formatarResumo())
        dataLimiteEstimada?.let { data ->
            append("\nEstimativa: ")
            append(FormatadorData.formatarData(data))
        }
        append("\nTrecho: ")
        append(textoOriginal)
    }
