package com.obiterjus.presentation.perfil

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.BuildConfig
import com.obiterjus.R
import com.obiterjus.presentation.componentes.SnackbarErroEffect
import com.obiterjus.presentation.componentes.ToggleObiter
import com.obiterjus.ui.theme.ObiterTheme
import com.obiterjus.ui.theme.TipoTema

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPerfil(
    viewModel: ModeloPerfil,
    aoAbrirAuditoria: () -> Unit,
    aoForcarSincronizacao: () -> Unit,
    aoLogout: () -> Unit,
    aoEntrar: () -> Unit,
    aoCriarConta: () -> Unit,
    aoEditarPerfil: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensagemSucesso by viewModel.mensagemSucesso.collectAsStateWithLifecycle()
    val mensagemErro by viewModel.mensagemErro.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val nome = estado.nomeUsuario ?: stringResource(R.string.perfil_usuario_anonimo)
    val oabHeader = if (estado.oab.isBlank() || estado.uf.isBlank()) {
        stringResource(R.string.perfil_oab_nao_cadastrada)
    } else {
        stringResource(R.string.perfil_oab_formato, estado.uf, estado.oab)
    }
    val iniciais = nome.trim().take(2).uppercase()
    var mostrarMenuConta by remember { mutableStateOf(false) }

    SnackbarErroEffect(
        mensagem = mensagemErro,
        snackbarHostState = snackbarHostState,
        rotuloRetry = stringResource(R.string.acao_tentar_novamente),
        aoTentarNovamente = aoForcarSincronizacao,
    )

    LaunchedEffect(mensagemSucesso) {
        val mensagem = mensagemSucesso ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(mensagem)
        viewModel.aoConsumirMensagemSucesso()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(dimens.screenMargin),
            verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
        ) {
            HeaderPerfil(
                nome = nome,
                oab = oabHeader,
                iniciais = iniciais,
                autenticado = estado.autenticado,
                aoAbrirMenu = { mostrarMenuConta = true },
            )

            if (!estado.autenticado) {
                CardAcessoAnonimo(
                    aoEntrar = aoEntrar,
                    aoCriarConta = aoCriarConta,
                )
            } else {
                CardDadosUsuario(
                    estado = estado,
                    viewModel = viewModel,
                )
            }

            CardSincronizacao(
                estado = estado,
                aoForcarSincronizacao = aoForcarSincronizacao,
            )

            GrupoPerfil(titulo = stringResource(R.string.perfil_grupo_notificacoes)) {
                LinhaToggle(
                    texto = stringResource(R.string.perfil_notificar_publicacoes),
                    ativo = estado.notificarPublicacoes,
                    aoAlternar = viewModel::aoAlternarNotificarPublicacoes,
                )
                LinhaToggle(
                    texto = stringResource(R.string.perfil_notificar_prazos),
                    ativo = estado.notificarPrazosUrgentes,
                    aoAlternar = viewModel::aoAlternarNotificarPrazos,
                )
                LinhaToggle(
                    texto = stringResource(R.string.perfil_notificar_movimentacoes),
                    ativo = estado.notificarMovimentacoes,
                    aoAlternar = viewModel::aoAlternarNotificarMovimentacoes,
                )
            }

            GrupoPerfil(titulo = stringResource(R.string.perfil_grupo_integracao)) {
                LinhaInfo(
                    rotulo = stringResource(R.string.perfil_fonte_principal),
                    valor = estado.fontePrincipal,
                )
                LinhaInfo(
                    rotulo = stringResource(R.string.perfil_enriquecimento),
                    valor = estado.enriquecimento,
                )
            }

            GrupoPerfil(titulo = stringResource(R.string.perfil_grupo_personalizacao)) {
                Text(
                    text = stringResource(R.string.perfil_tema),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.space1),
                ) {
                    OpcaoTema(
                        texto = stringResource(R.string.perfil_tema_sistema),
                        selecionado = estado.tema == TipoTema.SISTEMA,
                        onClick = { viewModel.aoAlterarTema(TipoTema.SISTEMA) },
                        modifier = Modifier.weight(1f),
                    )
                    OpcaoTema(
                        texto = stringResource(R.string.perfil_tema_claro),
                        selecionado = estado.tema == TipoTema.CLARO,
                        onClick = { viewModel.aoAlterarTema(TipoTema.CLARO) },
                        modifier = Modifier.weight(1f),
                    )
                    OpcaoTema(
                        texto = stringResource(R.string.perfil_tema_escuro),
                        selecionado = estado.tema == TipoTema.ESCURO,
                        onClick = { viewModel.aoAlterarTema(TipoTema.ESCURO) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            HorizontalDivider(color = colors.divider)

            Text(
                text = stringResource(R.string.perfil_versao, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
            Button(onClick = aoAbrirAuditoria, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.perfil_auditoria))
            }
        }
    }

    if (mostrarMenuConta) {
        BottomSheetMenuConta(
            aoFechar = { mostrarMenuConta = false },
            aoEditar = {
                mostrarMenuConta = false
                aoEditarPerfil()
            },
            aoLogout = {
                mostrarMenuConta = false
                aoLogout()
            },
        )
    }
}

@Composable
private fun GrupoPerfil(
    titulo: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    Surface(
        shape = RoundedCornerShape(dimens.settingsGroupRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(dimens.borderWidth, colors.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(dimens.cardPaddingH),
            verticalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
            )
            content()
        }
    }
}

@Composable
private fun LinhaInfo(rotulo: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = rotulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(text = valor, style = MaterialTheme.typography.bodyMedium, color = ObiterTheme.colors.textMuted)
    }
}

@Composable
private fun LinhaToggle(
    texto: String,
    ativo: Boolean,
    aoAlternar: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        ToggleObiter(ativo = ativo, aoAlternar = aoAlternar)
    }
}

@Composable
private fun BoxAvatar(iniciais: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(ObiterTheme.colors.primaryPale, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = iniciais,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun OpcaoTema(
    texto: String,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val backgroundColor = if (selecionado) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (selecionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selecionado) Color.Transparent else colors.border

    Surface(
        onClick = onClick,
        selected = selecionado,
        shape = RoundedCornerShape(dimens.settingsGroupRadius),
        color = backgroundColor,
        border = if (selecionado) null else BorderStroke(dimens.borderWidth, borderColor),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.padding(vertical = dimens.space1),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun HeaderPerfil(
    nome: String,
    oab: String,
    iniciais: String,
    autenticado: Boolean,
    aoAbrirMenu: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BoxAvatar(iniciais = iniciais)
            Column(
                modifier = Modifier.padding(start = dimens.space2),
                verticalArrangement = Arrangement.spacedBy(dimens.space1)
            ) {
                Text(
                    text = nome,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = oab,
                    style = MaterialTheme.typography.bodySmall,
                    color = ObiterTheme.colors.textMuted,
                )
            }
        }

        if (autenticado) {
            IconButton(
                onClick = aoAbrirMenu,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.perfil_menu_conta),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun CardAcessoAnonimo(
    aoEntrar: () -> Unit,
    aoCriarConta: () -> Unit,
) {
    GrupoPerfil(titulo = stringResource(R.string.perfil_grupo_acesso)) {
        Button(
            onClick = aoEntrar,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.perfil_entrar))
        }
        TextButton(
            onClick = aoCriarConta,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.perfil_criar_conta))
        }
    }
}

@Composable
private fun CardDadosUsuario(
    estado: EstadoPerfil,
    viewModel: ModeloPerfil,
) {
    GrupoPerfil(titulo = stringResource(R.string.perfil_dados_conta)) {
        LinhaInfo(
            rotulo = stringResource(R.string.autenticacao_label_email),
            valor = estado.email ?: stringResource(R.string.perfil_nao_autenticado),
        )
        LinhaInfo(
            rotulo = stringResource(R.string.perfil_status_nuvem),
            valor = estado.statusSincronizacaoNuvem
                ?: stringResource(R.string.perfil_status_nuvem_anonimo),
        )
        HorizontalDivider(color = ObiterTheme.colors.divider)
        LinhaInfo(
            rotulo = stringResource(R.string.perfil_intervalo_busca),
            valor = stringResource(R.string.perfil_intervalo_dias, estado.intervaloBuscaDias),
        )
        LinhaToggle(
            texto = stringResource(R.string.perfil_sincronizacao_automatica),
            ativo = estado.sincronizacaoAutomatica,
            aoAlternar = viewModel::aoAlternarSincronizacaoAutomatica,
        )
        LinhaInfo(
            rotulo = stringResource(R.string.perfil_frequencia),
            valor = estado.frequenciaSincronizacao,
        )
    }
}

@Composable
private fun CardSincronizacao(
    estado: EstadoPerfil,
    aoForcarSincronizacao: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    GrupoPerfil(titulo = stringResource(R.string.perfil_grupo_sincronizacao)) {
        Button(
            onClick = aoForcarSincronizacao,
            enabled = !estado.sincronizando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (estado.sincronizando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimens.iconSearchSize),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = dimens.borderWidth,
                )
                Text(
                    text = stringResource(R.string.perfil_forcar_sincronizacao_loading),
                    modifier = Modifier.padding(start = dimens.space1),
                )
            } else {
                Text(stringResource(R.string.perfil_forcar_sincronizacao))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetMenuConta(
    aoFechar: () -> Unit,
    aoEditar: () -> Unit,
    aoLogout: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = aoFechar,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = ObiterTheme.dimens.screenMargin),
        ) {
            Text(
                text = stringResource(R.string.perfil_menu_conta),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = ObiterTheme.dimens.cardPaddingH),
            )
            Surface(
                onClick = aoEditar,
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = ObiterTheme.dimens.cardPaddingH,
                            vertical = ObiterTheme.dimens.space2,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.perfil_editar_dados),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = ObiterTheme.dimens.space2),
                    )
                }
            }
            Surface(
                onClick = aoLogout,
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = ObiterTheme.dimens.cardPaddingH,
                            vertical = ObiterTheme.dimens.space2,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = ObiterTheme.colors.danger,
                    )
                    Text(
                        text = stringResource(R.string.perfil_logout),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ObiterTheme.colors.danger,
                        modifier = Modifier.padding(start = ObiterTheme.dimens.space2),
                    )
                }
            }
        }
    }
}
