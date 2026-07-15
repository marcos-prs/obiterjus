package com.obiterjus.presentation.autenticacao

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.SnackbarErroEffect
import com.obiterjus.presentation.componentes.ToggleObiter
import com.obiterjus.presentation.componentes.barras.BarraSuperiorSecundaria
import com.obiterjus.presentation.componentes.navegacao.NavegadorAbasSwipeable
import com.obiterjus.ui.theme.ObiterTheme
import java.text.Normalizer

@Composable
fun AutenticacaoScreen(
    viewModel: AutenticacaoViewModel,
    aoVoltar: () -> Unit,
    aoAutenticado: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dimens = ObiterTheme.dimens
    ObiterTheme.colors

    val acaoRetry = when {
        estado.modo == ModoAutenticacao.ENTRAR -> viewModel::aoEntrar
        estado.etapaCadastro == EtapaCadastro.VERIFICACAO -> viewModel::aoReiniciarValidacao
        else -> viewModel::aoAvancarCadastro
    }

    SnackbarErroEffect(
        mensagem = estado.mensagemErro,
        snackbarHostState = snackbarHostState,
        rotuloRetry = stringResource(R.string.acao_tentar_novamente),
        aoTentarNovamente = acaoRetry,
    )

    LaunchedEffect(estado.mensagemSucesso) {
        estado.mensagemSucesso ?: return@LaunchedEffect
        viewModel.aoConsumirMensagemSucesso()
        aoAutenticado()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            when (estado.modo) {
                ModoAutenticacao.ENTRAR -> BarraSuperiorSecundaria(
                    titulo = stringResource(R.string.autenticacao_title_acesso),
                    subtitulo = stringResource(R.string.autenticacao_subtitle_login),
                    onVoltar = aoVoltar,
                )
                ModoAutenticacao.CADASTRAR -> CabecalhoCadastro(
                    etapaAtual = estado.etapaCadastro,
                    onVoltar = {
                        if (estado.etapaCadastro == EtapaCadastro.CONTA) {
                            aoVoltar()
                        } else {
                            viewModel.aoVoltarEtapaOuModo()
                        }
                    },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        if (estado.modo == ModoAutenticacao.ENTRAR) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimens.screenMargin, vertical = dimens.cardGap),
                verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
            ) {
                ConteudoLogin(
                    estado = estado,
                    aoAlterarEmail = viewModel::aoAlterarEmail,
                    aoAlterarSenha = viewModel::aoAlterarSenha,
                    aoEntrar = viewModel::aoEntrar,
                    aoEnviarRedefinicaoSenha = viewModel::aoEnviarRedefinicaoSenha,
                    aoIrParaCadastro = viewModel::aoIrParaCadastro,
                )
            }
        } else {
            NavegadorAbasSwipeable(
                tabs = listOf(
                    stringResource(R.string.perfil_aba_conta),
                    stringResource(R.string.perfil_aba_oab),
                    stringResource(R.string.autenticacao_step_verificacao),
                    stringResource(R.string.perfil_aba_preferencias),
                    stringResource(R.string.autenticacao_step_resumo)
                ),
                initialTabIndex = estado.etapaCadastro.ordinal,
                onTabSelected = viewModel::aoSelecionarEtapa,
                modifier = Modifier.padding(paddingValues)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = dimens.screenMargin, vertical = dimens.cardGap),
                    verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
                ) {
                    when (EtapaCadastro.entries[page]) {
                        EtapaCadastro.CONTA -> EtapaConta(
                            estado = estado,
                            aoAlterarNome = viewModel::aoAlterarNome,
                            aoAlterarEmail = viewModel::aoAlterarEmail,
                            aoAlterarSenha = viewModel::aoAlterarSenha,
                            aoAlterarConfirmarSenha = viewModel::aoAlterarConfirmarSenha,
                            aoAlternarTermos = viewModel::aoAlternarTermos,
                            aoAlternarPrivacidade = viewModel::aoAlternarPrivacidade,
                            aoAvancar = viewModel::aoAvancarCadastro,
                        )

                        EtapaCadastro.OAB -> EtapaOab(
                            estado = estado,
                            aoAlterarUf = viewModel::aoAlterarUf,
                            aoAlterarNumeroOab = viewModel::aoAlterarNumeroOab,
                            aoAlterarTipoInscricao = viewModel::aoAlterarTipoInscricao,
                            aoAlterarNomeEscritorio = viewModel::aoAlterarNomeEscritorio,
                            aoAlternarAreaAtuacao = viewModel::aoAlternarAreaAtuacao,
                            aoAvancar = viewModel::aoAvancarCadastro,
                            aoVoltar = viewModel::aoVoltarEtapaOuModo,
                        )

                        EtapaCadastro.VERIFICACAO -> EtapaVerificacao(
                            estado = estado,
                            aoCorrigir = viewModel::aoVoltarEtapaOuModo,
                            aoContinuarSemValidacao = viewModel::aoContinuarSemValidacao,
                            aoAvancar = viewModel::aoAvancarCadastro,
                        )

                        EtapaCadastro.PREFERENCIAS -> EtapaPreferencias(
                            estado = estado,
                            aoAlterarJanelaBusca = viewModel::aoAlterarJanelaBusca,
                            aoAlternarNotificarPublicacoes = viewModel::aoAlternarNotificarPublicacoes,
                            aoAlternarNotificarPrazos = viewModel::aoAlternarNotificarPrazos,
                            aoAlternarNotificarMovimentacoes = viewModel::aoAlternarNotificarMovimentacoes,
                            aoAlternarTemaEscuro = viewModel::aoAlternarTemaEscuro,
                            aoAvancar = viewModel::aoAvancarCadastro,
                            aoVoltar = viewModel::aoVoltarEtapaOuModo,
                        )

                        EtapaCadastro.RESUMO -> EtapaResumo(
                            estado = estado,
                            aoFinalizar = viewModel::aoAvancarCadastro,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConteudoLogin(
    estado: EstadoAutenticacao,
    aoAlterarEmail: (String) -> Unit,
    aoAlterarSenha: (String) -> Unit,
    aoEntrar: () -> Unit,
    aoEnviarRedefinicaoSenha: () -> Unit,
    aoIrParaCadastro: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.cardGap)) {
        Text(
            text = stringResource(R.string.autenticacao_login_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = ObiterTheme.colors.textMuted,
        )

        CardLoginEmail(
            estado = estado,
            aoAlterarEmail = aoAlterarEmail,
            aoAlterarSenha = aoAlterarSenha,
            aoEntrar = aoEntrar,
            aoEnviarRedefinicaoSenha = aoEnviarRedefinicaoSenha,
        )

        TextButton(
            onClick = aoIrParaCadastro,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.autenticacao_action_signup))
        }
    }
}

@Composable
private fun CardLoginEmail(
    estado: EstadoAutenticacao,
    aoAlterarEmail: (String) -> Unit,
    aoAlterarSenha: (String) -> Unit,
    aoEntrar: () -> Unit,
    aoEnviarRedefinicaoSenha: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    var senhaVisivel by remember { mutableStateOf(false) }
    var manterConectado by remember { mutableStateOf(true) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(dimens.borderWidth, ObiterTheme.colors.border),
        shape = RoundedCornerShape(dimens.cardRadius),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = dimens.space4, vertical = dimens.space5),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
        ) {
            CampoLogin(
                label = stringResource(R.string.autenticacao_label_email_institucional),
                value = estado.email,
                onValueChange = aoAlterarEmail,
                leadingIcon = {
                    Icon(
                        imageVector = ObiterIcones.Email,
                        contentDescription = null,
                    )
                },
                placeholder = stringResource(R.string.autenticacao_placeholder_email),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )

            CampoLogin(
                label = stringResource(R.string.autenticacao_label_password),
                value = estado.senha,
                onValueChange = aoAlterarSenha,
                leadingIcon = {
                    Icon(
                        imageVector = ObiterIcones.Sigilo,
                        contentDescription = null,
                    )
                },
                placeholder = stringResource(R.string.autenticacao_placeholder_senha),
                visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false,
                ),
                keyboardActions = KeyboardActions(onDone = { aoEntrar() }),
                trailingIcon = {
                    IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                        Icon(
                            imageVector = if (senhaVisivel) {
                                ObiterIcones.VisibilidadeOculta
                            } else {
                                ObiterIcones.Visibilidade
                            },
                            contentDescription = stringResource(
                                if (senhaVisivel) {
                                    R.string.cd_ocultar_senha
                                } else {
                                    R.string.cd_mostrar_senha
                                },
                            ),
                        )
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = manterConectado,
                    onCheckedChange = { manterConectado = it },
                    enabled = !estado.carregando,
                )
                Text(
                    text = stringResource(R.string.autenticacao_manter_conectado),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = aoEnviarRedefinicaoSenha,
                    enabled = !estado.carregando,
                ) {
                    Text(stringResource(R.string.autenticacao_esqueci_senha))
                }
            }

            Button(
                onClick = aoEntrar,
                enabled = !estado.carregando,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (estado.carregando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = ObiterIcones.Login,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = if (estado.carregando) {
                        stringResource(R.string.autenticacao_action_loading)
                    } else {
                        stringResource(R.string.autenticacao_action_entrar_sistema)
                    },
                    modifier = Modifier.padding(start = dimens.space2),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }

            HorizontalDivider(color = ObiterTheme.colors.divider)

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(dimens.space2),
            ) {
                Icon(
                    imageVector = ObiterIcones.Seguranca,
                    contentDescription = null,
                    tint = ObiterTheme.colors.textMuted,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.autenticacao_rodape_seguranca),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObiterTheme.colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun CampoLogin(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: @Composable () -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val dimens = ObiterTheme.dimens
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ObiterTheme.colors.textMuted,
                )
            },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(dimens.cardRadius),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = ObiterTheme.colors.border,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = ObiterTheme.colors.textMuted,
                focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedTrailingIconColor = ObiterTheme.colors.textMuted,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun CabecalhoCadastro(
    etapaAtual: EtapaCadastro,
    onVoltar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val etapas = EtapaCadastro.entries
    Surface(
        color = colors.topAppBarBackground,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = dimens.topAppBarHeight)
                .padding(
                    horizontal = dimens.topAppBarPaddingH,
                    vertical = dimens.cardPaddingV,
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVoltar) {
                    Icon(
                        imageVector = ObiterIcones.Voltar,
                        contentDescription = stringResource(R.string.cd_voltar),
                        tint = colors.onTopAppBar.copy(alpha = 0.60f),
                        modifier = Modifier.size(dimens.iconBackSize),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.autenticacao_title_cadastro),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onTopAppBar,
                    )
                    Text(
                        text = stringResource(R.string.autenticacao_subtitle_cadastro, etapaAtual.ordinal + 1, etapas.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onTopAppBar.copy(alpha = 0.60f),
                    )
                }
            }
            SpacerProgressBar(etapaAtual = etapaAtual)
        }
    }
}

@Composable
private fun SpacerProgressBar(etapaAtual: EtapaCadastro) {
    val etapas = EtapaCadastro.entries
    val corAtiva by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "progressBar",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        etapas.forEachIndexed { index, etapa ->
            val active = index <= etapaAtual.ordinal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .size(height = 4.dp, width = 0.dp)
                    .background(
                        if (active) corAtiva else ObiterTheme.colors.divider,
                        RoundedCornerShape(99.dp),
                    ),
            )
        }
    }
}

@Composable
private fun EtapaConta(
    estado: EstadoAutenticacao,
    aoAlterarNome: (String) -> Unit,
    aoAlterarEmail: (String) -> Unit,
    aoAlterarSenha: (String) -> Unit,
    aoAlterarConfirmarSenha: (String) -> Unit,
    aoAlternarTermos: (Boolean) -> Unit,
    aoAlternarPrivacidade: (Boolean) -> Unit,
    aoAvancar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space2)) {
        Text(stringResource(R.string.autenticacao_step_dados_acesso), style = MaterialTheme.typography.headlineMedium)
        CampoTexto(stringResource(R.string.autenticacao_label_nome), estado.nomeCompleto, aoAlterarNome)
        CampoTexto(
            label = stringResource(R.string.autenticacao_label_email),
            value = estado.email,
            onValueChange = aoAlterarEmail,
            keyboardType = KeyboardType.Email,
        )
        CampoTexto(
            label = stringResource(R.string.autenticacao_label_password),
            value = estado.senha,
            onValueChange = aoAlterarSenha,
            oculto = true,
            keyboardType = KeyboardType.Password,
        )
        CampoTexto(
            label = stringResource(R.string.autenticacao_label_confirm_password),
            value = estado.confirmarSenha,
            onValueChange = aoAlterarConfirmarSenha,
            oculto = true,
            keyboardType = KeyboardType.Password,
        )

        ToggleLinha(
            titulo = stringResource(R.string.autenticacao_toggle_termos),
            subtitulo = stringResource(R.string.autenticacao_toggle_termos_sub),
            ativo = estado.aceitarTermos,
            aoAlternar = aoAlternarTermos,
        )
        ToggleLinha(
            titulo = stringResource(R.string.autenticacao_toggle_privacidade),
            subtitulo = stringResource(R.string.autenticacao_toggle_privacidade_sub),
            ativo = estado.aceitarPrivacidade,
            aoAlternar = aoAlternarPrivacidade,
        )

        Button(
            onClick = aoAvancar,
            enabled = estado.contaValida,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.autenticacao_action_continue))
        }
    }
}

@Composable
private fun EtapaOab(
    estado: EstadoAutenticacao,
    aoAlterarUf: (String) -> Unit,
    aoAlterarNumeroOab: (String) -> Unit,
    aoAlterarTipoInscricao: (TipoInscricaoCadastro) -> Unit,
    aoAlterarNomeEscritorio: (String) -> Unit,
    aoAlternarAreaAtuacao: (String) -> Unit,
    aoAvancar: () -> Unit,
    aoVoltar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val areas = listOf(
        R.string.autenticacao_area_civel,
        R.string.autenticacao_area_familia,
        R.string.autenticacao_area_trabalhista,
        R.string.autenticacao_area_criminal,
        R.string.autenticacao_area_tributario,
    )
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space2)) {
        Text(stringResource(R.string.autenticacao_step_dados_oab), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(R.string.autenticacao_oab_hint),
            style = MaterialTheme.typography.bodySmall,
            color = ObiterTheme.colors.textMuted,
        )
        Surface(
            color = ObiterTheme.colors.warningPale,
            shape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = dimens.alertStripRightRadius,
                bottomEnd = dimens.alertStripRightRadius,
                bottomStart = 0.dp,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = colors.accent,
                        topLeft = Offset.Zero,
                        size = Size(dimens.alertStripBorder.toPx(), size.height),
                    )
                },
        ) {
            Column(modifier = Modifier.padding(
                start = dimens.alertStripBorder + 8.dp,
                end = 8.dp,
                top = dimens.sectionGap,
                bottom = dimens.sectionGap,
            )) {
                Text(
                    text = stringResource(R.string.autenticacao_oab_para_que_serve_titulo),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = colors.warning,
                )
                Text(
                    text = stringResource(R.string.autenticacao_oab_para_que_serve_texto),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.warning,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space2)) {
            CampoEstadoOab(
                uf = estado.uf,
                aoAlterarUf = aoAlterarUf,
                modifier = Modifier.weight(0.45f),
            )
            CampoTexto(
                label = stringResource(R.string.autenticacao_label_numero_oab),
                value = estado.numeroOab,
                onValueChange = aoAlterarNumeroOab,
                modifier = Modifier.weight(0.55f),
                keyboardType = KeyboardType.Number,
            )
        }
        Text(stringResource(R.string.autenticacao_label_tipo_inscricao), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space1), modifier = Modifier.fillMaxWidth()) {
            TipoInscricaoCadastro.entries.forEach { tipo ->
                val ativo = estado.tipoInscricao == tipo
                val containerColor by animateColorAsState(
                    targetValue = if (ativo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    animationSpec = tween(100, easing = FastOutSlowInEasing),
                    label = "tipoInscricao",
                )
                Surface(
                    color = containerColor,
                    border = BorderStroke(dimens.borderWidth, ObiterTheme.colors.border),
                    shape = RoundedCornerShape(dimens.chipRadius),
                    modifier = Modifier.weight(1f),
                    onClick = { aoAlterarTipoInscricao(tipo) },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = when (tipo) {
                                TipoInscricaoCadastro.ADVOGADO -> stringResource(R.string.autenticacao_tipo_advogado)
                                TipoInscricaoCadastro.ESTAGIARIO -> stringResource(R.string.autenticacao_tipo_estagiario)
                                TipoInscricaoCadastro.SOCIO -> stringResource(R.string.autenticacao_tipo_socio)
                            },
                            color = if (ativo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        CampoTexto(stringResource(R.string.autenticacao_label_nome_escritorio), estado.nomeEscritorio, aoAlterarNomeEscritorio)
        Text(stringResource(R.string.autenticacao_label_areas_atuacao), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            areas.forEach { areaRes ->
                val area = stringResource(areaRes)
                val ativo = area in estado.areasAtuacao
                val bg by animateColorAsState(
                    targetValue = if (ativo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    animationSpec = tween(100, easing = FastOutSlowInEasing),
                    label = "areaAtuacao",
                )
                Surface(
                    color = bg,
                    border = BorderStroke(dimens.borderWidth, ObiterTheme.colors.border),
                    shape = RoundedCornerShape(99.dp),
                    modifier = Modifier.weight(1f),
                    onClick = { aoAlternarAreaAtuacao(area) },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = area,
                            color = if (ativo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space2), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = aoVoltar, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cd_voltar)) }
            Button(onClick = aoAvancar, enabled = estado.oabValida, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.autenticacao_action_continue))
            }
        }
    }
}

@Composable
private fun EtapaVerificacao(
    estado: EstadoAutenticacao,
    aoCorrigir: () -> Unit,
    aoContinuarSemValidacao: () -> Unit,
    aoAvancar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val itens = listOf(
        estado.verificacaoDjen,
        estado.verificacaoOab,
        estado.verificacaoBusca,
        estado.verificacaoDataJud,
        estado.verificacaoIndexacao,
    )
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space2)) {
        Text(stringResource(R.string.autenticacao_step_verificacao), style = MaterialTheme.typography.headlineMedium)
        Surface(
            color = ObiterTheme.colors.warningPale,
            border = BorderStroke(dimens.borderWidth, ObiterTheme.colors.warning),
            shape = RoundedCornerShape(dimens.cardRadius),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.autenticacao_verificacao_aviso),
                color = ObiterTheme.colors.warning,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(dimens.cardPaddingH),
            )
        }

        itens.forEach { item ->
            LinhaVerificacao(item)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space2), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = aoCorrigir, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.autenticacao_action_fix_data)) }
            TextButton(onClick = aoContinuarSemValidacao, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.autenticacao_action_continue_without_validation)) }
        }
        Button(onClick = aoAvancar, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.autenticacao_action_continue))
        }
    }
}

@Composable
private fun EtapaPreferencias(
    estado: EstadoAutenticacao,
    aoAlterarJanelaBusca: (Int) -> Unit,
    aoAlternarNotificarPublicacoes: (Boolean) -> Unit,
    aoAlternarNotificarPrazos: (Boolean) -> Unit,
    aoAlternarNotificarMovimentacoes: (Boolean) -> Unit,
    aoAlternarTemaEscuro: (Boolean) -> Unit,
    aoAvancar: () -> Unit,
    aoVoltar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val rotuloDias = stringResource(R.string.autenticacao_dias)
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space2)) {
        Text(stringResource(R.string.autenticacao_step_preferencias), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.autenticacao_preferencias_sub), style = MaterialTheme.typography.bodySmall, color = ObiterTheme.colors.textMuted)
        ChipsNumericos(
            titulo = stringResource(R.string.autenticacao_label_janela_busca),
            selecionado = estado.janelaBuscaDias,
            opcoes = listOf(7, 30, 60, 90),
            rotulo = { "$it $rotuloDias" },
            onSelect = aoAlterarJanelaBusca,
        )
        ToggleLinha(
            titulo = stringResource(R.string.autenticacao_notificar_publicacoes),
            subtitulo = null,
            ativo = estado.notificarPublicacoes,
            aoAlternar = aoAlternarNotificarPublicacoes,
        )
        ToggleLinha(
            titulo = stringResource(R.string.autenticacao_notificar_prazos),
            subtitulo = null,
            ativo = estado.notificarPrazosUrgentes,
            aoAlternar = aoAlternarNotificarPrazos,
        )
        ToggleLinha(
            titulo = stringResource(R.string.autenticacao_notificar_movimentacoes),
            subtitulo = null,
            ativo = estado.notificarMovimentacoes,
            aoAlternar = aoAlternarNotificarMovimentacoes,
        )
        ToggleLinha(
            titulo = stringResource(R.string.autenticacao_tema_escuro),
            subtitulo = stringResource(R.string.autenticacao_tema_escuro_sub),
            ativo = estado.temaEscuro,
            aoAlternar = aoAlternarTemaEscuro,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space2), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = aoVoltar, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cd_voltar)) }
            Button(onClick = aoAvancar, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.autenticacao_action_continue)) }
        }
    }
}

@Composable
private fun EtapaResumo(
    estado: EstadoAutenticacao,
    aoFinalizar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space2)) {
        Text(stringResource(R.string.autenticacao_step_resumo), style = MaterialTheme.typography.headlineMedium)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(dimens.borderWidth, ObiterTheme.colors.border),
            shape = RoundedCornerShape(dimens.cardRadius),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(dimens.cardPaddingH), verticalArrangement = Arrangement.spacedBy(dimens.space1)) {
                LinhaResumo(stringResource(R.string.autenticacao_resumo_nome), estado.nomeCompleto)
                LinhaResumo(stringResource(R.string.autenticacao_resumo_email), estado.email)
                LinhaResumo(stringResource(R.string.autenticacao_resumo_oab), "OAB/${estado.uf} ${estado.numeroOab} · ${tipoLabel(estado.tipoInscricao)}")
                LinhaResumo(stringResource(R.string.autenticacao_resumo_areas), estado.areasAtuacao.joinToString(" · ").ifBlank { "—" })
                LinhaResumo(stringResource(R.string.autenticacao_resumo_busca), stringResource(R.string.autenticacao_resumo_dias, estado.janelaBuscaDias))
            }
        }
        Text(
            text = stringResource(R.string.autenticacao_resumo_aviso),
            style = MaterialTheme.typography.bodySmall,
            color = ObiterTheme.colors.textMuted,
        )
        Button(
            onClick = aoFinalizar,
            enabled = !estado.carregando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (estado.carregando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text(
                text = if (estado.carregando) {
                    stringResource(R.string.autenticacao_action_loading)
                } else {
                    stringResource(R.string.autenticacao_action_home)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoEstadoOab(
    uf: String,
    aoAlterarUf: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var consulta by remember(uf) { mutableStateOf(uf) }
    val estadosFiltrados = remember(consulta) {
        val termo = consulta.normalizarBusca()
        if (termo.isBlank()) {
            estadosBrasileiros
        } else {
            estadosBrasileiros.filter { estado ->
                estado.sigla.normalizarBusca().contains(termo) ||
                    estado.nome.normalizarBusca().contains(termo)
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = consulta,
            onValueChange = { novoValor ->
                consulta = novoValor
                expanded = true
                estadosBrasileiros.firstOrNull { estado ->
                    estado.sigla.equals(novoValor, ignoreCase = true) ||
                        estado.nome.normalizarBusca() == novoValor.normalizarBusca()
                }?.let { aoAlterarUf(it.sigla) }
            },
            label = { Text(stringResource(R.string.autenticacao_label_estado)) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                .fillMaxWidth(),
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            estadosFiltrados.forEach { estado ->
                DropdownMenuItem(
                    text = { Text("${estado.sigla} - ${estado.nome}") },
                    onClick = {
                        consulta = estado.sigla
                        aoAlterarUf(estado.sigla)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun CampoTexto(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    oculto: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        visualTransformation = if (oculto) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

@Composable
private fun ToggleLinha(
    titulo: String,
    subtitulo: String?,
    ativo: Boolean,
    aoAlternar: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium)
            if (!subtitulo.isNullOrBlank()) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = ObiterTheme.colors.textMuted,
                )
            }
        }
        ToggleObiter(ativo = ativo, aoAlternar = aoAlternar)
    }
}

@Composable
private fun ChipsNumericos(
    titulo: String,
    selecionado: Int,
    opcoes: List<Int>,
    rotulo: (Int) -> String,
    onSelect: (Int) -> Unit,
) {
    val dimens = ObiterTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space1)) {
        Text(titulo, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space1), modifier = Modifier.fillMaxWidth()) {
            opcoes.forEach { opcao ->
                val ativo = selecionado == opcao
                val bg by animateColorAsState(
                    targetValue = if (ativo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    animationSpec = tween(100, easing = FastOutSlowInEasing),
                    label = "chipNumerico",
                )
                Surface(
                    color = bg,
                    border = BorderStroke(dimens.borderWidth, ObiterTheme.colors.border),
                    shape = RoundedCornerShape(dimens.chipRadius),
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(opcao) },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = rotulo(opcao),
                            color = if (ativo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaVerificacao(item: ItemVerificacaoCadastro) {
    val cor by animateColorAsState(
        targetValue = when (item.status) {
            StatusVerificacao.SUCESSO -> ObiterTheme.colors.success
            StatusVerificacao.ERRO -> ObiterTheme.colors.danger
            StatusVerificacao.CARREGANDO -> MaterialTheme.colorScheme.primary
            StatusVerificacao.OPCIONAL -> ObiterTheme.colors.warning
            StatusVerificacao.PENDENTE -> ObiterTheme.colors.textMuted
        },
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "verificacaoCor",
    )
    val texto = when (item.status) {
        StatusVerificacao.SUCESSO, StatusVerificacao.CARREGANDO, StatusVerificacao.PENDENTE -> item.detalhe.ifBlank { item.titulo }
        StatusVerificacao.ERRO, StatusVerificacao.OPCIONAL -> item.detalhe.ifBlank { item.titulo }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        when (item.status) {
            StatusVerificacao.CARREGANDO -> CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = cor)
            else -> Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(cor, CircleShape),
            )
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = cor,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun LinhaResumo(rotulo: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, style = MaterialTheme.typography.bodySmall, color = ObiterTheme.colors.textMuted)
        Text(valor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun tipoLabel(tipo: TipoInscricaoCadastro): String =
    when (tipo) {
        TipoInscricaoCadastro.ADVOGADO -> "Advogado(a)"
        TipoInscricaoCadastro.ESTAGIARIO -> "Estagiário(a)"
        TipoInscricaoCadastro.SOCIO -> "Sócio(a)"
    }

private data class EstadoBrasileiro(
    val sigla: String,
    val nome: String,
)

private val estadosBrasileiros = listOf(
    EstadoBrasileiro("AC", "Acre"),
    EstadoBrasileiro("AL", "Alagoas"),
    EstadoBrasileiro("AP", "Amapá"),
    EstadoBrasileiro("AM", "Amazonas"),
    EstadoBrasileiro("BA", "Bahia"),
    EstadoBrasileiro("CE", "Ceará"),
    EstadoBrasileiro("DF", "Distrito Federal"),
    EstadoBrasileiro("ES", "Espírito Santo"),
    EstadoBrasileiro("GO", "Goiás"),
    EstadoBrasileiro("MA", "Maranhão"),
    EstadoBrasileiro("MT", "Mato Grosso"),
    EstadoBrasileiro("MS", "Mato Grosso do Sul"),
    EstadoBrasileiro("MG", "Minas Gerais"),
    EstadoBrasileiro("PA", "Pará"),
    EstadoBrasileiro("PB", "Paraíba"),
    EstadoBrasileiro("PR", "Paraná"),
    EstadoBrasileiro("PE", "Pernambuco"),
    EstadoBrasileiro("PI", "Piauí"),
    EstadoBrasileiro("RJ", "Rio de Janeiro"),
    EstadoBrasileiro("RN", "Rio Grande do Norte"),
    EstadoBrasileiro("RS", "Rio Grande do Sul"),
    EstadoBrasileiro("RO", "Rondônia"),
    EstadoBrasileiro("RR", "Roraima"),
    EstadoBrasileiro("SC", "Santa Catarina"),
    EstadoBrasileiro("SP", "São Paulo"),
    EstadoBrasileiro("SE", "Sergipe"),
    EstadoBrasileiro("TO", "Tocantins"),
)

private fun String.normalizarBusca(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .uppercase()
        .trim()

@Composable
private fun Surface(
    color: androidx.compose.ui.graphics.Color,
    border: BorderStroke,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (onClick == null) {
        Surface(
            color = color,
            border = border,
            shape = shape,
            modifier = modifier,
            content = content,
        )
    } else {
        Surface(
            color = color,
            border = border,
            shape = shape,
            modifier = modifier,
            onClick = onClick,
            content = content,
        )
    }
}
