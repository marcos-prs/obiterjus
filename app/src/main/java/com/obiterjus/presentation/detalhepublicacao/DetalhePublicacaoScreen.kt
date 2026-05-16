package com.obiterjus.presentation.detalhepublicacao

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obiterjus.R
import com.obiterjus.core.texto.formatarCnj
import com.obiterjus.presentation.componentes.CarregandoCentral
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraSuperiorSecundaria
import com.obiterjus.ui.theme.ObiterTheme
import java.time.format.DateTimeFormatter

@Composable
fun DetalhePublicacaoScreen(
    viewModel: DetalhePublicacaoViewModel,
    publicacaoId: Long,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(publicacaoId) {
        viewModel.aoCarregar(publicacaoId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            BarraSuperiorSecundaria(
                titulo = stringResource(R.string.publicacoes_detalhe_title),
                subtitulo = if (estado.numeroProcesso.isNotBlank()) estado.numeroProcesso.formatarCnj() else null,
                onVoltar = onVoltar
            )
        }
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
                ConteudoDetalhe(estado = estado)
            }
        }
    }
}

@Composable
private fun ConteudoDetalhe(estado: EstadoDetalhePublicacao) {
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
            valor = estado.numeroProcesso.formatarCnj()
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
    modifier: Modifier = Modifier
) {
    if (valor.isNullOrBlank()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
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
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
