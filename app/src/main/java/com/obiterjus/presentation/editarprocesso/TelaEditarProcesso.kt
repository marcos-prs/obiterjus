package com.obiterjus.presentation.editarprocesso

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.presentation.componentes.CarregandoCentral
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.ui.theme.ObiterDimens
import com.obiterjus.ui.theme.ObiterExtendedColors
import com.obiterjus.ui.theme.ObiterTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.obiterjus.presentation.participantes.VALOR_POLO_ATIVO
import com.obiterjus.presentation.participantes.VALOR_POLO_PASSIVO
import com.obiterjus.presentation.participantes.ehParteAtiva
import com.obiterjus.presentation.participantes.ehPartePassiva

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaEditarProcesso(
    viewModel: ModeloEditarProcesso,
    numeroProcesso: String,
    onVoltar: () -> Unit,
    onExcluido: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val isDark = isSystemInDarkTheme()
    val barBackground = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
    val barContent = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary

    androidx.compose.runtime.LaunchedEffect(numeroProcesso) { viewModel.aoCarregar(numeroProcesso) }
    androidx.compose.runtime.LaunchedEffect(estado.excluido) { if (estado.excluido) onExcluido() }

    var mostrarConfirmacaoExclusao by remember { mutableStateOf(false) }

    if (mostrarConfirmacaoExclusao) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoExclusao = false },
            title = { Text(stringResource(R.string.editar_processo_action_excluir)) },
            text = { Text(stringResource(R.string.excluir_processo_confirmacao, estado.numeroProcesso)) },
            confirmButton = {
                TextButton(onClick = { mostrarConfirmacaoExclusao = false; viewModel.aoExcluir() }) {
                    Text(stringResource(R.string.excluir_processo_confirmar), color = colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacaoExclusao = false }) {
                    Text(stringResource(R.string.excluir_processo_cancelar))
                }
            },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = barBackground, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = dimens.space1, end = dimens.topAppBarPaddingH, top = dimens.cardPaddingV, bottom = dimens.cardPaddingV),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onVoltar) {
                    Icon(ObiterIcones.Voltar, contentDescription = stringResource(R.string.cd_voltar), tint = barContent.copy(alpha = 0.60f), modifier = Modifier.size(dimens.iconBackSize))
                }
                Text(stringResource(R.string.editar_processo_title), style = MaterialTheme.typography.titleLarge, color = barContent, modifier = Modifier.weight(1f))
            }
        }

        if (!estado.carregado) {
            CarregandoCentral()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = dimens.screenMargin),
                verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
            ) {
                item { Spacer(Modifier.height(dimens.space1)) }

                // ── IDENTIFICAÇÃO ──
                item { SecaoTitulo(stringResource(R.string.editar_processo_secao_identificacao), colors, dimens) }
                item { Text(stringResource(R.string.editar_processo_numero, estado.numeroProcesso), style = MaterialTheme.typography.titleSmall) }
                item {
                    CampoData(
                        label = stringResource(R.string.editar_processo_label_data_distribuicao),
                        data = estado.dataDistribuicao,
                        aoAlterar = viewModel::aoAlterarDataDistribuicao,
                        enabled = !estado.salvando, colors = colors, dimens = dimens,
                    )
                }
                item { CampoEdicao(stringResource(R.string.editar_processo_label_assunto), estado.assuntoPrincipal, viewModel::aoAlterarAssunto, !estado.salvando, colors, dimens) }
                item {
                    CampoDropdown(
                        label = stringResource(R.string.editar_processo_label_segredo),
                        valor = estado.segredoJustica,
                        opcoes = listOf(stringResource(R.string.opcao_sim), stringResource(R.string.opcao_nao)),
                        aoAlterar = viewModel::aoAlterarSegredo,
                        enabled = !estado.salvando, colors = colors, dimens = dimens,
                    )
                }

                // ── JUDICIÁRIO ──
                item { SecaoTitulo(stringResource(R.string.editar_processo_secao_judiciario), colors, dimens) }
                item { CampoEdicao(stringResource(R.string.editar_processo_label_tribunal), estado.tribunal, viewModel::aoAlterarTribunal, !estado.salvando, colors, dimens) }
                item { CampoEdicao(stringResource(R.string.editar_processo_label_comarca), estado.comarcaSecao, viewModel::aoAlterarComarca, !estado.salvando, colors, dimens) }
                item { CampoEdicao(stringResource(R.string.editar_processo_label_juizo), estado.juizo, viewModel::aoAlterarJuizo, !estado.salvando, colors, dimens) }
                item { CampoEdicao(stringResource(R.string.editar_processo_label_classe), estado.classeNome, viewModel::aoAlterarClasse, !estado.salvando, colors, dimens) }
                item { CampoEdicao(stringResource(R.string.editar_processo_label_orgao), estado.orgaoJulgadorNome, viewModel::aoAlterarOrgao, !estado.salvando, colors, dimens) }
                item {
                    CampoDropdown(
                        label = stringResource(R.string.editar_processo_label_prioridade),
                        valor = estado.prioridadeTramitacao,
                        opcoes = listOf(stringResource(R.string.opcao_prioridade_nenhuma), stringResource(R.string.opcao_prioridade_idoso)),
                        aoAlterar = viewModel::aoAlterarPrioridade,
                        enabled = !estado.salvando, colors = colors, dimens = dimens,
                    )
                }
                item {
                    CampoDropdown(
                        label = stringResource(R.string.editar_processo_label_gratuidade),
                        valor = estado.gratuidadeJustica,
                        opcoes = listOf(stringResource(R.string.opcao_sim), stringResource(R.string.opcao_nao)),
                        aoAlterar = viewModel::aoAlterarGratuidade,
                        enabled = !estado.salvando, colors = colors, dimens = dimens,
                    )
                }

                // ── REPRESENTAÇÃO ──
                item { SecaoTitulo(stringResource(R.string.editar_processo_secao_representacao), colors, dimens) }
                item { CampoEdicao(stringResource(R.string.editar_processo_label_advogados_ativo), estado.advogadosAtivo, viewModel::aoAlterarAdvogadosAtivo, !estado.salvando, colors, dimens) }
                item { CampoEdicao(stringResource(R.string.editar_processo_label_advogados_passivo), estado.advogadosPassivo, viewModel::aoAlterarAdvogadosPassivo, !estado.salvando, colors, dimens) }
                item { CampoEdicao(stringResource(R.string.editar_processo_label_defensoria), estado.defensoriaPublica, viewModel::aoAlterarDefensoria, !estado.salvando, colors, dimens) }
                item { CampoEdicao(stringResource(R.string.editar_processo_label_mp), estado.ministerioPublico, viewModel::aoAlterarMP, !estado.salvando, colors, dimens) }

                // ── POLO ATIVO ──
                item { 
                    SecaoTitulo(
                        titulo = stringResource(R.string.editar_processo_polo_ativo), 
                        colors = colors, 
                        dimens = dimens,
                        aoAdicionar = { viewModel.aoAdicionarParticipante(VALOR_POLO_ATIVO) }
                    ) 
                }
                val participantesAtivos = estado.participantes.filter { it.ehParteAtiva() }
                if (participantesAtivos.isNotEmpty()) {
                    itemsIndexed(items = participantesAtivos, key = { _, p -> p.idLocal }) { _, participante ->
                        CardParticipante(
                            participante = participante,
                            aoAlterar = { viewModel.aoAlterarParticipante(participante.idLocal, it) },
                            aoRemover = { viewModel.aoRemoverParticipante(participante.idLocal) },
                            enabled = !estado.salvando, colors = colors, dimens = dimens,
                        )
                    }
                }

                // ── POLO PASSIVO ──
                item { 
                    SecaoTitulo(
                        titulo = stringResource(R.string.editar_processo_polo_passivo), 
                        colors = colors, 
                        dimens = dimens,
                        aoAdicionar = { viewModel.aoAdicionarParticipante(VALOR_POLO_PASSIVO) }
                    ) 
                }
                val participantesPassivos = estado.participantes.filter { it.ehPartePassiva() }
                if (participantesPassivos.isNotEmpty()) {
                    itemsIndexed(items = participantesPassivos, key = { _, p -> p.idLocal }) { _, participante ->
                        CardParticipante(
                            participante = participante,
                            aoAlterar = { viewModel.aoAlterarParticipante(participante.idLocal, it) },
                            aoRemover = { viewModel.aoRemoverParticipante(participante.idLocal) },
                            enabled = !estado.salvando, colors = colors, dimens = dimens,
                        )
                    }
                }
                
                // ── OUTROS PARTICIPANTES ──
                val outrosParticipantes = estado.participantes.filter { !it.ehParteAtiva() && !it.ehPartePassiva() }
                if (outrosParticipantes.isNotEmpty()) {
                    item { SecaoTitulo(stringResource(R.string.editar_processo_secao_participantes), colors, dimens) }
                    itemsIndexed(items = outrosParticipantes, key = { _, p -> p.idLocal }) { _, participante ->
                        CardParticipante(
                            participante = participante,
                            aoAlterar = { viewModel.aoAlterarParticipante(participante.idLocal, it) },
                            aoRemover = { viewModel.aoRemoverParticipante(participante.idLocal) },
                            enabled = !estado.salvando, colors = colors, dimens = dimens,
                        )
                    }
                }
                item { CampoEdicao(stringResource(R.string.editar_processo_label_terceiros), estado.terceirosAuxiliares, viewModel::aoAlterarTerceiros, !estado.salvando, colors, dimens) }

                // ── FINANCEIRO ──
                item { SecaoTitulo(stringResource(R.string.editar_processo_secao_financeiro), colors, dimens) }
                item { CampoEdicao(stringResource(R.string.editar_processo_label_valor_causa), estado.valorCausa, viewModel::aoAlterarValorCausa, !estado.salvando, colors, dimens) }
                item {
                    CampoDropdown(
                        label = stringResource(R.string.editar_processo_label_fase),
                        valor = estado.faseProcessual,
                        opcoes = listOf(
                            stringResource(R.string.opcao_fase_conhecimento),
                            stringResource(R.string.opcao_fase_recursal),
                            stringResource(R.string.opcao_fase_execucao),
                        ),
                        aoAlterar = viewModel::aoAlterarFase,
                        enabled = !estado.salvando, colors = colors, dimens = dimens,
                    )
                }
                item {
                    CampoDropdown(
                        label = stringResource(R.string.editar_processo_label_situacao),
                        valor = estado.situacaoAtual,
                        opcoes = listOf(
                            stringResource(R.string.opcao_situacao_ativo),
                            stringResource(R.string.opcao_situacao_suspenso),
                            stringResource(R.string.opcao_situacao_arquivado),
                        ),
                        aoAlterar = viewModel::aoAlterarSituacao,
                        enabled = !estado.salvando, colors = colors, dimens = dimens,
                    )
                }

                // ── URGÊNCIA ──
                item { SecaoTitulo(stringResource(R.string.editar_processo_secao_urgencia), colors, dimens) }
                item {
                    CampoDropdown(
                        label = stringResource(R.string.editar_processo_label_tutela),
                        valor = estado.tutelaAntecipadaLiminar,
                        opcoes = listOf(
                            stringResource(R.string.opcao_nao),
                            stringResource(R.string.opcao_deferido),
                            stringResource(R.string.opcao_indeferido),
                            stringResource(R.string.opcao_parcialmente_deferido),
                        ),
                        aoAlterar = viewModel::aoAlterarTutela,
                        enabled = !estado.salvando, colors = colors, dimens = dimens,
                    )
                }

                item { Spacer(Modifier.height(dimens.space2)) }

                // ── AÇÕES ──
                item {
                    Button(
                        onClick = { viewModel.aoSalvar() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !estado.salvando,
                        shape = RoundedCornerShape(dimens.searchBarRadius),
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = colors.divider, disabledContentColor = colors.textMuted),
                    ) {
                        if (estado.salvando) {
                            CircularProgressIndicator(modifier = Modifier.size(dimens.iconWarningSize), strokeWidth = dimens.borderWidth * 2, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(dimens.chipRowGap))
                        }
                        Text(stringResource(R.string.editar_processo_action_salvar), style = MaterialTheme.typography.labelLarge)
                    }
                }
                item {
                    Button(
                        onClick = { viewModel.aoRessincronizar() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !estado.ressincronizando,
                        shape = RoundedCornerShape(dimens.searchBarRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentPale, contentColor = colors.accent, disabledContainerColor = colors.divider, disabledContentColor = colors.textMuted),
                    ) {
                        if (estado.ressincronizando) {
                            CircularProgressIndicator(modifier = Modifier.size(dimens.iconWarningSize), strokeWidth = dimens.borderWidth * 2, color = colors.accent)
                            Spacer(Modifier.width(dimens.chipRowGap))
                        }
                        Text(
                            text = if (estado.ressincronizando) stringResource(R.string.editar_processo_ressincronizando) else stringResource(R.string.editar_processo_action_ressincronizar),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                item {
                    Button(
                        onClick = { mostrarConfirmacaoExclusao = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !estado.excluindo,
                        shape = RoundedCornerShape(dimens.searchBarRadius),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.dangerPale, contentColor = colors.danger, disabledContainerColor = colors.divider, disabledContentColor = colors.textMuted),
                    ) {
                        Text(stringResource(R.string.editar_processo_action_excluir), style = MaterialTheme.typography.labelLarge)
                    }
                }

                item { Spacer(Modifier.height(dimens.screenMargin)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes privados
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SecaoTitulo(
    titulo: String,
    colors: ObiterExtendedColors,
    dimens: ObiterDimens,
    aoAdicionar: (() -> Unit)? = null
) {
    Column {
        Spacer(Modifier.height(dimens.space2))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(titulo.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.accent)
            if (aoAdicionar != null) {
                IconButton(onClick = aoAdicionar, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = ObiterIcones.Adicionar,
                        contentDescription = stringResource(R.string.cd_adicionar_participante),
                        tint = colors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = dimens.space1), color = colors.divider)
    }
}

@Composable
private fun CampoEdicao(
    label: String,
    valor: String,
    aoAlterar: (String) -> Unit,
    enabled: Boolean,
    colors: ObiterExtendedColors,
    dimens: ObiterDimens,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(dimens.space1))
        OutlinedTextField(
            value = valor,
            onValueChange = aoAlterar,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(dimens.searchBarRadius),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = colors.border),
            enabled = enabled,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoDropdown(
    label: String,
    valor: String,
    opcoes: List<String>,
    aoAlterar: (String) -> Unit,
    enabled: Boolean,
    colors: ObiterExtendedColors,
    dimens: ObiterDimens,
) {
    var expandido by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(dimens.space1))
        ExposedDropdownMenuBox(expanded = expandido && enabled, onExpandedChange = { if (enabled) expandido = it }) {
            OutlinedTextField(
                value = valor,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = enabled).fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(dimens.searchBarRadius),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = colors.border),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                enabled = enabled,
            )
            ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                opcoes.forEach { opcao ->
                    DropdownMenuItem(text = { Text(opcao) }, onClick = { aoAlterar(opcao); expandido = false })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoData(
    label: String,
    data: Instant?,
    aoAlterar: (Instant?) -> Unit,
    enabled: Boolean,
    colors: ObiterExtendedColors,
    dimens: ObiterDimens,
) {
    var mostrarPicker by remember { mutableStateOf(false) }
    val formatador = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault()) }
    val textoData = data?.let { formatador.format(it) } ?: ""

    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(dimens.space1))
        OutlinedTextField(
            value = textoData,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { mostrarPicker = true },
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(dimens.searchBarRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = colors.border,
                disabledBorderColor = colors.border,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            trailingIcon = {
                IconButton(onClick = { if (enabled) mostrarPicker = true }, enabled = enabled) {
                    Icon(ObiterIcones.Agenda, contentDescription = null)
                }
            },
            enabled = false,
        )
        if (mostrarPicker) {
            val pickerState = rememberDatePickerState(initialSelectedDateMillis = data?.toEpochMilli())
            DatePickerDialog(
                onDismissRequest = { mostrarPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { aoAlterar(Instant.ofEpochMilli(it)) }
                        mostrarPicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { mostrarPicker = false }) { Text("Cancelar") } },
            ) { DatePicker(state = pickerState) }
        }
    }
}

@Composable
private fun CardParticipante(
    participante: ParticipanteProcesso,
    aoAlterar: (ParticipanteProcesso) -> Unit,
    aoRemover: () -> Unit,
    enabled: Boolean,
    colors: ObiterExtendedColors,
    dimens: ObiterDimens,
) {
    var expandido by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = dimens.space1)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                // Polo (editável)
                OutlinedTextField(
                    value = participante.polo.orEmpty(),
                    onValueChange = { aoAlterar(participante.copy(polo = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.editar_processo_label_polo), style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.labelMedium,
                    shape = RoundedCornerShape(dimens.searchBarRadius),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = colors.border),
                    enabled = enabled,
                )
                Spacer(Modifier.height(dimens.space1))
                // Nome
                OutlinedTextField(
                    value = participante.nome.orEmpty(),
                    onValueChange = { aoAlterar(participante.copy(nome = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.editar_processo_label_nome_parte), style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = RoundedCornerShape(dimens.searchBarRadius),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = colors.border),
                    enabled = enabled,
                )
            }
            Column {
                IconButton(onClick = { expandido = !expandido }) {
                    Icon(
                        imageVector = if (expandido) ObiterIcones.Recolher else ObiterIcones.Expandir,
                        contentDescription = stringResource(R.string.participante_action_qualificacao),
                        tint = colors.accent,
                    )
                }
                IconButton(onClick = aoRemover, enabled = enabled) {
                    Icon(
                        imageVector = ObiterIcones.Excluir,
                        contentDescription = stringResource(R.string.cd_remover_participante),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        if (expandido) {
            Column(
                modifier = Modifier.padding(start = dimens.space2, top = dimens.space1).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimens.space1),
            ) {
                CampoQualificacao(stringResource(R.string.participante_label_cpf_cnpj), participante.cpfCnpj.orEmpty(), { aoAlterar(participante.copy(cpfCnpj = it)) }, enabled, colors, dimens)
                CampoQualificacao(stringResource(R.string.participante_label_estado_civil), participante.estadoCivil.orEmpty(), { aoAlterar(participante.copy(estadoCivil = it)) }, enabled, colors, dimens)
                CampoQualificacao(stringResource(R.string.participante_label_profissao), participante.profissao.orEmpty(), { aoAlterar(participante.copy(profissao = it)) }, enabled, colors, dimens)
                CampoQualificacao(stringResource(R.string.participante_label_endereco), participante.endereco.orEmpty(), { aoAlterar(participante.copy(endereco = it)) }, enabled, colors, dimens)
                CampoQualificacao(stringResource(R.string.participante_label_contatos), participante.contatos.orEmpty(), { aoAlterar(participante.copy(contatos = it)) }, enabled, colors, dimens)
            }
        }
    }
}

@Composable
private fun CampoQualificacao(
    label: String,
    valor: String,
    aoAlterar: (String) -> Unit,
    enabled: Boolean,
    colors: ObiterExtendedColors,
    dimens: ObiterDimens,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
        Spacer(Modifier.height(dimens.space1))
        OutlinedTextField(
            value = valor,
            onValueChange = aoAlterar,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(dimens.searchBarRadius),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = colors.border),
            enabled = enabled,
        )
    }
}
