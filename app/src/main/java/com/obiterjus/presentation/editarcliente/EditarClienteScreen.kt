package com.obiterjus.presentation.editarcliente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.domain.model.TipoPessoa
import com.obiterjus.presentation.componentes.barras.BarraSuperiorSecundaria
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun EditarClienteScreen(
    viewModel: EditarClienteViewModel,
    clienteId: String,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    titulo: String = stringResource(R.string.editar_cliente_titulo),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dimens = ObiterTheme.dimens
    val snackbarHostState = remember { SnackbarHostState() }
    val msgSalvo = stringResource(R.string.editar_cliente_salvo)

    LaunchedEffect(clienteId) { viewModel.aoCarregar(clienteId) }
    LaunchedEffect(estado.salvo) {
        if (estado.salvo) {
            snackbarHostState.showSnackbar(msgSalvo)
            viewModel.aoDescartarSalvo()
            onVoltar()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BarraSuperiorSecundaria(
                titulo = titulo,
                onVoltar = onVoltar,
            )
        },
    ) { padding ->
        // Column e não LazyColumn: o "próximo" do teclado percorre a árvore de
        // foco, e o que a LazyColumn ainda não compôs não existe para ela — a
        // travessia morreria no último campo visível. São ~20 campos fixos, e
        // virtualizar não compensa perder isso.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(dimens.screenMargin),
            verticalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            SecaoLabel(stringResource(R.string.cliente_secao_identificacao))

            Row(horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap)) {
                TipoPessoa.entries.forEach { tipo ->
                    FilterChip(
                        selected = estado.tipoPessoa == tipo,
                        onClick = { viewModel.aoAlterarTipoPessoa(tipo) },
                        label = { Text(stringResource(tipo.rotuloResId())) },
                        enabled = !estado.salvando,
                    )
                }
            }

            CampoCliente(
                rotulo = stringResource(
                    if (estado.tipoPessoa == TipoPessoa.JURIDICA) R.string.editar_cliente_label_razao_social
                    else R.string.cliente_campo_nome,
                ),
                valor = estado.nome,
                aoAlterar = viewModel::aoAlterarNome,
                enabled = !estado.salvando,
                erro = estado.erroNomeVazio,
                mensagemErro = stringResource(R.string.editar_cliente_erro_nome),
                capitalizacao = KeyboardCapitalization.Words,
            )
            CampoCliente(
                rotulo = stringResource(R.string.cliente_campo_documento),
                valor = estado.documento,
                aoAlterar = viewModel::aoAlterarDocumento,
                enabled = !estado.salvando,
                keyboardType = KeyboardType.Number,
            )

            // Numa PJ a qualificação pessoal pertence ao representante, não à
            // empresa — repetir estado civil e profissão aqui só confundiria.
            if (estado.tipoPessoa == TipoPessoa.FISICA) {
                CampoCliente(stringResource(R.string.cliente_campo_nacionalidade), estado.nacionalidade, viewModel::aoAlterarNacionalidade, !estado.salvando)
                CampoCliente(stringResource(R.string.cliente_campo_estado_civil), estado.estadoCivil, viewModel::aoAlterarEstadoCivil, !estado.salvando)
                CampoCliente(stringResource(R.string.cliente_campo_profissao), estado.profissao, viewModel::aoAlterarProfissao, !estado.salvando)
            }

            if (estado.tipoPessoa == TipoPessoa.JURIDICA) {
                SecaoLabel(stringResource(R.string.cliente_secao_representante))
                Text(
                    text = stringResource(R.string.editar_cliente_representante_ajuda),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObiterTheme.colors.textMuted,
                )
                CampoCliente(
                    rotulo = stringResource(R.string.cliente_campo_nome),
                    valor = estado.repNome,
                    aoAlterar = viewModel::aoAlterarRepNome,
                    enabled = !estado.salvando,
                    capitalizacao = KeyboardCapitalization.Words,
                )
                CampoCliente(stringResource(R.string.cliente_campo_cargo), estado.repCargo, viewModel::aoAlterarRepCargo, !estado.salvando)
                CampoCliente(
                    rotulo = stringResource(R.string.cliente_campo_documento),
                    valor = estado.repDocumento,
                    aoAlterar = viewModel::aoAlterarRepDocumento,
                    enabled = !estado.salvando,
                    keyboardType = KeyboardType.Number,
                )
                CampoCliente(stringResource(R.string.cliente_campo_nacionalidade), estado.repNacionalidade, viewModel::aoAlterarRepNacionalidade, !estado.salvando)
                CampoCliente(stringResource(R.string.cliente_campo_estado_civil), estado.repEstadoCivil, viewModel::aoAlterarRepEstadoCivil, !estado.salvando)
                CampoCliente(stringResource(R.string.cliente_campo_profissao), estado.repProfissao, viewModel::aoAlterarRepProfissao, !estado.salvando)
            }

            SecaoLabel(stringResource(R.string.editar_cliente_secao_endereco))
            CampoCliente(
                rotulo = stringResource(R.string.participante_label_logradouro),
                valor = estado.logradouro,
                aoAlterar = viewModel::aoAlterarLogradouro,
                enabled = !estado.salvando,
                capitalizacao = KeyboardCapitalization.Words,
            )
            CampoCliente(stringResource(R.string.participante_label_numero_endereco), estado.numero, viewModel::aoAlterarNumero, !estado.salvando)
            CampoCliente(stringResource(R.string.editar_cliente_label_complemento), estado.complemento, viewModel::aoAlterarComplemento, !estado.salvando)
            CampoCliente(
                rotulo = stringResource(R.string.editar_cliente_label_bairro),
                valor = estado.bairro,
                aoAlterar = viewModel::aoAlterarBairro,
                enabled = !estado.salvando,
                capitalizacao = KeyboardCapitalization.Words,
            )
            CampoCliente(
                rotulo = stringResource(R.string.editar_cliente_label_municipio),
                valor = estado.municipio,
                aoAlterar = viewModel::aoAlterarMunicipio,
                enabled = !estado.salvando,
                capitalizacao = KeyboardCapitalization.Words,
            )
            // UF é sigla: entra em caixa alta sem o usuário ter que segurar shift.
            CampoCliente(
                rotulo = stringResource(R.string.editar_cliente_label_uf),
                valor = estado.uf,
                aoAlterar = viewModel::aoAlterarUf,
                enabled = !estado.salvando,
                capitalizacao = KeyboardCapitalization.Characters,
            )
            CampoCliente(
                rotulo = stringResource(R.string.editar_cliente_label_cep),
                valor = estado.cep,
                aoAlterar = viewModel::aoAlterarCep,
                enabled = !estado.salvando,
                keyboardType = KeyboardType.Number,
            )

            SecaoLabel(stringResource(R.string.cliente_secao_contato))
            CampoCliente(
                rotulo = stringResource(R.string.cliente_campo_telefone),
                valor = estado.telefone,
                aoAlterar = viewModel::aoAlterarTelefone,
                enabled = !estado.salvando,
                keyboardType = KeyboardType.Phone,
            )
            CampoCliente(
                rotulo = stringResource(R.string.cliente_campo_email),
                valor = estado.email,
                aoAlterar = viewModel::aoAlterarEmail,
                enabled = !estado.salvando,
                keyboardType = KeyboardType.Email,
                // E-mail não leva maiúscula automática — "Fulano@..." é erro comum.
                capitalizacao = KeyboardCapitalization.None,
            )
            // Último campo: fecha o teclado em vez de oferecer um "próximo" que
            // não tem para onde ir.
            CampoCliente(
                rotulo = stringResource(R.string.editar_cliente_label_observacoes),
                valor = estado.observacoes,
                aoAlterar = viewModel::aoAlterarObservacoes,
                enabled = !estado.salvando,
                imeAction = ImeAction.Done,
            )

            Button(
                onClick = viewModel::aoSalvar,
                enabled = !estado.salvando,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.sectionGap),
            ) {
                Text(stringResource(R.string.editar_cliente_salvar))
            }
        }
    }
}

@Composable
private fun SecaoLabel(titulo: String) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    Column(
        modifier = Modifier.padding(top = dimens.sectionGap),
        verticalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        HorizontalDivider(color = colors.divider, thickness = dimens.borderWidth)
    }
}

@Composable
private fun CampoCliente(
    rotulo: String,
    valor: String,
    aoAlterar: (String) -> Unit,
    enabled: Boolean,
    erro: Boolean = false,
    mensagemErro: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalizacao: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val focusManager = LocalFocusManager.current

    Column {
        OutlinedTextField(
            value = valor,
            onValueChange = aoAlterar,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(rotulo, style = MaterialTheme.typography.labelSmall) },
            singleLine = true,
            isError = erro,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = capitalizacao,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) },
                onDone = { focusManager.clearFocus() },
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(dimens.searchBarRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = colors.border,
            ),
            enabled = enabled,
        )
        if (erro && mensagemErro != null) {
            Text(
                text = mensagemErro,
                style = MaterialTheme.typography.labelSmall,
                color = colors.danger,
                modifier = Modifier.padding(start = dimens.space1),
            )
        }
    }
}

private fun TipoPessoa.rotuloResId(): Int =
    when (this) {
        TipoPessoa.FISICA -> R.string.clientes_tipo_fisica
        TipoPessoa.JURIDICA -> R.string.clientes_tipo_juridica
    }
