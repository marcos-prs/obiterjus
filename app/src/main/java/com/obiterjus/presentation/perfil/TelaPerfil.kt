package com.obiterjus.presentation.perfil

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.presentation.componentes.SnackbarErroEffect
import com.obiterjus.presentation.componentes.ToggleObiter
import com.obiterjus.ui.theme.ObiterTheme
import com.obiterjus.ui.theme.TipoTema

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
    val menuContaAberto by viewModel.mostrarMenuConta.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors

    val nome = estado.nomeUsuario ?: stringResource(R.string.perfil_usuario_anonimo)
    val iniciais = iniciaisDoNome(nome)
    val subtituloPerfil = subtituloPerfil(
        uf = estado.uf,
        numeroOab = estado.oab,
        tipoInscricao = estado.tipoInscricao,
    )

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
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            if (estado.autenticado) {
                CabecalhoUsuario(
                    nome = nome,
                    subtitulo = subtituloPerfil,
                    iniciais = iniciais,
                    menuAberto = menuContaAberto,
                    aoAbrirMenu = viewModel::aoAbrirMenu,
                    aoFecharMenu = viewModel::aoFecharMenu,
                    aoEditarPerfil = aoEditarPerfil,
                    aoLogout = aoLogout,
                    modifier = Modifier.padding(
                        start = dimens.screenMargin,
                        end = dimens.screenMargin,
                        top = 0.dp,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                SecaoConfiguracao(
                    titulo = stringResource(R.string.perfil_grupo_monitoramento),
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                ) {
                    LinhaValor(
                        rotulo = stringResource(R.string.perfil_oab_rotulo),
                        valor = listOfNotNull(
                            estado.uf.takeIf(String::isNotBlank),
                            estado.oab.takeIf(String::isNotBlank),
                        ).joinToString(" ").ifBlank { "—" },
                    )
                    DividerInterno()
                    LinhaValor(
                        rotulo = stringResource(R.string.perfil_estado_uf),
                        valor = nomeEstadoUf(estado.uf),
                    )
                    DividerInterno()
                    LinhaValor(
                        rotulo = stringResource(R.string.perfil_intervalo_busca),
                        valor = stringResource(R.string.perfil_intervalo_dias, estado.intervaloBuscaDias),
                    )
                    DividerInterno()
                    LinhaToggle(
                        rotulo = stringResource(R.string.perfil_sincronizacao_automatica),
                        ativo = estado.sincronizacaoAutomatica,
                        aoAlternar = viewModel::aoAlternarSincronizacaoAutomatica,
                    )
                    DividerInterno()
                    LinhaToggleDescritivo(
                        rotulo = stringResource(R.string.perfil_apenas_por_nome),
                        descricao = stringResource(R.string.perfil_apenas_por_nome_descricao),
                        ativo = estado.apenasPorNome,
                        aoAlternar = viewModel::aoAlternarApenasPorNome,
                    )
                    DividerInterno()
                    LinhaValor(
                        rotulo = stringResource(R.string.perfil_frequencia),
                        valor = estado.frequenciaSincronizacao,
                    )
                }
                
                Spacer(modifier = Modifier.height(dimens.cardGap))

                SecaoConfiguracao(
                    titulo = stringResource(R.string.perfil_grupo_sincronizacao),
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                ) {
                    LinhaSincronizacao(
                        rotulo = stringResource(R.string.perfil_forcar_sincronizacao),
                        sincronizando = estado.sincronizando,
                        aoForcar = aoForcarSincronizacao,
                    )
                    DividerInterno()
                    LinhaAcao(
                        rotulo = stringResource(R.string.nav_auditoria),
                        rotuloBotao = stringResource(R.string.auditoria_action_ver_historico),
                        acao = aoAbrirAuditoria,
                        mostrarBotao = false,
                    )
                }

                Spacer(modifier = Modifier.height(dimens.cardGap))

                SecaoConfiguracao(
                    titulo = stringResource(R.string.perfil_grupo_notificacoes),
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                ) {
                    LinhaToggle(
                        rotulo = stringResource(R.string.perfil_notificar_publicacoes),
                        ativo = estado.notificarPublicacoes,
                        aoAlternar = viewModel::aoAlternarNotificarPublicacoes,
                    )
                    DividerInterno()
                    LinhaToggle(
                        rotulo = stringResource(R.string.perfil_notificar_prazos),
                        ativo = estado.notificarPrazosUrgentes,
                        aoAlternar = viewModel::aoAlternarNotificarPrazos,
                    )
                    DividerInterno()
                    LinhaToggle(
                        rotulo = stringResource(R.string.perfil_notificar_movimentacoes),
                        ativo = estado.notificarMovimentacoes,
                        aoAlternar = viewModel::aoAlternarNotificarMovimentacoes,
                    )
                }

                Spacer(modifier = Modifier.height(dimens.cardGap))

                SecaoConfiguracao(
                    titulo = stringResource(R.string.perfil_grupo_integracao),
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                ) {
                    LinhaValor(
                        rotulo = stringResource(R.string.perfil_fonte_principal),
                        valor = estado.fontePrincipal,
                    )
                    DividerInterno()
                    LinhaValor(
                        rotulo = stringResource(R.string.perfil_enriquecimento),
                        valor = estado.enriquecimento,
                    )
                }

                Spacer(modifier = Modifier.height(dimens.cardGap))
            } else {
                SecaoAcessoAnonimo(
                    aoEntrar = aoEntrar,
                    aoCriarConta = aoCriarConta,
                    modifier = Modifier.padding(
                        start = dimens.screenMargin,
                        end = dimens.screenMargin,
                        top = 0.dp,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(dimens.cardGap))

            SecaoPersonalizacao(
                temaAtual = estado.tema,
                aoMudarTema = viewModel::aoAlterarTema,
                modifier = Modifier.padding(horizontal = dimens.screenMargin)
            )

            Spacer(modifier = Modifier.height(dimens.screenMargin))
        }
    }
}


@Composable
private fun CabecalhoUsuario(
    nome: String,
    subtitulo: String,
    iniciais: String,
    menuAberto: Boolean,
    aoAbrirMenu: () -> Unit,
    aoFecharMenu: () -> Unit,
    aoEditarPerfil: () -> Unit,
    aoLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxAvatar(iniciais = iniciais)
        Column(
            modifier = Modifier
                .padding(start = dimens.space2)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = nome,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = ObiterTheme.colors.textMuted,
            )
        }
        Box {
            IconButton(onClick = aoAbrirMenu) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.cd_abrir_menu_perfil),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = menuAberto,
                onDismissRequest = aoFecharMenu,
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.perfil_editar_dados)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        aoFecharMenu()
                        aoEditarPerfil()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.perfil_logout)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        aoFecharMenu()
                        aoLogout()
                    },
                )
            }
        }
    }
}

@Composable
private fun BoxAvatar(iniciais: String) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(ObiterTheme.colors.primaryPale, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = iniciais,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SecaoConfiguracao(
    titulo: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = ObiterTheme.dimens
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
            ),
            color = ObiterTheme.colors.textMuted,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimens.settingsGroupRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(dimens.borderWidth, ObiterTheme.colors.border),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SecaoAcessoAnonimo(
    aoEntrar: () -> Unit,
    aoCriarConta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SecaoConfiguracao(
        titulo = stringResource(R.string.perfil_grupo_acesso),
        modifier = modifier,
    ) {
        LinhaAcao(
            rotulo = stringResource(R.string.perfil_entrar),
            acao = aoEntrar,
            mostrarBotao = true,
        )
        DividerInterno()
        LinhaAcao(
            rotulo = stringResource(R.string.perfil_criar_conta),
            acao = aoCriarConta,
            mostrarBotao = false,
        )
    }
}

@Composable
private fun LinhaValor(
    rotulo: String,
    valor: String,
) {
    val dimens = ObiterTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.cardPaddingH, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            color = ObiterTheme.colors.textMuted,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun LinhaToggle(
    rotulo: String,
    ativo: Boolean,
    aoAlternar: (Boolean) -> Unit,
) {
    val dimens = ObiterTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.cardPaddingH, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        ToggleObiter(ativo = ativo, aoAlternar = aoAlternar)
    }
}

@Composable
private fun LinhaToggleDescritivo(
    rotulo: String,
    descricao: String,
    ativo: Boolean,
    aoAlternar: (Boolean) -> Unit,
) {
    val dimens = ObiterTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.cardPaddingH, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rotulo,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = descricao,
                style = MaterialTheme.typography.bodySmall,
                color = ObiterTheme.colors.textMuted,
            )
        }
        Spacer(modifier = Modifier.size(dimens.space1))
        ToggleObiter(ativo = ativo, aoAlternar = aoAlternar)
    }
}

@Composable
private fun LinhaSincronizacao(
    rotulo: String,
    sincronizando: Boolean,
    aoForcar: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.cardPaddingH, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = aoForcar,
            enabled = !sincronizando,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (sincronizando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(text = rotulo)
            }
        }
    }
}

@Composable
private fun LinhaAcao(
    rotulo: String,
    acao: () -> Unit,
    mostrarBotao: Boolean,
    rotuloBotao: String = rotulo,
) {
    val dimens = ObiterTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.cardPaddingH, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (mostrarBotao) {
            Button(onClick = acao) {
                Text(text = rotuloBotao)
            }
        } else {
            TextButton(onClick = acao) {
                Text(text = rotuloBotao)
            }
        }
    }
}

@Composable
private fun DividerInterno() {
    HorizontalDivider(color = ObiterTheme.colors.divider)
}

@Composable
private fun SecaoPersonalizacao(
    temaAtual: TipoTema,
    aoMudarTema: (TipoTema) -> Unit,
    modifier: Modifier = Modifier,
) {
    SecaoConfiguracao(
        titulo = stringResource(R.string.perfil_grupo_personalizacao),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ObiterTheme.dimens.cardPaddingH),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.perfil_tema),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ObiterTheme.dimens.space1)
            ) {
                OpcaoTema(
                    texto = stringResource(R.string.perfil_tema_sistema),
                    selecionado = temaAtual == TipoTema.SISTEMA,
                    onClick = { aoMudarTema(TipoTema.SISTEMA) },
                    modifier = Modifier.weight(1f)
                )
                OpcaoTema(
                    texto = stringResource(R.string.perfil_tema_claro),
                    selecionado = temaAtual == TipoTema.CLARO,
                    onClick = { aoMudarTema(TipoTema.CLARO) },
                    modifier = Modifier.weight(1f)
                )
                OpcaoTema(
                    texto = stringResource(R.string.perfil_tema_escuro),
                    selecionado = temaAtual == TipoTema.ESCURO,
                    onClick = { aoMudarTema(TipoTema.ESCURO) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}


private data class UfNome(
    val sigla: String,
    val nome: String,
)

private val ufsBrasileiras = listOf(
    UfNome("AC", "Acre"),
    UfNome("AL", "Alagoas"),
    UfNome("AP", "Amapá"),
    UfNome("AM", "Amazonas"),
    UfNome("BA", "Bahia"),
    UfNome("CE", "Ceará"),
    UfNome("DF", "Distrito Federal"),
    UfNome("ES", "Espírito Santo"),
    UfNome("GO", "Goiás"),
    UfNome("MA", "Maranhão"),
    UfNome("MT", "Mato Grosso"),
    UfNome("MS", "Mato Grosso do Sul"),
    UfNome("MG", "Minas Gerais"),
    UfNome("PA", "Pará"),
    UfNome("PB", "Paraíba"),
    UfNome("PR", "Paraná"),
    UfNome("PE", "Pernambuco"),
    UfNome("PI", "Piauí"),
    UfNome("RJ", "Rio de Janeiro"),
    UfNome("RN", "Rio Grande do Norte"),
    UfNome("RS", "Rio Grande do Sul"),
    UfNome("RO", "Rondônia"),
    UfNome("RR", "Roraima"),
    UfNome("SC", "Santa Catarina"),
    UfNome("SP", "São Paulo"),
    UfNome("SE", "Sergipe"),
    UfNome("TO", "Tocantins"),
)

private fun nomeEstadoUf(uf: String): String =
    ufsBrasileiras.firstOrNull { it.sigla.equals(uf, ignoreCase = true) }?.nome
        ?: uf.ifBlank { "—" }

private fun iniciaisDoNome(nome: String): String {
    val partes = nome
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    val iniciais = partes
        .take(2)
        .joinToString(separator = "") { parte ->
            parte.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
        }

    return iniciais.ifBlank { "?" }
}

private fun subtituloPerfil(
    uf: String,
    numeroOab: String,
    tipoInscricao: String,
): String {
    val partes = mutableListOf<String>()
    val oab = listOfNotNull(
        uf.takeIf { it.isNotBlank() },
        numeroOab.takeIf { it.isNotBlank() },
    ).joinToString(" ")

    if (oab.isNotBlank()) {
        partes += "OAB/$oab"
    }

    tipoInscricaoLabel(tipoInscricao)?.let { partes += it }

    return partes.joinToString(" · ").ifBlank { "—" }
}

private fun tipoInscricaoLabel(valor: String): String? =
    when (valor.uppercase()) {
        "ADVOGADO" -> "Advogado"
        "ESTAGIARIO" -> "Estagiário"
        "SOCIO" -> "Sócio"
        else -> null
    }
