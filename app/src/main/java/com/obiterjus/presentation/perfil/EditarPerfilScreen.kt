package com.obiterjus.presentation.perfil

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.presentation.autenticacao.TipoInscricaoCadastro
import com.obiterjus.presentation.componentes.SnackbarErroEffect
import com.obiterjus.presentation.componentes.ToggleObiter
import com.obiterjus.presentation.componentes.barras.BarraSuperiorSecundaria
import com.obiterjus.presentation.componentes.navegacao.NavegadorAbasSwipeable
import com.obiterjus.ui.theme.ObiterTheme
import java.text.Normalizer

@Composable
fun EditarPerfilScreen(
    viewModel: EditarPerfilViewModel,
    aoVoltar: () -> Unit,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dimens = ObiterTheme.dimens

    SnackbarErroEffect(
        mensagem = estado.mensagemErro,
        snackbarHostState = snackbarHostState,
        rotuloRetry = stringResource(R.string.autenticacao_action_fix_data),
        aoTentarNovamente = { viewModel.aoConsumirMensagens() }
    )

    LaunchedEffect(estado.mensagemSucesso) {
        estado.mensagemSucesso?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.aoConsumirMensagens()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BarraSuperiorSecundaria(
                titulo = stringResource(R.string.perfil_editar_dados_topbar),
                onVoltar = aoVoltar
            )
        }
    ) { padding ->
        if (estado.carregando && estado.nomeCompleto.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            NavegadorAbasSwipeable(
                tabs = listOf(
                    stringResource(R.string.perfil_aba_conta),
                    stringResource(R.string.perfil_aba_oab),
                    stringResource(R.string.perfil_aba_preferencias),
                    stringResource(R.string.perfil_aba_seguranca),
                    stringResource(R.string.perfil_aba_resumo),
                ),
                modifier = Modifier.padding(padding)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(dimens.screenMargin),
                    verticalArrangement = Arrangement.spacedBy(dimens.space2)
                ) {
                    when (EtapaEditarPerfil.entries[page]) {
                        EtapaEditarPerfil.CONTA -> AbaConta(estado, viewModel)
                        EtapaEditarPerfil.OAB -> AbaOab(estado, viewModel)
                        EtapaEditarPerfil.PREFERENCIAS -> AbaPreferencias(estado, viewModel)
                        EtapaEditarPerfil.SEGURANCA -> AbaSeguranca(estado, viewModel)
                        EtapaEditarPerfil.RESUMO -> AbaResumo(estado, aoVoltar)
                    }
                }
            }
        }
    }
}

@Composable
private fun AbaConta(estado: EstadoEditarPerfil, viewModel: EditarPerfilViewModel) {
    Text(stringResource(R.string.perfil_dados_conta), style = MaterialTheme.typography.titleLarge)
    CampoTexto(
        label = stringResource(R.string.autenticacao_label_nome),
        value = estado.nomeCompleto,
        onValueChange = viewModel::aoAlterarNome
    )
    CampoTexto(
        label = stringResource(R.string.autenticacao_label_email),
        value = estado.email,
        onValueChange = {}, // E-mail não editável aqui por simplicidade/segurança Firebase
        enabled = false
    )
}

@Composable
private fun AbaOab(estado: EstadoEditarPerfil, viewModel: EditarPerfilViewModel) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val areas = listOf(
        R.string.autenticacao_area_civel,
        R.string.autenticacao_area_familia,
        R.string.autenticacao_area_trabalhista,
        R.string.autenticacao_area_criminal,
        R.string.autenticacao_area_tributario,
    )

    Text(stringResource(R.string.autenticacao_step_dados_oab), style = MaterialTheme.typography.titleLarge)

    Surface(
        color = colors.warningPale,
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
            aoAlterarUf = viewModel::aoAlterarUf,
            modifier = Modifier.weight(0.45f),
        )
        CampoTexto(
            label = stringResource(R.string.autenticacao_label_numero_oab),
            value = estado.numeroOab,
            onValueChange = viewModel::aoAlterarNumeroOab,
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
                onClick = { viewModel.aoAlterarTipoInscricao(tipo) },
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

    CampoTexto(stringResource(R.string.autenticacao_label_nome_escritorio), estado.nomeEscritorio, viewModel::aoAlterarNomeEscritorio)

    Text(stringResource(R.string.autenticacao_label_areas_atuacao), style = MaterialTheme.typography.labelLarge)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
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
                onClick = { viewModel.aoAlternarAreaAtuacao(area) },
            ) {
                Text(
                    text = area,
                    color = if (ativo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun AbaPreferencias(estado: EstadoEditarPerfil, viewModel: EditarPerfilViewModel) {
    val rotuloDias = stringResource(R.string.autenticacao_dias)

    Text(stringResource(R.string.autenticacao_step_preferencias), style = MaterialTheme.typography.titleLarge)

    ChipsNumericos(
        titulo = stringResource(R.string.autenticacao_label_janela_busca),
        selecionado = estado.janelaBuscaDias,
        opcoes = listOf(7, 30, 60, 90),
        rotulo = { "$it $rotuloDias" },
        onSelect = viewModel::aoAlterarJanelaBusca
    )

    Spacer(Modifier.height(8.dp))

    ToggleLinha(
        titulo = stringResource(R.string.autenticacao_notificar_publicacoes),
        subtitulo = null,
        ativo = estado.notificarPublicacoes,
        aoAlternar = viewModel::aoAlternarNotificarPublicacoes
    )
    ToggleLinha(
        titulo = stringResource(R.string.autenticacao_notificar_prazos),
        subtitulo = null,
        ativo = estado.notificarPrazosUrgentes,
        aoAlternar = viewModel::aoAlternarNotificarPrazos
    )
    ToggleLinha(
        titulo = stringResource(R.string.autenticacao_notificar_movimentacoes),
        subtitulo = null,
        ativo = estado.notificarMovimentacoes,
        aoAlternar = viewModel::aoAlternarNotificarMovimentacoes
    )
    ToggleLinha(
        titulo = stringResource(R.string.autenticacao_tema_escuro),
        subtitulo = stringResource(R.string.autenticacao_tema_escuro_sub),
        ativo = estado.temaEscuro,
        aoAlternar = viewModel::aoAlternarTemaEscuro
    )
}

@Composable
private fun AbaSeguranca(estado: EstadoEditarPerfil, viewModel: EditarPerfilViewModel) {
    Text(stringResource(R.string.perfil_aba_seguranca), style = MaterialTheme.typography.titleLarge)
    Text(
        text = stringResource(R.string.perfil_hint_seguranca),
        style = MaterialTheme.typography.bodySmall,
        color = ObiterTheme.colors.textMuted
    )

    CampoTexto(
        label = stringResource(R.string.perfil_label_senha_atual),
        value = estado.senhaAtual,
        onValueChange = viewModel::aoAlterarSenhaAtual,
        oculto = true,
        keyboardType = KeyboardType.Password
    )

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    CampoTexto(
        label = stringResource(R.string.perfil_label_nova_senha),
        value = estado.novaSenha,
        onValueChange = viewModel::aoAlterarNovaSenha,
        oculto = true,
        keyboardType = KeyboardType.Password
    )
    CampoTexto(
        label = stringResource(R.string.perfil_label_confirmar_nova_senha),
        value = estado.confirmarNovaSenha,
        onValueChange = viewModel::aoAlterarConfirmarNovaSenha,
        oculto = true,
        keyboardType = KeyboardType.Password
    )

    Button(
        onClick = viewModel::aoTrocarSenha,
        enabled = estado.senhaAtual.isNotBlank() && estado.novaSenhaValida && estado.novaSenhaConfere && !estado.carregando,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (estado.carregando) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(stringResource(R.string.perfil_action_trocar_senha))
        }
    }
}

@Composable
private fun AbaResumo(
    estado: EstadoEditarPerfil,
    aoConcluir: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val areas = estado.areasAtuacao.joinToString(", ").ifBlank { "—" }

    Column(verticalArrangement = Arrangement.spacedBy(dimens.space2)) {
        Text(stringResource(R.string.perfil_aba_resumo), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.perfil_resumo_aviso),
            style = MaterialTheme.typography.bodySmall,
            color = ObiterTheme.colors.textMuted,
        )

        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(dimens.borderWidth, ObiterTheme.colors.border),
            shape = RoundedCornerShape(dimens.settingsGroupRadius),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(dimens.cardPaddingH),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResumoLinha(stringResource(R.string.autenticacao_label_nome), estado.nomeCompleto.ifBlank { "—" })
                ResumoLinha(
                    stringResource(R.string.perfil_oab_rotulo),
                    listOfNotNull(estado.uf.takeIf(String::isNotBlank), estado.numeroOab.takeIf(String::isNotBlank)).joinToString(" ").ifBlank { "—" }
                )
                ResumoLinha(stringResource(R.string.autenticacao_label_nome_escritorio), estado.nomeEscritorio.ifBlank { "—" })
                ResumoLinha(stringResource(R.string.autenticacao_label_areas_atuacao), areas)
                ResumoLinha(
                    stringResource(R.string.autenticacao_label_janela_busca),
                    "${estado.janelaBuscaDias} dias"
                )
                ResumoLinha(
                    stringResource(R.string.autenticacao_notificar_publicacoes),
                    listOfNotNull(
                        if (estado.notificarPublicacoes) "publicações" else null,
                        if (estado.notificarPrazosUrgentes) "prazos" else null,
                        if (estado.notificarMovimentacoes) "movimentações" else null,
                    ).joinToString(", ").ifBlank { "desativados" }
                )
                ResumoLinha(stringResource(R.string.perfil_tema), if (estado.temaEscuro) "Escuro" else "Claro")
            }
        }

        Button(
            onClick = aoConcluir,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.perfil_action_concluir))
        }
    }
}

@Composable
private fun ResumoLinha(
    rotulo: String,
    valor: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodyMedium,
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

// UI Helpers (Redefinidos para evitar acoplamento com AutenticacaoScreen)

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
        if (termo.isBlank()) estadosBrasileiros
        else estadosBrasileiros.filter { it.sigla.normalizarBusca().contains(termo) || it.nome.normalizarBusca().contains(termo) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = consulta,
            onValueChange = {
                consulta = it
                expanded = true
                estadosBrasileiros.firstOrNull { est -> est.sigla.equals(it, ignoreCase = true) || est.nome.normalizarBusca() == it.normalizarBusca() }?.let { est -> aoAlterarUf(est.sigla) }
            },
            label = { Text(stringResource(R.string.autenticacao_label_estado)) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            estadosFiltrados.forEach { estado ->
                DropdownMenuItem(
                    text = { Text("${estado.sigla} - ${estado.nome}") },
                    onClick = {
                        consulta = estado.sigla
                        aoAlterarUf(estado.sigla)
                        expanded = false
                    }
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
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        enabled = enabled,
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
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = rotulo(opcao),
                            color = if (ativo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

private data class EstadoBrasileiro(val sigla: String, val nome: String)
private val estadosBrasileiros = listOf(
    EstadoBrasileiro("AC", "Acre"), EstadoBrasileiro("AL", "Alagoas"), EstadoBrasileiro("AP", "Amapá"),
    EstadoBrasileiro("AM", "Amazonas"), EstadoBrasileiro("BA", "Bahia"), EstadoBrasileiro("CE", "Ceará"),
    EstadoBrasileiro("DF", "Distrito Federal"), EstadoBrasileiro("ES", "Espírito Santo"), EstadoBrasileiro("GO", "Goiás"),
    EstadoBrasileiro("MA", "Maranhão"), EstadoBrasileiro("MT", "Mato Grosso"), EstadoBrasileiro("MS", "Mato Grosso do Sul"),
    EstadoBrasileiro("MG", "Minas Gerais"), EstadoBrasileiro("PA", "Pará"), EstadoBrasileiro("PB", "Paraíba"),
    EstadoBrasileiro("PR", "Paraná"), EstadoBrasileiro("PE", "Pernambuco"), EstadoBrasileiro("PI", "Piauí"),
    EstadoBrasileiro("RJ", "Rio de Janeiro"), EstadoBrasileiro("RN", "Rio Grande do Norte"), EstadoBrasileiro("RS", "Rio Grande do Sul"),
    EstadoBrasileiro("RO", "Rondônia"), EstadoBrasileiro("RR", "Roraima"), EstadoBrasileiro("SC", "Santa Catarina"),
    EstadoBrasileiro("SP", "São Paulo"), EstadoBrasileiro("SE", "Sergipe"), EstadoBrasileiro("TO", "Tocantins"),
)

private fun String.normalizarBusca(): String = Normalizer.normalize(this, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "").uppercase().trim()
