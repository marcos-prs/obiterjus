package com.obiterjus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.obiterjus.presentation.agenda.AgendaPrazosViewModel
import com.obiterjus.presentation.auditoria.AuditoriaViewModel
import com.obiterjus.presentation.autenticacao.ModeloAutenticacao
import com.obiterjus.presentation.monitoramento.MonitoramentoViewModel
import com.obiterjus.presentation.principal.AbaPrincipal
import com.obiterjus.presentation.principal.TelaPrincipal
import com.obiterjus.presentation.processos.ModeloProcessos
import com.obiterjus.presentation.publicacoes.PublicacoesViewModel
import com.obiterjus.ui.theme.ObiterJusTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ObiterJusTheme {
                val viewModel: MonitoramentoViewModel = koinViewModel()
                val publicacoesViewModel: PublicacoesViewModel = koinViewModel()
                val agendaPrazosViewModel: AgendaPrazosViewModel = koinViewModel()
                val modeloProcessos: ModeloProcessos = koinViewModel()
                val auditoriaViewModel: AuditoriaViewModel = koinViewModel()
                val modeloAutenticacao: ModeloAutenticacao = koinViewModel()
                val estadoMonitoramento by viewModel.uiState.collectAsStateWithLifecycle()
                val estadoPublicacoes by publicacoesViewModel.estado.collectAsStateWithLifecycle()
                val estadoAgendaPrazos by agendaPrazosViewModel.estado.collectAsStateWithLifecycle()
                val estadoProcessos by modeloProcessos.estado.collectAsStateWithLifecycle()
                val estadoAuditoria by auditoriaViewModel.estado.collectAsStateWithLifecycle()
                val estadoAutenticacao by modeloAutenticacao.estado.collectAsStateWithLifecycle()
                val exportTextoPendente by viewModel.exportTextoPendente.collectAsStateWithLifecycle()
                var abaSelecionada by remember { mutableStateOf(AbaPrincipal.MONITORAMENTO) }
                val coroutineScope = rememberCoroutineScope()
                val credentialManager = remember {
                    CredentialManager.create(this@MainActivity)
                }
                val googleWebClientId = getString(R.string.google_web_client_id)
                val loginGoogleHabilitado = googleWebClientId.isNotBlank()
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) {}

                LaunchedEffect(Unit) {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                LaunchedEffect(estadoPublicacoes.certidao.uri) {
                    val uri = estadoPublicacoes.certidao.uri ?: return@LaunchedEffect
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching {
                        startActivity(intent)
                    }.onSuccess {
                        publicacoesViewModel.aoConsumirCertidao()
                    }.onFailure {
                        publicacoesViewModel.aoFalharAbrirCertidao()
                    }
                }

                LaunchedEffect(exportTextoPendente) {
                    val texto = exportTextoPendente ?: return@LaunchedEffect
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, texto)
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.monitoramento_exportar_escolher_app)))
                    viewModel.aoConsumirExporte()
                }

                TelaPrincipal(
                    abaSelecionada = abaSelecionada,
                    aoSelecionarAba = { abaSelecionada = it },
                    estadoMonitoramento = estadoMonitoramento,
                    aoAlterarNumeroOab = viewModel::onNumeroOabChange,
                    aoAlterarUfOab = viewModel::onUfOabChange,
                    aoAlterarDataInicio = viewModel::onDataInicioChange,
                    aoAlterarDataFim = viewModel::onDataFimChange,
                    aoAlterarFrequencia = viewModel::onSyncFrequencyChange,
                    aoSincronizar = viewModel::sincronizar,
                    aoExportarRelatorio = viewModel::exportar,
                    estadoPublicacoes = estadoPublicacoes,
                    aoAlterarFiltroPublicacoes = publicacoesViewModel::aoAlterarFiltroTexto,
                    aoAlterarFiltroTribunalPublicacoes = publicacoesViewModel::aoAlterarFiltroTribunal,
                    aoAlterarFiltroTipoPublicacoes = publicacoesViewModel::aoAlterarFiltroTipo,
                    aoAlterarFiltroDataInicioPublicacoes = publicacoesViewModel::aoAlterarFiltroDataInicio,
                    aoAlterarFiltroDataFimPublicacoes = publicacoesViewModel::aoAlterarFiltroDataFim,
                    aoAlternarSomenteSigilosas = publicacoesViewModel::aoAlternarSomenteSigilosas,
                    aoLimparFiltrosPublicacoes = publicacoesViewModel::aoLimparFiltros,
                    aoSelecionarPublicacao = publicacoesViewModel::aoSelecionarPublicacao,
                    aoFecharDetalhePublicacao = publicacoesViewModel::aoFecharDetalhe,
                    aoAbrirCertidaoPublicacao = publicacoesViewModel::aoAbrirCertidao,
                    estadoAgendaPrazos = estadoAgendaPrazos,
                    aoSelecionarPublicacaoAgenda = { publicacaoId ->
                        publicacoesViewModel.aoSelecionarPublicacao(publicacaoId)
                        abaSelecionada = AbaPrincipal.PUBLICACOES
                    },
                    estadoProcessos = estadoProcessos,
                    aoAlterarFiltroProcessos = modeloProcessos::aoAlterarFiltroTexto,
                    aoAlterarFiltroParticipanteProcessos = modeloProcessos::aoAlterarFiltroParticipante,
                    aoAlterarFiltroSyncStatusProcessos = modeloProcessos::aoAlterarFiltroSyncStatus,
                    aoAlterarOrdenacaoProcessos = modeloProcessos::aoAlterarOrdenacao,
                    aoLimparFiltrosProcessos = modeloProcessos::aoLimparFiltros,
                    aoSelecionarProcesso = modeloProcessos::aoSelecionarProcesso,
                    aoFecharDetalheProcesso = modeloProcessos::aoFecharDetalhe,
                    estadoAuditoria = estadoAuditoria,
                    estadoAutenticacao = estadoAutenticacao,
                    aoSair = {
                        modeloAutenticacao.signOut()
                        coroutineScope.launch {
                            runCatching {
                                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                            }
                        }
                    },
                    aoLogarGoogle = {
                        if (loginGoogleHabilitado) {
                            coroutineScope.launch {
                                val googleIdOption = GetSignInWithGoogleOption.Builder(googleWebClientId)
                                    .build()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                runCatching {
                                    credentialManager.getCredential(this@MainActivity, request)
                                }.getOrNull()?.credential
                                    ?.let { credential ->
                                        credential as? CustomCredential
                                    }
                                    ?.takeIf { credential ->
                                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                    }
                                    ?.let { credential ->
                                        runCatching {
                                            GoogleIdTokenCredential.createFrom(credential.data)
                                        }.getOrNull()?.idToken
                                    }
                                    ?.let(modeloAutenticacao::signInGoogle)
                            }
                        }
                    },
                    aoLogarEmail = modeloAutenticacao::signInEmail,
                    aoCadastrarEmail = modeloAutenticacao::signUpEmail,
                    loginGoogleHabilitado = loginGoogleHabilitado,
                )
            }
        }
    }
}
