package com.obiterjus.presentation.perfil

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.presentation.componentes.SnackbarErroEffect
import com.obiterjus.presentation.componentes.ToggleObiter
import com.obiterjus.presentation.componentes.barras.BarraSuperiorSecundaria
import com.obiterjus.ui.theme.ObiterTheme

/**
 * Exclusão da conta pelo próprio titular, no aplicativo.
 *
 * A tela diz antes o que vai embora e o que fica, porque depois não há como
 * desfazer: não existe lixeira, nem cópia de segurança do lado do servidor.
 */
@Composable
fun ExcluirContaScreen(
    viewModel: ExcluirContaViewModel,
    aoVoltar: () -> Unit,
    aoExcluir: () -> Unit,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dimens = ObiterTheme.dimens
    val context = LocalContext.current
    val urlPedido = stringResource(R.string.url_exclusao_conta)

    SnackbarErroEffect(
        mensagem = estado.mensagemErro,
        snackbarHostState = snackbarHostState,
        rotuloRetry = stringResource(R.string.acao_tentar_novamente),
        aoTentarNovamente = viewModel::aoConsumirErro,
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BarraSuperiorSecundaria(
                titulo = stringResource(R.string.excluir_conta_titulo),
                onVoltar = aoVoltar,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenMargin, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.excluir_conta_aviso),
                style = MaterialTheme.typography.bodyMedium,
            )

            if (estado.email.isNotBlank()) {
                Text(
                    text = stringResource(R.string.excluir_conta_conta_atual, estado.email),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            BlocoLista(
                titulo = stringResource(R.string.excluir_conta_apagado_titulo),
                itens = listOf(
                    stringResource(R.string.excluir_conta_apagado_acervo),
                    stringResource(R.string.excluir_conta_apagado_nuvem),
                    stringResource(R.string.excluir_conta_apagado_perfil),
                    stringResource(R.string.excluir_conta_apagado_local),
                    stringResource(R.string.excluir_conta_apagado_acesso),
                ),
            )

            BlocoLista(
                titulo = stringResource(R.string.excluir_conta_mantido_titulo),
                itens = listOf(
                    stringResource(R.string.excluir_conta_mantido_aceites),
                    stringResource(R.string.excluir_conta_mantido_legal),
                    stringResource(R.string.excluir_conta_mantido_terceiros),
                ),
            )

            if (estado.pedeSenha) {
                OutlinedTextField(
                    value = estado.senha,
                    onValueChange = viewModel::aoAlterarSenha,
                    label = { Text(stringResource(R.string.excluir_conta_label_senha)) },
                    supportingText = { Text(stringResource(R.string.excluir_conta_senha_motivo)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = stringResource(R.string.excluir_conta_sem_senha),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.excluir_conta_ciencia),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                ToggleObiter(
                    ativo = estado.cienteDoEfeito,
                    aoAlternar = viewModel::aoAlternarCiencia,
                )
            }

            Button(
                onClick = viewModel::aoPedirConfirmacao,
                enabled = estado.podeExcluir,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (estado.excluindo) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    Text(stringResource(R.string.excluir_conta_acao))
                }
            }

            TextButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlPedido)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.excluir_conta_outro_meio))
            }
        }
    }

    if (estado.confirmando) {
        AlertDialog(
            onDismissRequest = viewModel::aoDesistir,
            title = { Text(stringResource(R.string.excluir_conta_confirmar_titulo)) },
            text = { Text(stringResource(R.string.excluir_conta_confirmar_texto)) },
            confirmButton = {
                TextButton(onClick = viewModel::aoConfirmarExclusao) {
                    Text(
                        text = stringResource(R.string.excluir_conta_confirmar_acao),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::aoDesistir) {
                    Text(stringResource(R.string.excluir_conta_cancelar))
                }
            },
        )
    }

    if (estado.excluida) {
        AlertDialog(
            onDismissRequest = aoExcluir,
            title = { Text(stringResource(R.string.excluir_conta_pronto_titulo)) },
            text = { Text(stringResource(R.string.excluir_conta_pronto_texto)) },
            confirmButton = {
                TextButton(onClick = aoExcluir) {
                    Text(stringResource(R.string.excluir_conta_pronto_acao))
                }
            },
        )
    }
}

@Composable
private fun BlocoLista(
    titulo: String,
    itens: List<String>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            itens.forEach { item ->
                Text(
                    text = "•  $item",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
