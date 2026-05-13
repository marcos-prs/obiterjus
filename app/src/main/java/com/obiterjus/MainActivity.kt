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
import com.obiterjus.presentation.adicionarprocesso.ModeloAdicionarProcesso
import com.obiterjus.presentation.auditoria.AuditoriaViewModel
import com.obiterjus.presentation.autenticacao.ModeloAutenticacao
import com.obiterjus.presentation.detalheprocesso.ModeloDetalheProcesso
import com.obiterjus.presentation.detalhepublicacao.ModeloDetalhePublicacao
import com.obiterjus.presentation.editarprocesso.ModeloEditarProcesso
import com.obiterjus.presentation.inicio.ModeloInicio
import com.obiterjus.presentation.monitoramento.MonitoramentoViewModel
import com.obiterjus.presentation.perfil.ModeloPerfil
import com.obiterjus.presentation.principal.ObiterViewModels
import com.obiterjus.presentation.principal.TelaPrincipal
import com.obiterjus.presentation.processos.ModeloProcessos
import com.obiterjus.presentation.prazos.ModeloPrazos
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
                val modeloPerfil: ModeloPerfil = koinViewModel()
                val estadoPerfil by modeloPerfil.estado.collectAsStateWithLifecycle()

                ObiterJusTheme(tema = estadoPerfil.tema) {
                    val modeloInicio: ModeloInicio = koinViewModel()
                    val publicacoesViewModel: PublicacoesViewModel = koinViewModel()
                    val modeloPrazos: ModeloPrazos = koinViewModel()
                    val modeloProcessos: ModeloProcessos = koinViewModel()
                    val modeloAutenticacao: ModeloAutenticacao = koinViewModel()
                    val monitoramentoViewModel: MonitoramentoViewModel = koinViewModel()
                    val detalheProcessoViewModel: ModeloDetalheProcesso = koinViewModel()
                    val detalhePublicacaoViewModel: ModeloDetalhePublicacao = koinViewModel()
                    val auditoriaViewModel: AuditoriaViewModel = koinViewModel()
                    val adicionarProcessoViewModel: ModeloAdicionarProcesso = koinViewModel()
                    val editarProcessoViewModel: ModeloEditarProcesso = koinViewModel()

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

                    TelaPrincipal(viewModels = viewModels)
                }
        }
    }
}
