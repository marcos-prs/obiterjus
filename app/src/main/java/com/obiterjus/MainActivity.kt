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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.presentation.adicionarprocesso.AdicionarProcessoViewModel
import com.obiterjus.presentation.auditoria.AuditoriaViewModel
import com.obiterjus.presentation.autenticacao.AutenticacaoViewModel
import com.obiterjus.presentation.detalheprocesso.DetalheProcessoViewModel
import com.obiterjus.presentation.detalhepublicacao.DetalhePublicacaoViewModel
import com.obiterjus.presentation.editarprocesso.EditarProcessoViewModel
import com.obiterjus.presentation.inicio.InicioViewModel
import com.obiterjus.presentation.monitoramento.MonitoramentoViewModel
import com.obiterjus.presentation.perfil.PerfilViewModel
import com.obiterjus.presentation.principal.ObiterViewModels
import com.obiterjus.presentation.principal.PrincipalScreen
import com.obiterjus.presentation.processos.ProcessosViewModel
import com.obiterjus.presentation.prazos.PrazosViewModel
import com.obiterjus.presentation.publicacoes.PublicacoesViewModel
import com.obiterjus.ui.theme.ObiterJusTheme
import com.obiterjus.ui.theme.TipoTema
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
                val modeloPerfil: PerfilViewModel = koinViewModel()
                val estadoPerfil by modeloPerfil.estado.collectAsStateWithLifecycle()

                ObiterJusTheme(tema = estadoPerfil.tema) {
                    val modeloInicio: InicioViewModel = koinViewModel()
                    val publicacoesViewModel: PublicacoesViewModel = koinViewModel()
                    val modeloPrazos: PrazosViewModel = koinViewModel()
                    val modeloProcessos: ProcessosViewModel = koinViewModel()
                    val modeloAutenticacao: AutenticacaoViewModel = koinViewModel()
                    val monitoramentoViewModel: MonitoramentoViewModel = koinViewModel()
                    val detalheProcessoViewModel: DetalheProcessoViewModel = koinViewModel()
                    val detalhePublicacaoViewModel: DetalhePublicacaoViewModel = koinViewModel()
                    val auditoriaViewModel: AuditoriaViewModel = koinViewModel()
                    val adicionarProcessoViewModel: AdicionarProcessoViewModel = koinViewModel()
                    val editarProcessoViewModel: EditarProcessoViewModel = koinViewModel()

                    val estadoPublicacoes by publicacoesViewModel.estado.collectAsStateWithLifecycle()
                    val exportTextoPendente by monitoramentoViewModel.exportTextoPendente.collectAsStateWithLifecycle()
                    val coroutineScope = rememberCoroutineScope()

                    val viewModels = ObiterViewModels(
                        inicio = modeloInicio,
                        publicacoes = publicacoesViewModel,
                        prazos = modeloPrazos,
                        processos = modeloProcessos,
                        perfil = modeloPerfil,
                        autenticacao = modeloAutenticacao,
                        monitoramento = monitoramentoViewModel,
                        detalheProcesso = detalheProcessoViewModel,
                        detalhePublicacao = detalhePublicacaoViewModel,
                        auditoria = auditoriaViewModel,
                        adicionarProcesso = adicionarProcessoViewModel,
                        editarProcesso = editarProcessoViewModel,
                    )

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
                        runCatching { startActivity(intent) }
                            .onSuccess { publicacoesViewModel.aoConsumirCertidao() }
                            .onFailure { publicacoesViewModel.aoFalharAbrirCertidao() }
                    }

                    LaunchedEffect(exportTextoPendente) {
                        val texto = exportTextoPendente ?: return@LaunchedEffect
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, texto)
                        }
                        startActivity(Intent.createChooser(intent, getString(R.string.monitoramento_exportar_escolher_app)))
                        monitoramentoViewModel.aoConsumirExporte()
                    }

                    PrincipalScreen(viewModels = viewModels)
                }
        }
    }
}
