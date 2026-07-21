package com.obiterjus.presentation.detalhepublicacao

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.obiterjus.R
import com.obiterjus.core.texto.formatarCnj
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.data.agenda.remote.GoogleCalendarAuthorizationRepository
import com.obiterjus.data.agenda.remote.GoogleCalendarAuthorizationResult
import com.obiterjus.domain.model.ConfirmacaoPrazoResultado
import com.obiterjus.domain.model.ProvedorCalendario
import com.obiterjus.presentation.componentes.CarregandoCentral
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraSuperiorSecundaria
import com.obiterjus.ui.theme.ObiterTheme
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

@Composable
fun DetalhePublicacaoScreen(
    viewModel: DetalhePublicacaoViewModel,
    publicacaoId: Long,
    onVoltar: () -> Unit,
    aoAbrirProcesso: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsState()
    val fluxoCadastro by viewModel.fluxoCadastro.collectAsState()
    val resultadoCadastro by viewModel.resultadoCadastroPrazo.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val googleCalendarAuthorizationRepository = remember {
        GlobalContext.get().get<GoogleCalendarAuthorizationRepository>()
    }
    var aguardandoAutorizacaoGoogle by remember { mutableStateOf(false) }

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

    LaunchedEffect(publicacaoId) {
        viewModel.aoCarregar(publicacaoId)
    }

    val autorizacaoGoogleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (!aguardandoAutorizacaoGoogle) return@rememberLauncherForActivityResult
        aguardandoAutorizacaoGoogle = false
        if (result.resultCode != Activity.RESULT_OK) {
            coroutineScope.launch { snackbarHostState.showSnackbar(feedbackErro) }
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            googleCalendarAuthorizationRepository.completeAuthorization(result.data).fold(
                onSuccess = { viewModel.aoConfirmarProvedor(ProvedorCalendario.GOOGLE) },
                onFailure = { snackbarHostState.showSnackbar(feedbackErro) },
            )
        }
    }

    val confirmarComProvedor: (ProvedorCalendario) -> Unit = { provedor ->
        if (provedor == ProvedorCalendario.GOOGLE) {
            val activity = context as? Activity
            if (activity == null) {
                coroutineScope.launch { snackbarHostState.showSnackbar(feedbackErro) }
            } else {
                coroutineScope.launch {
                    googleCalendarAuthorizationRepository.authorize(activity).fold(
                        onSuccess = { outcome ->
                            when (outcome) {
                                is GoogleCalendarAuthorizationResult.Authorized ->
                                    viewModel.aoConfirmarProvedor(provedor)

                                is GoogleCalendarAuthorizationResult.NeedsResolution -> {
                                    aguardandoAutorizacaoGoogle = true
                                    autorizacaoGoogleLauncher.launch(
                                        IntentSenderRequest.Builder(
                                            outcome.pendingIntent.intentSender,
                                        ).build(),
                                    )
                                }
                            }
                        },
                        onFailure = {
                            snackbarHostState.showSnackbar(feedbackErro)
                        },
                    )
                }
            }
        } else {
            viewModel.aoConfirmarProvedor(provedor)
        }
    }

    LaunchedEffect(resultadoCadastro) {
        val resultado = resultadoCadastro ?: return@LaunchedEffect
        when (resultado) {
            ConfirmacaoPrazoResultado.ConfirmadoLocalmente -> {
                snackbarHostState.showSnackbar(message = feedbackLocal)
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
                    viewModel.aoRepetirUltimoCadastro()
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
                    viewModel.aoRepetirUltimoCadastro()
                }
            }
        }
        viewModel.aoConsumirResultadoCadastro()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            BarraSuperiorSecundaria(
                titulo = stringResource(R.string.publicacoes_detalhe_title),
                subtitulo = if (estado.numeroProcesso.isNotBlank()) estado.numeroProcesso.formatarCnj() else null,
                onVoltar = onVoltar
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (!estado.estaCarregando && !estado.naoEncontrada && estado.podeCadastrarPrazo) {
                RodapePrazo(
                    estado = estado,
                    aoAbrirCadastro = viewModel::aoAbrirCadastroPrazo,
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (estado.estaCarregando) {
                CarregandoCentral(modifier = Modifier.fillMaxSize())
            } else if (estado.naoEncontrada || (estado.conteudoCompleto.isBlank() && estado.numeroProcesso.isBlank())) {
                EstadoVazioObiter(
                    titulo = stringResource(R.string.publicacoes_empty_title),
                    corpo = stringResource(R.string.erro_carregar_publicacao),
                    icone = ObiterIcones.Erro,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ConteudoDetalhe(
                    estado = estado,
                    aoAbrirProcesso = aoAbrirProcesso,
                )
            }
        }
    }

    CadastrarPrazoBottomSheet(
        fluxo = fluxoCadastro,
        prazoAtual = estado.prazoAtual,
        dataExpediente = estado.dataExpediente,
        aoAlterarSelecao = viewModel::aoAlterarSelecao,
        aoCalcular = viewModel::aoCalcular,
        aoConfirmarData = viewModel::aoConfirmarData,
        aoVoltarParaSelecao = viewModel::aoVoltarParaSelecao,
        aoConfirmarProvedor = confirmarComProvedor,
        aoFechar = viewModel::aoFecharCadastro,
    )
}

@Composable
private fun RodapePrazo(
    estado: EstadoDetalhePublicacao,
    aoAbrirCadastro: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val prazoAtual = estado.prazoAtual

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = ObiterTheme.colors.divider)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.screenMargin),
            ) {
                val dataAtual = prazoAtual?.dataLimiteEstimada
                if (dataAtual != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimens.space2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.prazo_manual_vencimento_atual,
                                FormatadorData.formatarData(dataAtual),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (prazoAtual.isConfirmado) {
                            Icon(
                                imageVector = ObiterIcones.Sucesso,
                                contentDescription = null,
                                tint = ObiterTheme.colors.success,
                            )
                            Spacer(modifier = Modifier.width(dimens.space1))
                            Text(
                                text = stringResource(R.string.prazo_manual_confirmado_badge),
                                style = MaterialTheme.typography.labelLarge,
                                color = ObiterTheme.colors.success,
                            )
                        }
                    }
                }

                Button(
                    onClick = aoAbrirCadastro,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = if (prazoAtual != null) ObiterIcones.Editar else ObiterIcones.Agenda,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(dimens.space1))
                    Text(
                        text = stringResource(
                            if (prazoAtual != null) {
                                R.string.prazo_manual_editar
                            } else {
                                R.string.prazo_manual_cadastrar
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConteudoDetalhe(
    estado: EstadoDetalhePublicacao,
    aoAbrirProcesso: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val dimens = ObiterTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(dimens.screenMargin)
    ) {
        ItemDetalhe(
            rotulo = stringResource(R.string.label_autos),
            valor = estado.numeroProcesso.formatarCnj(),
            aoClicar = estado.numeroProcesso
                .takeIf { it.isNotBlank() }
                ?.let { numero -> { aoAbrirProcesso(numero) } },
        )

        ItemDetalhe(
            rotulo = stringResource(R.string.label_autor),
            valor = estado.parteAtivaNome,
            tipo = estado.parteAtivaTipo,
        )

        ItemDetalhe(
            rotulo = stringResource(R.string.label_reu),
            valor = estado.partePassivaNome,
            tipo = estado.partePassivaTipo,
        )

        if (estado.advogados.isNotEmpty()) {
            ItemDetalhe(
                rotulo = stringResource(R.string.label_advogados),
                valor = estado.advogados.joinToString("\n")
            )
        }

        // Seção: Data do Expediente
        estado.dataExpediente?.let { data ->
            ItemDetalhe(
                rotulo = stringResource(R.string.label_data_expediente),
                valor = data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = ObiterTheme.colors.divider)
        Spacer(modifier = Modifier.height(16.dp))

        // Seção: Nome do Ato
        Text(
            text = estado.nomeAto,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Conteúdo Completo
        Text(
            text = estado.conteudoCompleto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ItemDetalhe(
    rotulo: String,
    valor: String?,
    tipo: String? = null,
    aoClicar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (valor.isNullOrBlank()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (aoClicar != null) Modifier.clickable(onClick = aoClicar) else Modifier)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = rotulo.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        if (!tipo.isNullOrBlank()) {
            Text(
                text = tipo,
                style = MaterialTheme.typography.labelSmall,
                color = ObiterTheme.colors.textMuted,
            )
        }
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyLarge,
            color = if (aoClicar != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (aoClicar != null) TextDecoration.Underline else null,
        )
    }
}
