package com.obiterjus.presentation.prazos

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.obiterjus.R
import com.obiterjus.data.agenda.remote.GoogleCalendarAuthorizationRepository
import com.obiterjus.data.agenda.remote.GoogleCalendarAuthorizationResult
import com.obiterjus.core.texto.formatarCnj
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.domain.model.ConfirmacaoPrazoResultado
import com.obiterjus.domain.model.ProvedorCalendario
import com.obiterjus.presentation.componentes.filtros.DropdownTribunal
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraBusca
import com.obiterjus.presentation.componentes.chips.BadgeTipoAto
import com.obiterjus.presentation.componentes.chips.VarianteBadge
import com.obiterjus.ui.theme.ObiterTheme
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

@Composable
fun PrazosScreen(
    viewModel: PrazosViewModel,
    aoAbrirPublicacao: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val googleCalendarAuthorizationRepository = remember {
        GlobalContext.get().get<GoogleCalendarAuthorizationRepository>()
    }
    var prazoEmDialogo by remember { mutableStateOf<PrazoUiItem?>(null) }
    var prazoGooglePendente by remember { mutableStateOf<PrazoUiItem?>(null) }
    val feedbackLocal = stringResource(R.string.prazos_feedback_local)
    val rotuloGoogle = stringResource(R.string.prazos_provedor_google)
    val rotuloOutlook = stringResource(R.string.prazos_provedor_outlook)
    val rotuloLocal = stringResource(R.string.prazos_provedor_local)
    val feedbackCalendarioPrefixo = stringResource(R.string.prazos_feedback_calendario)
    val feedbackPendentePrefixo = stringResource(R.string.prazos_feedback_pendente)
    val feedbackErro = stringResource(R.string.prazos_confirmacao_erro)
    val acaoTentarNovamente = stringResource(R.string.acao_tentar_novamente)
    val rotuloProvedor: (ProvedorCalendario) -> String = { provedor ->
        when (provedor) {
            ProvedorCalendario.GOOGLE -> rotuloGoogle
            ProvedorCalendario.OUTLOOK -> rotuloOutlook
            ProvedorCalendario.LOCAL -> rotuloLocal
        }
    }

    val autorizacaoGoogleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val prazoPendente = prazoGooglePendente ?: return@rememberLauncherForActivityResult
        if (result.resultCode != Activity.RESULT_OK) {
            prazoGooglePendente = null
            coroutineScope.launch {
                snackbarHostState.showSnackbar(feedbackErro)
            }
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            val tokenResult = googleCalendarAuthorizationRepository.completeAuthorization(result.data)
            tokenResult.fold(
                onSuccess = {
                    viewModel.aoConfirmarPrazo(
                        publicacaoId = prazoPendente.item.publicacao.id,
                        provedor = ProvedorCalendario.GOOGLE,
                    )
                },
                onFailure = {
                    snackbarHostState.showSnackbar(feedbackErro)
                },
            )
            prazoGooglePendente = null
        }
    }

    LaunchedEffect(estado.resultadoConfirmacao) {
        val resultado = estado.resultadoConfirmacao ?: return@LaunchedEffect
        when (resultado) {
            ConfirmacaoPrazoResultado.ConfirmadoLocalmente -> {
                snackbarHostState.showSnackbar(
                    message = feedbackLocal,
                )
            }
            is ConfirmacaoPrazoResultado.EventoCriado -> {
                snackbarHostState.showSnackbar(
                    message = String.format(
                        Locale.getDefault(),
                        feedbackCalendarioPrefixo,
                        rotuloProvedor(resultado.provedor),
                    ),
                )
            }
            is ConfirmacaoPrazoResultado.SincronizacaoPendente -> {
                if (
                    snackbarHostState.showSnackbar(
                        message = String.format(
                            Locale.getDefault(),
                            feedbackPendentePrefixo,
                            rotuloProvedor(resultado.provedor),
                        ),
                        actionLabel = acaoTentarNovamente,
                        withDismissAction = true,
                    ) == SnackbarResult.ActionPerformed
                ) {
                    viewModel.aoRepetirUltimaConfirmacao()
                }
            }
            ConfirmacaoPrazoResultado.Falha -> {
                if (
                    snackbarHostState.showSnackbar(
                        message = feedbackErro,
                        actionLabel = acaoTentarNovamente,
                        withDismissAction = true,
                    ) == SnackbarResult.ActionPerformed
                ) {
                    viewModel.aoRepetirUltimaConfirmacao()
                }
            }
        }
        viewModel.aoConsumirResultadoConfirmacao()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        val dimens = ObiterTheme.dimens
        val abas = listOf(
            AbaPrazos.TODOS to stringResource(R.string.prazos_aba_todos),
            AbaPrazos.VENCIDOS to stringResource(R.string.prazos_aba_vencidos),
            AbaPrazos.PROXIMOS to stringResource(R.string.prazos_aba_proximos),
            AbaPrazos.SEM_DATA to stringResource(R.string.prazos_aba_sem_data),
        )
        val tituloExpirados = stringResource(R.string.prazos_secao_expirados)
        val tituloUrgente = stringResource(R.string.prazos_secao_urgente)
        val tituloSemana = stringResource(R.string.prazos_secao_semana)
        val tituloProximos = stringResource(R.string.prazos_secao_proximos)
        val tituloSemData = stringResource(R.string.prazos_secao_sem_data)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = dimens.screenMargin),
            verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
        ) {
            item {
                BarraBusca(
                    consulta = estado.filtros.busca,
                    aoMudarConsulta = viewModel::aoAlterarBusca,
                    placeholder = stringResource(R.string.busca_prazos_placeholder),
                )
            }
            item {
                DropdownTribunal(
                    tribunalSelecionado = estado.filtros.tribunal,
                    tribunaisPorGenero = estado.tribunaisPorGenero,
                    aoSelecionarTribunal = { tribunal ->
                        viewModel.aoSelecionarTribunal(tribunal.takeIf { it.isNotBlank() })
                    },
                    modifier = Modifier.padding(horizontal = dimens.screenMargin),
                )
            }
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.screenMargin),
                    shape = RoundedCornerShape(dimens.cardRadius),
                    color = ObiterTheme.colors.warningPale,
                    border = BorderStroke(dimens.borderWidth, ObiterTheme.colors.warning),
                ) {
                    Text(
                        text = stringResource(R.string.prazos_aviso),
                        style = MaterialTheme.typography.bodySmall,
                        color = ObiterTheme.colors.warning,
                        modifier = Modifier.padding(dimens.cardPaddingH),
                    )
                }
            }
            item {
                SecondaryTabRow(
                    selectedTabIndex = estado.abaSelecionada.ordinal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.screenMargin),
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = {
                        HorizontalDivider(
                            color = ObiterTheme.colors.divider,
                            thickness = dimens.borderWidth,
                        )
                    },
                    indicator = {
                        SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(
                                estado.abaSelecionada.ordinal,
                            ),
                            color = ObiterTheme.colors.accent,
                        )
                    },
                ) {
                    abas.forEach { (aba, rotulo) ->
                        Tab(
                            selected = estado.abaSelecionada == aba,
                            onClick = { viewModel.aoSelecionarAba(aba) },
                            text = {
                                Text(
                                    text = rotulo,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (estado.abaSelecionada == aba) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        ObiterTheme.colors.textMuted
                                    },
                                )
                            },
                        )
                    }
                }
            }

            when (estado.abaSelecionada) {
                AbaPrazos.TODOS -> {
                    secaoPrazos(
                        titulo = tituloExpirados,
                        itens = estado.expirados,
                        prioridade = VarianteBadge.URGENTE,
                        confirmandoPrazoId = estado.confirmandoPrazoId,
                        onSolicitarConfirmacao = { prazoEmDialogo = it },
                    )
                    secaoPrazos(
                        titulo = tituloUrgente,
                        itens = estado.urgente,
                        prioridade = VarianteBadge.URGENTE,
                        confirmandoPrazoId = estado.confirmandoPrazoId,
                        onSolicitarConfirmacao = { prazoEmDialogo = it },
                    )
                    secaoPrazos(
                        titulo = tituloSemana,
                        itens = estado.estaSemana,
                        prioridade = VarianteBadge.SENTENCA,
                        confirmandoPrazoId = estado.confirmandoPrazoId,
                        onSolicitarConfirmacao = { prazoEmDialogo = it },
                    )
                    secaoPrazos(
                        titulo = tituloProximos,
                        itens = estado.proximos,
                        prioridade = VarianteBadge.DESPACHO,
                        confirmandoPrazoId = estado.confirmandoPrazoId,
                        onSolicitarConfirmacao = { prazoEmDialogo = it },
                    )
                    secaoPrazos(
                        titulo = tituloSemData,
                        itens = estado.semData,
                        prioridade = VarianteBadge.DESPACHO,
                        confirmandoPrazoId = estado.confirmandoPrazoId,
                        onSolicitarConfirmacao = { prazoEmDialogo = it },
                    )
                }
                AbaPrazos.VENCIDOS -> secaoPrazos(
                    titulo = tituloExpirados,
                    itens = estado.expirados,
                    prioridade = VarianteBadge.URGENTE,
                    confirmandoPrazoId = estado.confirmandoPrazoId,
                    onSolicitarConfirmacao = { prazoEmDialogo = it },
                )
                AbaPrazos.PROXIMOS -> {
                    secaoPrazos(
                        titulo = tituloUrgente,
                        itens = estado.urgente,
                        prioridade = VarianteBadge.URGENTE,
                        confirmandoPrazoId = estado.confirmandoPrazoId,
                        onSolicitarConfirmacao = { prazoEmDialogo = it },
                    )
                    secaoPrazos(
                        titulo = tituloSemana,
                        itens = estado.estaSemana,
                        prioridade = VarianteBadge.SENTENCA,
                        confirmandoPrazoId = estado.confirmandoPrazoId,
                        onSolicitarConfirmacao = { prazoEmDialogo = it },
                    )
                }
                // FUTUROS aba removida; PRÓXIMOS já cobrem próximos e futuros
                AbaPrazos.SEM_DATA -> secaoPrazos(
                    titulo = tituloSemData,
                    itens = estado.semData,
                    prioridade = VarianteBadge.DESPACHO,
                    confirmandoPrazoId = estado.confirmandoPrazoId,
                    onSolicitarConfirmacao = { prazoEmDialogo = it },
                )
            }

            if (estado.itensExibidos.isEmpty()) {
                item {
                    EstadoVazioObiter(
                        titulo = stringResource(R.string.agenda_empty_title),
                        corpo = stringResource(R.string.agenda_empty_body),
                        icone = ObiterIcones.PrazosInativo,
                        modifier = Modifier.padding(horizontal = dimens.screenMargin),
                    )
                }
            }
        }
    }

    val prazoParaConfirmar = prazoEmDialogo
    if (prazoParaConfirmar != null) {
            DialogConfirmarPrazo(
                prazo = prazoParaConfirmar,
                onDismiss = { prazoEmDialogo = null },
                onVerAto = {
                    prazoEmDialogo = null
                    aoAbrirPublicacao(prazoParaConfirmar.item.publicacao.id)
                },
                onConfirmar = { provedor ->
                    if (provedor == ProvedorCalendario.GOOGLE) {
                        val activity = context as? Activity
                        if (activity == null) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(feedbackErro)
                            }
                        } else {
                            coroutineScope.launch {
                                val authorizationResult = googleCalendarAuthorizationRepository.authorize(activity)
                                authorizationResult.fold(
                                    onSuccess = { outcome ->
                                        when (outcome) {
                                            is GoogleCalendarAuthorizationResult.Authorized -> {
                                                prazoGooglePendente = null
                                                viewModel.aoConfirmarPrazo(
                                                    publicacaoId = prazoParaConfirmar.item.publicacao.id,
                                                    provedor = provedor,
                                                )
                                            }
                                            is GoogleCalendarAuthorizationResult.NeedsResolution -> {
                                                prazoGooglePendente = prazoParaConfirmar
                                                autorizacaoGoogleLauncher.launch(
                                                    IntentSenderRequest.Builder(
                                                        outcome.pendingIntent.intentSender,
                                                    ).build(),
                                                )
                                            }
                                        }
                                        prazoEmDialogo = null
                                    },
                                    onFailure = {
                                        prazoGooglePendente = null
                                        snackbarHostState.showSnackbar(feedbackErro)
                                    },
                                )
                            }
                        }
                    } else {
                        viewModel.aoConfirmarPrazo(
                            publicacaoId = prazoParaConfirmar.item.publicacao.id,
                            provedor = provedor,
                        )
                        prazoEmDialogo = null
                    }
            },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.secaoPrazos(
    titulo: String,
    itens: List<PrazoUiItem>,
    prioridade: VarianteBadge,
    confirmandoPrazoId: Long?,
    onSolicitarConfirmacao: (PrazoUiItem) -> Unit,
) {
    if (itens.isEmpty()) return

    item {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = ObiterTheme.dimens.screenMargin),
        )
    }
    items(
        items = itens,
        key = { prazo -> prazo.item.publicacao.id },
    ) { prazo ->
        CardPrazo(
            prazo = prazo,
            prioridade = prioridade,
            confirmando = confirmandoPrazoId == prazo.item.publicacao.id,
            onSolicitarConfirmacao = { onSolicitarConfirmacao(prazo) },
            modifier = Modifier.padding(horizontal = ObiterTheme.dimens.screenMargin),
        )
    }
}

@Composable
private fun CardPrazo(
    prazo: PrazoUiItem,
    prioridade: VarianteBadge,
    confirmando: Boolean,
    onSolicitarConfirmacao: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = if (isPressed) colors.primaryPale else colorScheme.surface,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "prazoPress",
    )
    val item = prazo.item
    val numeroProcesso = item.publicacao.numeroProcesso?.formatarCnj()
        ?: stringResource(R.string.prazos_sem_processo)
    val dataVencimento = item.prazo.dataLimiteEstimada?.let(FormatadorData::formatarData)
        ?: stringResource(R.string.agenda_item_sem_data)
    val diasBadge = prazo.rotuloDias()
    val confirmado = item.prazo.isConfirmado
    val provedor = ProvedorCalendario.fromCodigo(item.prazo.provedorCalendario)
    val labelConfirmado = when (provedor) {
        ProvedorCalendario.GOOGLE -> stringResource(R.string.prazos_confirmado_google)
        ProvedorCalendario.OUTLOOK -> stringResource(R.string.prazos_confirmado_outlook)
        ProvedorCalendario.LOCAL -> stringResource(R.string.prazos_confirmado_local)
        null -> stringResource(R.string.prazos_confirmado_generico)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { if (!confirmado && !confirmando) onSolicitarConfirmacao() },
            ),
        shape = RoundedCornerShape(dimens.cardRadius),
        color = containerColor,
        border = BorderStroke(dimens.borderWidth, colors.border),
    ) {
        Row(modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .width(dimens.stripeWidth)
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topStart = dimens.cardRadius,
                            bottomStart = dimens.cardRadius,
                        ),
                    )
                    .background(
                        when (prioridade) {
                            VarianteBadge.URGENTE -> colors.danger
                            VarianteBadge.SENTENCA -> colorScheme.secondary
                            VarianteBadge.DECISAO -> colorScheme.primary
                            VarianteBadge.DESPACHO -> colors.divider
                            VarianteBadge.FAVORAVEL -> colors.success
                            VarianteBadge.TRIBUNAL -> colorScheme.primary
                            VarianteBadge.CONFIANCA_ALTA,
                            VarianteBadge.CONFIANCA_MEDIA,
                            VarianteBadge.CONFIANCA_BAIXA -> colors.divider
                        },
                    ),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = dimens.cardPaddingH,
                        vertical = dimens.cardPaddingV,
                    ),
                verticalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BadgeTipoAto(
                        texto = diasBadge,
                        variante = when (prazo.grupo) {
                            GrupoPrazo.EXPIRADOS -> VarianteBadge.URGENTE
                            GrupoPrazo.URGENTE -> VarianteBadge.URGENTE
                            GrupoPrazo.ESTA_SEMANA -> VarianteBadge.SENTENCA
                            GrupoPrazo.PROXIMOS -> VarianteBadge.DESPACHO
                            GrupoPrazo.SEM_DATA -> VarianteBadge.DESPACHO
                        },
                    )
                    if (confirmado) {
                        BadgeTipoAto(
                            texto = labelConfirmado,
                            variante = VarianteBadge.FAVORAVEL,
                        )
                    } else {
                        BadgeTipoAto(
                            texto = item.publicacao.tribunal
                                ?: stringResource(R.string.publicacoes_sem_tribunal),
                            variante = VarianteBadge.TRIBUNAL,
                        )
                    }
                }

                Text(
                    text = dataVencimento,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colorScheme.onSurface,
                )

                Text(
                    text = numeroProcesso,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )

                Text(
                    text = item.prazo.textoOriginal,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BadgeTipoAto(
                        texto = item.publicacao.tipoComunicacao
                            ?: stringResource(R.string.publicacoes_sem_tipo),
                        variante = when (prioridade) {
                            VarianteBadge.URGENTE -> VarianteBadge.URGENTE
                            VarianteBadge.SENTENCA -> VarianteBadge.SENTENCA
                            VarianteBadge.DECISAO -> VarianteBadge.DECISAO
                            VarianteBadge.DESPACHO -> VarianteBadge.DESPACHO
                            VarianteBadge.FAVORAVEL -> VarianteBadge.FAVORAVEL
                            VarianteBadge.TRIBUNAL -> VarianteBadge.TRIBUNAL
                            VarianteBadge.CONFIANCA_ALTA,
                            VarianteBadge.CONFIANCA_MEDIA,
                            VarianteBadge.CONFIANCA_BAIXA -> VarianteBadge.DESPACHO
                        },
                    )
                    BadgeTipoAto(
                        texto = item.publicacao.fonte,
                        variante = VarianteBadge.DESPACHO,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (confirmado) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = ObiterIcones.Sucesso,
                                contentDescription = null,
                                tint = colors.success,
                            )
                            Spacer(modifier = Modifier.width(dimens.space1))
                            Text(
                                text = labelConfirmado,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.success,
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onSolicitarConfirmacao,
                            enabled = !confirmado && !confirmando,
                        ) {
                            if (confirmando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(dimens.iconSearchSize),
                                    strokeWidth = dimens.borderWidth,
                                    color = colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(dimens.space1))
                                Text(text = stringResource(R.string.prazos_confirmando))
                            } else {
                                Icon(
                                    imageVector = ObiterIcones.Confirmar,
                                    contentDescription = null,
                                )
                                Spacer(modifier = Modifier.width(dimens.space1))
                                Text(text = stringResource(R.string.agenda_confirmar_prazo))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogConfirmarPrazo(
    prazo: PrazoUiItem,
    onDismiss: () -> Unit,
    onVerAto: () -> Unit,
    onConfirmar: (ProvedorCalendario) -> Unit,
) {
    var provedorSelecionado by rememberSaveable(prazo.item.publicacao.id) {
        mutableStateOf(ProvedorCalendario.GOOGLE)
    }
    val provedores = listOf(
        ProvedorCalendario.GOOGLE to stringResource(R.string.prazos_provedor_google),
        ProvedorCalendario.OUTLOOK to stringResource(R.string.prazos_provedor_outlook),
        ProvedorCalendario.LOCAL to stringResource(R.string.prazos_provedor_local),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.prazos_confirmar_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ObiterTheme.dimens.space2)) {
                Text(
                    text = prazo.item.publicacao.numeroProcesso?.formatarCnj()
                        ?: stringResource(R.string.prazos_sem_processo),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = prazo.item.prazo.dataLimiteEstimada?.let(FormatadorData::formatarData)
                        ?: stringResource(R.string.agenda_item_sem_data),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = prazo.item.prazo.textoOriginal,
                    style = MaterialTheme.typography.bodySmall,
                    color = ObiterTheme.colors.textMuted,
                )
                TextButton(onClick = onVerAto) {
                    Icon(
                        imageVector = ObiterIcones.Documento,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(ObiterTheme.dimens.space1))
                    Text(text = stringResource(R.string.prazos_ver_ato))
                }
                provedores.forEach { (provedor, rotulo) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { provedorSelecionado = provedor }
                            .padding(vertical = ObiterTheme.dimens.space1),
                    ) {
                        RadioButton(
                            selected = provedorSelecionado == provedor,
                            onClick = { provedorSelecionado = provedor },
                        )
                        Spacer(modifier = Modifier.width(ObiterTheme.dimens.space2))
                        Text(text = rotulo)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(provedorSelecionado) },
            ) {
                Text(text = stringResource(R.string.prazos_confirmar_acao))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun PrazoUiItem.rotuloDias(): String =
    when (diasRestantes) {
        null -> stringResource(R.string.prazos_badge_sem_data)
        0 -> stringResource(R.string.prazos_badge_hoje)
        1 -> stringResource(R.string.prazos_badge_amanha)
        else -> {
            if (diasRestantes < 0) {
                stringResource(R.string.prazos_badge_expirado)
            } else {
                stringResource(R.string.prazos_badge_dias, diasRestantes)
            }
        }
    }
