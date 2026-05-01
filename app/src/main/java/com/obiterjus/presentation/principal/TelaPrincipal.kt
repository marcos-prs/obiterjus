package com.obiterjus.presentation.principal

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.obiterjus.R
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.presentation.agenda.EstadoAgendaPrazos
import com.obiterjus.presentation.agenda.TelaAgendaPrazos
import com.obiterjus.presentation.auditoria.EstadoAuditoria
import com.obiterjus.presentation.auditoria.TelaAuditoria
import com.obiterjus.presentation.autenticacao.EstadoAutenticacao
import com.obiterjus.presentation.autenticacao.TelaAutenticacao
import com.obiterjus.presentation.monitoramento.MonitoramentoUiState
import com.obiterjus.presentation.monitoramento.TelaMonitoramento
import com.obiterjus.presentation.processos.EstadoProcessos
import com.obiterjus.presentation.processos.OrdenacaoProcessos
import com.obiterjus.presentation.processos.TelaProcessos
import com.obiterjus.presentation.publicacoes.EstadoPublicacoes
import com.obiterjus.presentation.publicacoes.TelaPublicacoes
import com.obiterjus.ui.theme.ObiterTheme

enum class AbaPrincipal {
    MONITORAMENTO,
    PUBLICACOES,
    AGENDA,
    PROCESSOS,
    AUDITORIA,
    PERFIL,
}

@Composable
fun TelaPrincipal(
    abaSelecionada: AbaPrincipal,
    aoSelecionarAba: (AbaPrincipal) -> Unit,
    estadoMonitoramento: MonitoramentoUiState,
    aoAlterarNumeroOab: (String) -> Unit,
    aoAlterarUfOab: (String) -> Unit,
    aoAlterarDataInicio: (String) -> Unit,
    aoAlterarDataFim: (String) -> Unit,
    aoAlterarFrequencia: (Int) -> Unit,
    aoSincronizar: () -> Unit,
    aoExportarRelatorio: () -> Unit,
    estadoPublicacoes: EstadoPublicacoes,
    aoAlterarFiltroPublicacoes: (String) -> Unit,
    aoAlterarFiltroTribunalPublicacoes: (String) -> Unit,
    aoAlterarFiltroTipoPublicacoes: (String) -> Unit,
    aoAlterarFiltroDataInicioPublicacoes: (String) -> Unit,
    aoAlterarFiltroDataFimPublicacoes: (String) -> Unit,
    aoAlternarSomenteSigilosas: () -> Unit,
    aoLimparFiltrosPublicacoes: () -> Unit,
    aoSelecionarPublicacao: (Long) -> Unit,
    aoFecharDetalhePublicacao: () -> Unit,
    aoAbrirCertidaoPublicacao: (Publicacao) -> Unit,
    estadoAgendaPrazos: EstadoAgendaPrazos,
    aoSelecionarPublicacaoAgenda: (Long) -> Unit,
    estadoProcessos: EstadoProcessos,
    aoAlterarFiltroProcessos: (String) -> Unit,
    aoAlterarFiltroParticipanteProcessos: (String) -> Unit,
    aoAlterarFiltroSyncStatusProcessos: (String) -> Unit,
    aoAlterarOrdenacaoProcessos: (OrdenacaoProcessos) -> Unit,
    aoLimparFiltrosProcessos: () -> Unit,
    aoSelecionarProcesso: (String) -> Unit,
    aoFecharDetalheProcesso: () -> Unit,
    estadoAuditoria: EstadoAuditoria,
    estadoAutenticacao: EstadoAutenticacao,
    aoSair: () -> Unit,
    aoLogarGoogle: () -> Unit,
    aoLogarEmail: (String, String) -> Unit,
    aoCadastrarEmail: (String, String) -> Unit,
    loginGoogleHabilitado: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Scaffold(
            bottomBar = {
                BarraPrincipal(
                    abaSelecionada = abaSelecionada,
                    aoSelecionarAba = aoSelecionarAba,
                )
            },
        ) { paddingValues ->
            ConteudoPrincipal(
                paddingValues = paddingValues,
                abaSelecionada = abaSelecionada,
                estadoMonitoramento = estadoMonitoramento,
                aoAlterarNumeroOab = aoAlterarNumeroOab,
                aoAlterarUfOab = aoAlterarUfOab,
                aoAlterarDataInicio = aoAlterarDataInicio,
                aoAlterarDataFim = aoAlterarDataFim,
                aoAlterarFrequencia = aoAlterarFrequencia,
                aoSincronizar = aoSincronizar,
                aoExportarRelatorio = aoExportarRelatorio,
                estadoPublicacoes = estadoPublicacoes,
                aoAlterarFiltroPublicacoes = aoAlterarFiltroPublicacoes,
                aoAlterarFiltroTribunalPublicacoes = aoAlterarFiltroTribunalPublicacoes,
                aoAlterarFiltroTipoPublicacoes = aoAlterarFiltroTipoPublicacoes,
                aoAlterarFiltroDataInicioPublicacoes = aoAlterarFiltroDataInicioPublicacoes,
                aoAlterarFiltroDataFimPublicacoes = aoAlterarFiltroDataFimPublicacoes,
                aoAlternarSomenteSigilosas = aoAlternarSomenteSigilosas,
                aoLimparFiltrosPublicacoes = aoLimparFiltrosPublicacoes,
                aoSelecionarPublicacao = aoSelecionarPublicacao,
                aoFecharDetalhePublicacao = aoFecharDetalhePublicacao,
                aoAbrirCertidaoPublicacao = aoAbrirCertidaoPublicacao,
                estadoAgendaPrazos = estadoAgendaPrazos,
                aoSelecionarPublicacaoAgenda = aoSelecionarPublicacaoAgenda,
                estadoProcessos = estadoProcessos,
                aoAlterarFiltroProcessos = aoAlterarFiltroProcessos,
                aoAlterarFiltroParticipanteProcessos = aoAlterarFiltroParticipanteProcessos,
                aoAlterarFiltroSyncStatusProcessos = aoAlterarFiltroSyncStatusProcessos,
                aoAlterarOrdenacaoProcessos = aoAlterarOrdenacaoProcessos,
                aoLimparFiltrosProcessos = aoLimparFiltrosProcessos,
                aoSelecionarProcesso = aoSelecionarProcesso,
                aoFecharDetalheProcesso = aoFecharDetalheProcesso,
                estadoAuditoria = estadoAuditoria,
                estadoAutenticacao = estadoAutenticacao,
                aoSair = aoSair,
                aoLogarGoogle = aoLogarGoogle,
                aoLogarEmail = aoLogarEmail,
                aoCadastrarEmail = aoCadastrarEmail,
                loginGoogleHabilitado = loginGoogleHabilitado,
            )
        }
    }
}

@Composable
private fun ConteudoPrincipal(
    paddingValues: PaddingValues,
    abaSelecionada: AbaPrincipal,
    estadoMonitoramento: MonitoramentoUiState,
    aoAlterarNumeroOab: (String) -> Unit,
    aoAlterarUfOab: (String) -> Unit,
    aoAlterarDataInicio: (String) -> Unit,
    aoAlterarDataFim: (String) -> Unit,
    aoAlterarFrequencia: (Int) -> Unit,
    aoSincronizar: () -> Unit,
    aoExportarRelatorio: () -> Unit,
    estadoPublicacoes: EstadoPublicacoes,
    aoAlterarFiltroPublicacoes: (String) -> Unit,
    aoAlterarFiltroTribunalPublicacoes: (String) -> Unit,
    aoAlterarFiltroTipoPublicacoes: (String) -> Unit,
    aoAlterarFiltroDataInicioPublicacoes: (String) -> Unit,
    aoAlterarFiltroDataFimPublicacoes: (String) -> Unit,
    aoAlternarSomenteSigilosas: () -> Unit,
    aoLimparFiltrosPublicacoes: () -> Unit,
    aoSelecionarPublicacao: (Long) -> Unit,
    aoFecharDetalhePublicacao: () -> Unit,
    aoAbrirCertidaoPublicacao: (Publicacao) -> Unit,
    estadoAgendaPrazos: EstadoAgendaPrazos,
    aoSelecionarPublicacaoAgenda: (Long) -> Unit,
    estadoProcessos: EstadoProcessos,
    aoAlterarFiltroProcessos: (String) -> Unit,
    aoAlterarFiltroParticipanteProcessos: (String) -> Unit,
    aoAlterarFiltroSyncStatusProcessos: (String) -> Unit,
    aoAlterarOrdenacaoProcessos: (OrdenacaoProcessos) -> Unit,
    aoLimparFiltrosProcessos: () -> Unit,
    aoSelecionarProcesso: (String) -> Unit,
    aoFecharDetalheProcesso: () -> Unit,
    estadoAuditoria: EstadoAuditoria,
    estadoAutenticacao: EstadoAutenticacao,
    aoSair: () -> Unit,
    aoLogarGoogle: () -> Unit,
    aoLogarEmail: (String, String) -> Unit,
    aoCadastrarEmail: (String, String) -> Unit,
    loginGoogleHabilitado: Boolean,
) {
    when (abaSelecionada) {
        AbaPrincipal.MONITORAMENTO -> TelaMonitoramento(
            uiState = estadoMonitoramento,
            onNumeroOabChange = aoAlterarNumeroOab,
            onUfOabChange = aoAlterarUfOab,
            onDataInicioChange = aoAlterarDataInicio,
            onDataFimChange = aoAlterarDataFim,
            onSyncFrequencyChange = aoAlterarFrequencia,
            onSincronizarClick = aoSincronizar,
            onExportarClick = aoExportarRelatorio,
            modifier = Modifier.padding(paddingValues),
        )

        AbaPrincipal.PUBLICACOES -> TelaPublicacoes(
            estado = estadoPublicacoes,
            aoAlterarFiltroTexto = aoAlterarFiltroPublicacoes,
            aoAlterarFiltroTribunal = aoAlterarFiltroTribunalPublicacoes,
            aoAlterarFiltroTipo = aoAlterarFiltroTipoPublicacoes,
            aoAlterarFiltroDataInicio = aoAlterarFiltroDataInicioPublicacoes,
            aoAlterarFiltroDataFim = aoAlterarFiltroDataFimPublicacoes,
            aoAlternarSomenteSigilosas = aoAlternarSomenteSigilosas,
            aoLimparFiltros = aoLimparFiltrosPublicacoes,
            aoSelecionarPublicacao = aoSelecionarPublicacao,
            aoFecharDetalhe = aoFecharDetalhePublicacao,
            aoAbrirCertidao = aoAbrirCertidaoPublicacao,
            modifier = Modifier.padding(paddingValues),
        )

        AbaPrincipal.AGENDA -> TelaAgendaPrazos(
            estado = estadoAgendaPrazos,
            aoSelecionarPublicacao = aoSelecionarPublicacaoAgenda,
            modifier = Modifier.padding(paddingValues),
        )

        AbaPrincipal.PROCESSOS -> TelaProcessos(
            estado = estadoProcessos,
            aoAlterarFiltroTexto = aoAlterarFiltroProcessos,
            aoAlterarFiltroParticipante = aoAlterarFiltroParticipanteProcessos,
            aoAlterarFiltroSyncStatus = aoAlterarFiltroSyncStatusProcessos,
            aoAlterarOrdenacao = aoAlterarOrdenacaoProcessos,
            aoLimparFiltros = aoLimparFiltrosProcessos,
            aoSelecionarProcesso = aoSelecionarProcesso,
            aoFecharDetalhe = aoFecharDetalheProcesso,
            modifier = Modifier.padding(paddingValues),
        )

        AbaPrincipal.AUDITORIA -> TelaAuditoria(
            estado = estadoAuditoria,
            modifier = Modifier.padding(paddingValues),
        )

        AbaPrincipal.PERFIL -> TelaAutenticacao(
            estado = estadoAutenticacao,
            onSignOut = aoSair,
            onSignInGoogle = aoLogarGoogle,
            onSignInEmail = aoLogarEmail,
            onSignUpEmail = aoCadastrarEmail,
            googleLoginEnabled = loginGoogleHabilitado,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun BarraPrincipal(
    abaSelecionada: AbaPrincipal,
    aoSelecionarAba: (AbaPrincipal) -> Unit,
) {
    val dimens = ObiterTheme.dimens

    NavigationBar(
        modifier = Modifier.height(dimens.bottomBarHeight),
    ) {
        AbaPrincipal.entries.forEach { aba ->
            NavigationBarItem(
                selected = abaSelecionada == aba,
                onClick = { aoSelecionarAba(aba) },
                icon = {
                    Icon(
                        imageVector = aba.icone(),
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(aba.tituloResId())) },
            )
        }
    }
}

private fun AbaPrincipal.tituloResId(): Int =
    when (this) {
        AbaPrincipal.MONITORAMENTO -> R.string.aba_monitoramento
        AbaPrincipal.PUBLICACOES -> R.string.aba_publicacoes
        AbaPrincipal.AGENDA -> R.string.aba_agenda
        AbaPrincipal.PROCESSOS -> R.string.aba_processos
        AbaPrincipal.AUDITORIA -> R.string.auditoria_title
        AbaPrincipal.PERFIL -> R.string.aba_perfil
    }

private fun AbaPrincipal.icone(): ImageVector =
    when (this) {
        AbaPrincipal.MONITORAMENTO -> Icons.Default.Sync
        AbaPrincipal.PUBLICACOES -> Icons.AutoMirrored.Filled.Article
        AbaPrincipal.AGENDA -> Icons.Default.Event
        AbaPrincipal.PROCESSOS -> Icons.Default.Folder
        AbaPrincipal.AUDITORIA -> Icons.Default.History
        AbaPrincipal.PERFIL -> Icons.Default.AccountCircle
    }
