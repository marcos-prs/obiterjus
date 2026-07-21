package com.obiterjus.presentation.editarprocesso

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.presentation.componentes.CarregandoCentral
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraSuperiorSecundaria
import com.obiterjus.ui.theme.ObiterDimens
import com.obiterjus.ui.theme.ObiterExtendedColors
import com.obiterjus.ui.theme.ObiterTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.obiterjus.presentation.participantes.GrupoPolo
import com.obiterjus.presentation.participantes.TiposParticipacao

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarProcessoScreen(
    viewModel: EditarProcessoViewModel,
    numeroProcesso: String,
    onVoltar: () -> Unit,
    onExcluido: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val subtituloBarra = listOf(estado.tribunal, estado.orgaoJulgadorNome)
        .filter(String::isNotBlank)
        .joinToString(" · ")
        .takeIf(String::isNotBlank)

    val snackbarHostState = remember { SnackbarHostState() }
    // Estado de edição de cada parte içado para cá: assim o rodapé fixo sabe
    // quando mostrar "Registrar parte" e a edição não se perde ao rolar a lista
    // (um item de LazyColumn perde o state local ao sair da composição).
    val edicaoPorParticipante = remember { mutableStateMapOf<String, Boolean>() }
    val msgSalvo = stringResource(R.string.editar_processo_salvo)
    val msgErroExclusao = stringResource(R.string.editar_processo_erro_excluir)
    val msgRessincronizado = stringResource(R.string.editar_processo_ressincronizado)
    val msgErroRessincronizacao = stringResource(R.string.editar_processo_erro_ressincronizar)

    LaunchedEffect(numeroProcesso) { viewModel.aoCarregar(numeroProcesso) }
    LaunchedEffect(estado.excluido) { if (estado.excluido) onExcluido() }
    LaunchedEffect(estado.salvo) {
        if (estado.salvo) {
            snackbarHostState.showSnackbar(msgSalvo)
            viewModel.aoDescartarSalvo()
        }
    }
    LaunchedEffect(estado.erroExclusao) {
        if (estado.erroExclusao) {
            snackbarHostState.showSnackbar(msgErroExclusao)
            viewModel.aoDescartarErroExclusao()
        }
    }
    LaunchedEffect(estado.ressincronizado) {
        if (estado.ressincronizado) {
            snackbarHostState.showSnackbar(msgRessincronizado)
            viewModel.aoDescartarRessincronizado()
        }
    }
    LaunchedEffect(estado.erroRessincronizacao) {
        if (estado.erroRessincronizacao) {
            snackbarHostState.showSnackbar(msgErroRessincronizacao)
            viewModel.aoDescartarErroRessincronizacao()
        }
    }

    estado.sugestaoCliente?.let { sugestao ->
        SheetVincularCliente(
            sugestao = sugestao,
            aoVincular = viewModel::aoVincularClienteExistente,
            aoCriarNovo = viewModel::aoCriarClienteNovo,
            aoDispensar = viewModel::aoDispensarSugestaoCliente,
        )
    }

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

    if (estado.erroNomeParticipanteVazio) {
        AlertDialog(
            onDismissRequest = { viewModel.aoDescartarErroValidacao() },
            title = { Text(stringResource(R.string.participante_erro_nome_vazio_titulo)) },
            text = { Text(stringResource(R.string.participante_erro_nome_vazio_msg)) },
            confirmButton = {
                TextButton(onClick = { viewModel.aoDescartarErroValidacao() }) {
                    Text(stringResource(R.string.participante_erro_ok))
                }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        BarraSuperiorSecundaria(
            titulo = stringResource(R.string.editar_processo_title),
            subtitulo = subtituloBarra,
            onVoltar = onVoltar,
        )

        if (!estado.carregado) {
            CarregandoCentral()
        } else {
            val nomesDuplicados = estado.participantes
                .mapNotNull { it.nome?.trim()?.lowercase()?.takeIf(String::isNotBlank) }
                .groupingBy { it }.eachCount()
                .filterValues { it > 1 }.keys

            // Semeia a edição de cada parte na primeira vez que ela aparece
            // (nova parte entra em edição; parte já registrada entra recolhida) e
            // limpa entradas de partes removidas.
            LaunchedEffect(estado.participantes.map { it.idLocal }) {
                estado.participantes.forEach { p ->
                    if (p.idLocal !in edicaoPorParticipante) {
                        edicaoPorParticipante[p.idLocal] = !p.registrado()
                    }
                }
                edicaoPorParticipante.keys.retainAll(estado.participantes.map { it.idLocal }.toSet())
            }

            val emEdicao: (ParticipanteProcesso) -> Boolean = { p ->
                edicaoPorParticipante[p.idLocal] ?: !p.registrado()
            }
            // Uma parte mostra o formulário quando está em edição ou ainda não foi
            // registrada — é exatamente quando o "Registrar parte" deve aparecer.
            val participanteEditando = estado.participantes
                .firstOrNull { emEdicao(it) || !it.registrado() }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = dimens.screenMargin),
                verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
            ) {
                item { Spacer(Modifier.height(dimens.space1)) }
                item { Text(stringResource(R.string.editar_processo_numero, estado.numeroProcesso), style = MaterialTheme.typography.titleSmall) }

                // ── PARTES ──
                item {
                    SecaoTitulo(
                        titulo = stringResource(R.string.partes_secao),
                        colors = colors,
                        dimens = dimens,
                        aoAdicionar = { viewModel.aoAdicionarParticipante() },
                        adicionarHabilitado = !estado.salvando && estado.participantes.all { it.registrado() },
                    )
                }
                itemsIndexed(items = estado.participantes, key = { _, p -> p.idLocal }) { index, participante ->
                    CardParticipante(
                        participante = participante,
                        emEdicao = emEdicao(participante),
                        aoIniciarEdicao = { edicaoPorParticipante[participante.idLocal] = true },
                        aoAlterar = { viewModel.aoAlterarParticipante(participante.idLocal, it) },
                        aoAlternarCliente = { marcado ->
                            if (marcado) viewModel.aoMarcarComoCliente(participante)
                            else viewModel.aoDesmarcarComoCliente(participante)
                        },
                        aoRemover = { viewModel.aoRemoverParticipante(participante.idLocal) },
                        nomeDuplicado = participante.nome?.trim()?.lowercase() in nomesDuplicados,
                        podeSubir = index > 0,
                        podeDescer = index < estado.participantes.lastIndex,
                        aoSubir = { viewModel.aoMoverParticipante(participante.idLocal, true) },
                        aoDescer = { viewModel.aoMoverParticipante(participante.idLocal, false) },
                        buscandoCep = estado.cepBuscandoIdLocal == participante.idLocal,
                        erroCep = estado.cepErroIdLocal == participante.idLocal,
                        aoBuscarCep = { cep -> viewModel.aoBuscarCep(participante.idLocal, cep) },
                        enabled = !estado.salvando, colors = colors, dimens = dimens,
                    )
                }

                // ── IDENTIFICAÇÃO ──
                item { SecaoTitulo(stringResource(R.string.editar_processo_secao_identificacao), colors, dimens) }
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
                item {
                    CampoDropdown(
                        label = stringResource(R.string.editar_processo_label_natureza),
                        valor = estado.natureza,
                        opcoes = listOf(
                            stringResource(R.string.opcao_natureza_civel),
                            stringResource(R.string.opcao_natureza_penal),
                        ),
                        aoAlterar = viewModel::aoAlterarNatureza,
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
                // "Salvar alterações" mora no rodapé fixo (sempre visível);
                // aqui ficam as ações secundárias.
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

            RodapeAcoes(
                salvando = estado.salvando,
                participanteEditando = participanteEditando,
                aoSalvar = { viewModel.aoSalvar() },
                aoRegistrarParte = { participante ->
                    edicaoPorParticipante[participante.idLocal] = false
                },
                colors = colors,
                dimens = dimens,
            )
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter),
        snackbar = { data -> Snackbar(snackbarData = data) },
    )
    } // Box
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes privados
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Rodapé fixo com as ações primárias, sempre visível ao pé da tela. "Salvar
 * alterações" está sempre presente; enquanto uma parte está em edição, o
 * "Registrar parte" dela aparece logo acima, para não depender de rolar a lista.
 */
@Composable
private fun RodapeAcoes(
    salvando: Boolean,
    participanteEditando: ParticipanteProcesso?,
    aoSalvar: () -> Unit,
    aoRegistrarParte: (ParticipanteProcesso) -> Unit,
    colors: ObiterExtendedColors,
    dimens: ObiterDimens,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = colors.divider)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenMargin, vertical = dimens.space2),
                verticalArrangement = Arrangement.spacedBy(dimens.space1),
            ) {
                if (participanteEditando != null) {
                    Button(
                        onClick = { aoRegistrarParte(participanteEditando) },
                        enabled = !salvando && participanteEditando.registrado(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimens.searchBarRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentPale,
                            contentColor = colors.accent,
                            disabledContainerColor = colors.divider,
                            disabledContentColor = colors.textMuted,
                        ),
                    ) {
                        Icon(ObiterIcones.Confirmar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(dimens.space1))
                        Text(stringResource(R.string.participante_registrar), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Button(
                    onClick = aoSalvar,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !salvando,
                    shape = RoundedCornerShape(dimens.searchBarRadius),
                    colors = ButtonDefaults.buttonColors(disabledContainerColor = colors.divider, disabledContentColor = colors.textMuted),
                ) {
                    if (salvando) {
                        CircularProgressIndicator(modifier = Modifier.size(dimens.iconWarningSize), strokeWidth = dimens.borderWidth * 2, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(dimens.chipRowGap))
                    }
                    Text(stringResource(R.string.editar_processo_action_salvar), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SecaoTitulo(
    titulo: String,
    colors: ObiterExtendedColors,
    dimens: ObiterDimens,
    aoAdicionar: (() -> Unit)? = null,
    adicionarHabilitado: Boolean = true,
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
                IconButton(onClick = aoAdicionar, enabled = adicionarHabilitado, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = ObiterIcones.Adicionar,
                        contentDescription = stringResource(R.string.cd_adicionar_participante),
                        tint = if (adicionarHabilitado) colors.accent else colors.textMuted,
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

private fun ParticipanteProcesso.registrado(): Boolean =
    !nome.isNullOrBlank() && (!tipoParticipacao.isNullOrBlank() || polo != null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardParticipante(
    participante: ParticipanteProcesso,
    emEdicao: Boolean,
    aoIniciarEdicao: () -> Unit,
    aoAlterar: (ParticipanteProcesso) -> Unit,
    aoAlternarCliente: (Boolean) -> Unit,
    aoRemover: () -> Unit,
    nomeDuplicado: Boolean,
    podeSubir: Boolean,
    podeDescer: Boolean,
    aoSubir: () -> Unit,
    aoDescer: () -> Unit,
    buscandoCep: Boolean,
    erroCep: Boolean,
    aoBuscarCep: (String) -> Unit,
    enabled: Boolean,
    colors: ObiterExtendedColors,
    dimens: ObiterDimens,
) {
    var mostrarConfirmRemover by remember { mutableStateOf(false) }

    if (mostrarConfirmRemover) {
        val nome = participante.nome?.trim().orEmpty()
        AlertDialog(
            onDismissRequest = { mostrarConfirmRemover = false },
            title = { Text(stringResource(R.string.participante_remover_titulo)) },
            text = {
                Text(
                    if (nome.isNotBlank()) stringResource(R.string.participante_remover_confirmacao, nome)
                    else stringResource(R.string.participante_remover_confirmacao_sem_nome),
                )
            },
            confirmButton = {
                TextButton(onClick = { mostrarConfirmRemover = false; aoRemover() }) {
                    Text(stringResource(R.string.participante_remover_confirmar), color = colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmRemover = false }) {
                    Text(stringResource(R.string.participante_remover_cancelar))
                }
            },
        )
    }

    val advogadoLabel = stringResource(R.string.participante_grupo_advogado)
    val outroLabel = stringResource(R.string.participante_papel_outro)
    val defPapel = TiposParticipacao.definicao(participante.tipoParticipacao)
    val ehPapelAdvogado = TiposParticipacao.ehAdvogadoTipo(participante.tipoParticipacao)
    var modoOutro by remember(participante.idLocal) {
        mutableStateOf(defPapel == null && !ehPapelAdvogado && !participante.tipoParticipacao.isNullOrBlank())
    }
    val labelPapel = when {
        modoOutro -> outroLabel
        ehPapelAdvogado -> advogadoLabel
        defPapel != null -> defPapel.rotulo
        else -> ""
    }
    val labelPapelExibicao = labelPapel.ifBlank {
        when (TiposParticipacao.grupoDeTipoOuPolo(participante.polo)) {
            GrupoPolo.ATIVO -> stringResource(R.string.participante_grupo_ativo)
            GrupoPolo.PASSIVO -> stringResource(R.string.participante_grupo_passivo)
            else -> ""
        }
    }
    var papelExpandido by remember { mutableStateOf(false) }

    fun selecionarPapel(rotulo: String) {
        modoOutro = false
        val advogado = TiposParticipacao.ehAdvogadoTipo(rotulo)
        aoAlterar(
            participante.copy(
                tipoParticipacao = rotulo,
                polo = TiposParticipacao.poloDerivado(rotulo),
                ehCliente = if (advogado) false else participante.ehCliente,
            ),
        )
        papelExpandido = false
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = dimens.space1)) {
        if (!emEdicao && participante.registrado()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = enabled) { aoIniciarEdicao() }
                        .padding(vertical = dimens.space1),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                    ) {
                        Text(labelPapelExibicao, style = MaterialTheme.typography.labelSmall, color = colors.accent)
                        if (participante.ehCliente) ChipClienteEditar(colors, dimens)
                    }
                    Text(
                        participante.nome.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (nomeDuplicado) colors.danger else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = aoIniciarEdicao, enabled = enabled) {
                    Icon(ObiterIcones.Editar, contentDescription = stringResource(R.string.cd_editar_participante), tint = if (enabled) colors.accent else colors.textMuted)
                }
                IconButton(onClick = aoSubir, enabled = enabled && podeSubir) {
                    Icon(ObiterIcones.MoverCima, contentDescription = stringResource(R.string.cd_mover_participante_cima), tint = if (enabled && podeSubir) colors.accent else colors.textMuted)
                }
                IconButton(onClick = aoDescer, enabled = enabled && podeDescer) {
                    Icon(ObiterIcones.MoverBaixo, contentDescription = stringResource(R.string.cd_mover_participante_baixo), tint = if (enabled && podeDescer) colors.accent else colors.textMuted)
                }
                IconButton(onClick = { mostrarConfirmRemover = true }, enabled = enabled) {
                    Icon(ObiterIcones.Excluir, contentDescription = stringResource(R.string.cd_remover_participante), tint = MaterialTheme.colorScheme.error)
                }
            }
            return@Column
        }

        // ── Cabeçalho: papel + ações ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExposedDropdownMenuBox(
                expanded = papelExpandido && enabled,
                onExpandedChange = { if (enabled) papelExpandido = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = labelPapelExibicao,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled).fillMaxWidth(),
                    label = { Text(stringResource(R.string.participante_label_papel), style = MaterialTheme.typography.labelSmall) },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = RoundedCornerShape(dimens.searchBarRadius),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = colors.border),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = papelExpandido) },
                    enabled = enabled,
                )
                ExposedDropdownMenu(expanded = papelExpandido, onDismissRequest = { papelExpandido = false }) {
                    MenuGrupoHeader(stringResource(R.string.participante_grupo_advogado), colors, dimens)
                    DropdownMenuItem(text = { Text(TiposParticipacao.advogado.rotulo) }, onClick = { selecionarPapel(TiposParticipacao.advogado.rotulo) })
                    MenuGrupoHeader(stringResource(R.string.participante_grupo_ativo), colors, dimens)
                    TiposParticipacao.ativos.forEach { def ->
                        DropdownMenuItem(text = { Text(def.rotulo) }, onClick = { selecionarPapel(def.rotulo) })
                    }
                    MenuGrupoHeader(stringResource(R.string.participante_grupo_passivo), colors, dimens)
                    TiposParticipacao.passivos.forEach { def ->
                        DropdownMenuItem(text = { Text(def.rotulo) }, onClick = { selecionarPapel(def.rotulo) })
                    }
                    MenuGrupoHeader(stringResource(R.string.participante_grupo_outros), colors, dimens)
                    TiposParticipacao.outros.forEach { def ->
                        DropdownMenuItem(text = { Text(def.rotulo) }, onClick = { selecionarPapel(def.rotulo) })
                    }
                    HorizontalDivider(color = colors.divider)
                    DropdownMenuItem(
                        text = { Text(outroLabel) },
                        onClick = {
                            modoOutro = true
                            aoAlterar(participante.copy(tipoParticipacao = "", polo = null))
                            papelExpandido = false
                        },
                    )
                }
            }
            IconButton(onClick = aoSubir, enabled = enabled && podeSubir) {
                Icon(ObiterIcones.MoverCima, contentDescription = stringResource(R.string.cd_mover_participante_cima), tint = if (enabled && podeSubir) colors.accent else colors.textMuted)
            }
            IconButton(onClick = aoDescer, enabled = enabled && podeDescer) {
                Icon(ObiterIcones.MoverBaixo, contentDescription = stringResource(R.string.cd_mover_participante_baixo), tint = if (enabled && podeDescer) colors.accent else colors.textMuted)
            }
            IconButton(onClick = { mostrarConfirmRemover = true }, enabled = enabled) {
                Icon(ObiterIcones.Excluir, contentDescription = stringResource(R.string.cd_remover_participante), tint = MaterialTheme.colorScheme.error)
            }
        }

        // ── Campos (visíveis após o papel ser definido) ──
        val classificado = ehPapelAdvogado || defPapel != null || modoOutro || participante.polo != null
        if (classificado) {
            Column(
                modifier = Modifier.padding(top = dimens.space1).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimens.space1),
            ) {
                if (modoOutro) {
                    CampoQualificacao(stringResource(R.string.participante_label_papel_personalizado), participante.tipoParticipacao.orEmpty(), { aoAlterar(participante.copy(tipoParticipacao = it, polo = null)) }, enabled, colors, dimens)
                }
                OutlinedTextField(
                    value = participante.nome.orEmpty(),
                    onValueChange = { aoAlterar(participante.copy(nome = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.editar_processo_label_nome_parte), style = MaterialTheme.typography.labelSmall) },
                    isError = nomeDuplicado,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = RoundedCornerShape(dimens.searchBarRadius),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = colors.border),
                    enabled = enabled,
                )
                if (nomeDuplicado) {
                    Text(stringResource(R.string.participante_nome_duplicado), style = MaterialTheme.typography.labelSmall, color = colors.danger, modifier = Modifier.padding(start = dimens.space1))
                }
                if (!ehPapelAdvogado) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = participante.ehCliente,
                            onCheckedChange = aoAlternarCliente,
                            enabled = enabled,
                        )
                        Text(
                            stringResource(R.string.participante_eh_cliente),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                CampoPessoaTipo(participante = participante, aoAlterar = aoAlterar, enabled = enabled, colors = colors, dimens = dimens)
                CampoCpfCnpj(participante.cpfCnpj.orEmpty(), { aoAlterar(participante.copy(cpfCnpj = it)) }, enabled, colors, dimens)
                CampoEstadoCivil(participante = participante, aoAlterar = aoAlterar, enabled = enabled, colors = colors, dimens = dimens)
                CampoQualificacao(stringResource(R.string.participante_label_profissao), participante.profissao.orEmpty(), { aoAlterar(participante.copy(profissao = it)) }, enabled, colors, dimens)
                SubsecaoLabel(stringResource(R.string.participante_secao_endereco), colors, dimens)
                CampoCep(participante.cep.orEmpty(), { aoAlterar(participante.copy(cep = it)) }, buscandoCep, erroCep, aoBuscarCep, enabled, colors, dimens)
                CampoQualificacao(stringResource(R.string.participante_label_logradouro), participante.logradouro.orEmpty(), { aoAlterar(participante.copy(logradouro = it)) }, enabled, colors, dimens)
                CampoQualificacao(stringResource(R.string.participante_label_numero_endereco), participante.numeroEndereco.orEmpty(), { aoAlterar(participante.copy(numeroEndereco = it)) }, enabled, colors, dimens)
                SubsecaoLabel(stringResource(R.string.participante_secao_contatos), colors, dimens)
                CampoQualificacao(stringResource(R.string.participante_label_telefone), participante.telefone.orEmpty(), { aoAlterar(participante.copy(telefone = it)) }, enabled, colors, dimens)
                CampoQualificacao(stringResource(R.string.participante_label_email), participante.email.orEmpty(), { aoAlterar(participante.copy(email = it)) }, enabled, colors, dimens)
            }
        }
    }
}

@Composable
private fun MenuGrupoHeader(titulo: String, colors: ObiterExtendedColors, dimens: ObiterDimens) {
    Text(
        text = titulo.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = colors.accent,
        modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.space2, vertical = dimens.space1),
    )
}

@Composable
private fun ChipClienteEditar(colors: ObiterExtendedColors, dimens: ObiterDimens) {
    Surface(
        shape = RoundedCornerShape(dimens.badgeRadius),
        color = colors.accentPale,
    ) {
        Text(
            text = stringResource(R.string.detalhe_chip_cliente),
            style = MaterialTheme.typography.labelSmall,
            color = colors.accent,
            modifier = Modifier.padding(horizontal = dimens.badgePaddingH, vertical = dimens.badgePaddingV),
        )
    }
}

@Composable
private fun SubsecaoLabel(titulo: String, colors: ObiterExtendedColors, dimens: ObiterDimens) {
    Column {
        Spacer(Modifier.height(dimens.space1))
        Text(titulo.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.accent)
        HorizontalDivider(modifier = Modifier.padding(top = 2.dp), color = colors.divider)
    }
}

@Composable
private fun CampoPessoaTipo(
    participante: ParticipanteProcesso,
    aoAlterar: (ParticipanteProcesso) -> Unit,
    enabled: Boolean,
    colors: ObiterExtendedColors,
    dimens: ObiterDimens,
) {
    val fisica = stringResource(R.string.opcao_pessoa_fisica)
    val juridica = stringResource(R.string.opcao_pessoa_juridica)
    val valorAtual = when (participante.tipoPessoa?.trim()?.uppercase()) {
        "F" -> fisica
        "J" -> juridica
        else -> ""
    }
    CampoDropdown(
        label = stringResource(R.string.participante_label_tipo_pessoa),
        valor = valorAtual,
        opcoes = listOf(fisica, juridica),
        aoAlterar = { sel -> aoAlterar(participante.copy(tipoPessoa = if (sel == fisica) "F" else "J")) },
        enabled = enabled, colors = colors, dimens = dimens,
    )
}

@Composable
private fun CampoEstadoCivil(
    participante: ParticipanteProcesso,
    aoAlterar: (ParticipanteProcesso) -> Unit,
    enabled: Boolean,
    colors: ObiterExtendedColors,
    dimens: ObiterDimens,
) {
    CampoDropdown(
        label = stringResource(R.string.participante_label_estado_civil),
        valor = participante.estadoCivil.orEmpty(),
        opcoes = listOf(
            stringResource(R.string.opcao_estado_civil_solteiro),
            stringResource(R.string.opcao_estado_civil_casado),
            stringResource(R.string.opcao_estado_civil_divorciado),
            stringResource(R.string.opcao_estado_civil_separado),
            stringResource(R.string.opcao_estado_civil_viuvo),
            stringResource(R.string.opcao_estado_civil_uniao_estavel),
        ),
        aoAlterar = { aoAlterar(participante.copy(estadoCivil = it)) },
        enabled = enabled, colors = colors, dimens = dimens,
    )
}

@Composable
private fun CampoCpfCnpj(
    valor: String,
    aoAlterar: (String) -> Unit,
    enabled: Boolean,
    colors: ObiterExtendedColors,
    dimens: ObiterDimens,
) {
    val digitos = valor.filter(Char::isDigit).take(14)
    Column {
        Text(stringResource(R.string.participante_label_cpf_cnpj), style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
        Spacer(Modifier.height(dimens.space1))
        OutlinedTextField(
            value = digitos,
            onValueChange = { entrada -> aoAlterar(entrada.filter(Char::isDigit).take(14)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = CpfCnpjVisualTransformation,
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(dimens.searchBarRadius),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = colors.border),
            enabled = enabled,
        )
    }
}

@Composable
private fun CampoCep(
    valor: String,
    aoAlterar: (String) -> Unit,
    buscando: Boolean,
    erro: Boolean,
    aoBuscarCep: (String) -> Unit,
    enabled: Boolean,
    colors: ObiterExtendedColors,
    dimens: ObiterDimens,
) {
    val digitos = valor.filter(Char::isDigit).take(8)
    Column {
        Text(stringResource(R.string.participante_label_cep), style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
        Spacer(Modifier.height(dimens.space1))
        OutlinedTextField(
            value = digitos,
            onValueChange = { entrada ->
                val novos = entrada.filter(Char::isDigit).take(8)
                aoAlterar(novos)
                if (novos.length == 8) aoBuscarCep(novos)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = erro,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = CepVisualTransformation,
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(dimens.searchBarRadius),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = colors.border),
            trailingIcon = {
                if (buscando) CircularProgressIndicator(modifier = Modifier.size(dimens.iconWarningSize), strokeWidth = dimens.borderWidth * 2, color = colors.accent)
            },
            enabled = enabled,
        )
        if (erro) {
            Text(stringResource(R.string.participante_cep_nao_encontrado), style = MaterialTheme.typography.labelSmall, color = colors.danger, modifier = Modifier.padding(top = dimens.space1, start = dimens.space1))
        }
    }
}

private val CpfCnpjVisualTransformation = VisualTransformation { text ->
    val digitos = text.text.filter(Char::isDigit).take(14)
    val sb = StringBuilder()
    val ehCpf = digitos.length <= 11
    digitos.forEachIndexed { i, c ->
        if (ehCpf) {
            if (i == 3 || i == 6) sb.append('.')
            if (i == 9) sb.append('-')
        } else {
            if (i == 2 || i == 5) sb.append('.')
            if (i == 8) sb.append('/')
            if (i == 12) sb.append('-')
        }
        sb.append(c)
    }
    val saida = sb.toString()
    val mapeamento = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            var add = 0
            if (ehCpf) {
                if (offset > 3) add++
                if (offset > 6) add++
                if (offset > 9) add++
            } else {
                if (offset > 2) add++
                if (offset > 5) add++
                if (offset > 8) add++
                if (offset > 12) add++
            }
            return (offset + add).coerceAtMost(saida.length)
        }
        override fun transformedToOriginal(offset: Int): Int =
            saida.substring(0, offset.coerceAtMost(saida.length)).count(Char::isDigit)
    }
    TransformedText(AnnotatedString(saida), mapeamento)
}

private val CepVisualTransformation = VisualTransformation { text ->
    val d = text.text.filter(Char::isDigit).take(8)
    val sb = StringBuilder()
    d.forEachIndexed { i, c ->
        if (i == 5) sb.append('-')
        sb.append(c)
    }
    val saida = sb.toString()
    val mapeamento = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int =
            if (offset > 5) (offset + 1).coerceAtMost(saida.length) else offset
        override fun transformedToOriginal(offset: Int): Int =
            saida.substring(0, offset.coerceAtMost(saida.length)).count(Char::isDigit)
    }
    TransformedText(AnnotatedString(saida), mapeamento)
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
