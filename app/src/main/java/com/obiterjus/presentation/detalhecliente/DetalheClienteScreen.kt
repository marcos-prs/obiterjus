package com.obiterjus.presentation.detalhecliente

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.core.texto.formatarCnj
import com.obiterjus.domain.logic.QualificacaoPorExtenso
import com.obiterjus.domain.model.Cliente
import com.obiterjus.domain.model.EnderecoCliente
import com.obiterjus.domain.model.TipoPessoa
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraSuperiorSecundaria
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun DetalheClienteScreen(
    viewModel: DetalheClienteViewModel,
    clienteId: String,
    onVoltar: () -> Unit,
    aoAbrirProcesso: (String) -> Unit,
    aoEditarCliente: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(clienteId) { viewModel.aoAbrirCliente(clienteId) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val clipboard = LocalClipboardManager.current
    val cliente = estado.cliente

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperiorSecundaria(
            titulo = cliente?.nome ?: stringResource(R.string.nav_clientes),
            onVoltar = onVoltar,
            acoes = {
                if (cliente != null) {
                    IconButton(onClick = { aoEditarCliente(cliente.id) }) {
                        Icon(
                            imageVector = ObiterIcones.Editar,
                            contentDescription = stringResource(R.string.editar_cliente_titulo),
                            tint = ObiterTheme.colors.onTopAppBar,
                        )
                    }
                }
            },
        )

        if (cliente == null) {
            // Só é "não encontrado" depois que o fluxo emitiu: antes disso o
            // cliente ainda pode estar a caminho do banco.
            if (!estado.carregando) {
                EstadoVazioObiter(
                    titulo = stringResource(R.string.cliente_nao_encontrado_title),
                    corpo = stringResource(R.string.cliente_nao_encontrado_body),
                    icone = ObiterIcones.Cliente,
                    modifier = Modifier.padding(dimens.screenMargin),
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(dimens.screenMargin),
            verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
        ) {
            item {
                SecaoCliente(titulo = stringResource(R.string.cliente_secao_identificacao)) {
                    LinhaDado(
                        rotulo = stringResource(R.string.cliente_campo_tipo),
                        valor = stringResource(cliente.tipoPessoa.rotuloResId()),
                    )
                    LinhaDado(
                        rotulo = stringResource(R.string.cliente_campo_documento),
                        valor = cliente.documento,
                    )
                    // Qualificação de pessoa física; numa PJ ela vive no
                    // representante e estes campos ficam vazios de propósito.
                    if (cliente.tipoPessoa == TipoPessoa.FISICA) {
                        LinhaDado(
                            rotulo = stringResource(R.string.cliente_campo_nacionalidade),
                            valor = cliente.nacionalidade,
                        )
                        LinhaDado(
                            rotulo = stringResource(R.string.cliente_campo_estado_civil),
                            valor = cliente.estadoCivil,
                        )
                        LinhaDado(
                            rotulo = stringResource(R.string.cliente_campo_profissao),
                            valor = cliente.profissao,
                        )
                    }
                }
            }

            cliente.representante?.let { representante ->
                item {
                    SecaoCliente(titulo = stringResource(R.string.cliente_secao_representante)) {
                        LinhaDado(
                            rotulo = stringResource(R.string.cliente_campo_nome),
                            valor = representante.nome,
                        )
                        LinhaDado(
                            rotulo = stringResource(R.string.cliente_campo_cargo),
                            valor = representante.cargo,
                        )
                        LinhaDado(
                            rotulo = stringResource(R.string.cliente_campo_documento),
                            valor = representante.documento,
                        )
                        LinhaDado(
                            rotulo = stringResource(R.string.cliente_campo_nacionalidade),
                            valor = representante.nacionalidade,
                        )
                        LinhaDado(
                            rotulo = stringResource(R.string.cliente_campo_estado_civil),
                            valor = representante.estadoCivil,
                        )
                        LinhaDado(
                            rotulo = stringResource(R.string.cliente_campo_profissao),
                            valor = representante.profissao,
                        )
                    }
                }
            }

            item {
                SecaoCliente(titulo = stringResource(R.string.cliente_secao_contato)) {
                    LinhaDado(
                        rotulo = stringResource(R.string.cliente_campo_telefone),
                        valor = cliente.telefone,
                    )
                    LinhaDado(
                        rotulo = stringResource(R.string.cliente_campo_email),
                        valor = cliente.email,
                    )
                    LinhaDado(
                        rotulo = stringResource(R.string.cliente_campo_endereco),
                        valor = cliente.endereco.formatar(),
                    )
                }
            }

            item {
                SecaoCliente(titulo = stringResource(R.string.cliente_secao_qualificacao)) {
                    val qualificacao = QualificacaoPorExtenso.montar(cliente)
                    Text(
                        text = qualificacao,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(
                        onClick = { clipboard.setText(AnnotatedString(qualificacao)) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(
                            text = stringResource(R.string.cliente_copiar_qualificacao),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.cliente_secao_processos),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = dimens.sectionGap),
                )
            }

            if (estado.numerosProcesso.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.cliente_sem_processos),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                }
            } else {
                items(estado.numerosProcesso) { numero ->
                    Text(
                        text = numero.formatarCnj(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { aoAbrirProcesso(numero) }
                            .padding(vertical = dimens.chipRowGap),
                    )
                }
            }
        }
    }
}

@Composable
private fun SecaoCliente(
    titulo: String,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors

    Column(
        verticalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
        content = {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            HorizontalDivider(color = colors.divider, thickness = dimens.borderWidth)
            conteudo()
        },
    )
}

@Composable
private fun LinhaDado(
    rotulo: String,
    valor: String?,
) {
    val colors = ObiterTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
        )
        Text(
            text = valor?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.processos_nao_informado),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun TipoPessoa.rotuloResId(): Int =
    when (this) {
        TipoPessoa.FISICA -> R.string.clientes_tipo_fisica
        TipoPessoa.JURIDICA -> R.string.clientes_tipo_juridica
    }

/** Endereço em uma linha, pulando os campos que o usuário não preencheu. */
private fun EnderecoCliente.formatar(): String? {
    val linha = listOfNotNull(
        listOfNotNull(logradouro, numero).joinToString(", ").takeIf { it.isNotBlank() },
        complemento?.takeIf { it.isNotBlank() },
        bairro?.takeIf { it.isNotBlank() },
        listOfNotNull(municipio, uf).joinToString("/").takeIf { it.isNotBlank() },
        cep?.takeIf { it.isNotBlank() },
    )
    return linha.joinToString(" — ").takeIf { it.isNotBlank() }
}
