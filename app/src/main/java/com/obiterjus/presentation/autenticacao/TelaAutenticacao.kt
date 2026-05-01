package com.obiterjus.presentation.autenticacao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.obiterjus.R
import com.obiterjus.presentation.componentes.ConteudoRolavelAba
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun TelaAutenticacao(
    estado: EstadoAutenticacao,
    onSignOut: () -> Unit,
    onSignInGoogle: () -> Unit,
    onSignInEmail: (String, String) -> Unit,
    onSignUpEmail: (String, String) -> Unit,
    googleLoginEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens

    ConteudoRolavelAba(modifier = modifier) {
        CabecalhoAutenticacao()

        if (estado.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        estado.erro?.let { erro ->
            MensagemEstadoConta(
                texto = stringResource(erro.mensagemResId()),
                tipo = TipoMensagemConta.ERRO,
            )
        }

        MensagemSincronizacao(estado = estado.sincronizacao)

        if (estado.usuario == null || estado.usuario.isAnonymous) {
            ConteudoContaConvidada(
                estado = estado,
                onSignInGoogle = onSignInGoogle,
                onSignInEmail = onSignInEmail,
                onSignUpEmail = onSignUpEmail,
                googleLoginEnabled = googleLoginEnabled,
            )
        } else {
            ConteudoContaAutenticada(
                email = estado.usuario.email ?: stringResource(R.string.autenticacao_user_fallback),
                isLoading = estado.isLoading,
                onSignOut = onSignOut,
            )
        }
    }
}

@Composable
private fun CabecalhoAutenticacao() {
    val dimens = ObiterTheme.dimens

    Column(
        verticalArrangement = Arrangement.spacedBy(dimens.space2),
    ) {
        Text(
            text = stringResource(R.string.autenticacao_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.autenticacao_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConteudoContaConvidada(
    estado: EstadoAutenticacao,
    onSignInGoogle: () -> Unit,
    onSignInEmail: (String, String) -> Unit,
    onSignUpEmail: (String, String) -> Unit,
    googleLoginEnabled: Boolean,
) {
    val dimens = ObiterTheme.dimens
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }

    CartaoConta {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimens.space3),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.autenticacao_subtitle_anonymous),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    CartaoConta {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimens.space3),
        ) {
            Text(
                text = stringResource(R.string.autenticacao_email_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.autenticacao_label_email)) },
                enabled = !estado.isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text(stringResource(R.string.autenticacao_label_password)) },
                enabled = !estado.isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            )

            Button(
                onClick = { onSignInEmail(email, senha) },
                enabled = !estado.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        if (estado.isLoading) {
                            R.string.autenticacao_action_loading
                        } else {
                            R.string.autenticacao_action_login_email
                        },
                    ),
                )
            }

            TextButton(
                onClick = { onSignUpEmail(email, senha) },
                enabled = !estado.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.autenticacao_action_signup))
            }
        }
    }

    CartaoConta {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimens.space3),
        ) {
            Text(
                text = stringResource(R.string.autenticacao_google_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedButton(
                onClick = onSignInGoogle,
                enabled = googleLoginEnabled && !estado.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.autenticacao_action_login_google))
            }

            if (!googleLoginEnabled) {
                Text(
                    text = stringResource(R.string.autenticacao_google_indisponivel),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConteudoContaAutenticada(
    email: String,
    isLoading: Boolean,
    onSignOut: () -> Unit,
) {
    val dimens = ObiterTheme.dimens

    CartaoConta {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimens.space3),
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.autenticacao_subtitle_authenticated, email),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onSignOut,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.autenticacao_action_logout))
            }
        }
    }
}

@Composable
private fun CartaoConta(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dimens = ObiterTheme.dimens

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.radiusSmall),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(dimens.space4),
        ) {
            content()
        }
    }
}

@Composable
private fun MensagemSincronizacao(
    estado: EstadoSincronizacaoNuvem,
) {
    when (estado) {
        EstadoSincronizacaoNuvem.Ociosa -> Unit
        EstadoSincronizacaoNuvem.Sincronizando -> MensagemEstadoConta(
            texto = stringResource(R.string.autenticacao_sync_loading),
            tipo = TipoMensagemConta.INFORMACAO,
        )

        EstadoSincronizacaoNuvem.Falha -> MensagemEstadoConta(
            texto = stringResource(R.string.autenticacao_sync_error),
            tipo = TipoMensagemConta.ERRO,
        )

        is EstadoSincronizacaoNuvem.Concluida -> MensagemEstadoConta(
            texto = stringResource(
                R.string.autenticacao_sync_success,
                estado.resumo.total,
            ),
            tipo = TipoMensagemConta.SUCESSO,
        )
    }
}

@Composable
private fun MensagemEstadoConta(
    texto: String,
    tipo: TipoMensagemConta,
) {
    val dimens = ObiterTheme.dimens
    val (icone, cor) = when (tipo) {
        TipoMensagemConta.INFORMACAO -> Icons.Default.CloudSync to MaterialTheme.colorScheme.primary
        TipoMensagemConta.SUCESSO -> Icons.Default.CloudDone to MaterialTheme.colorScheme.primary
        TipoMensagemConta.ERRO -> Icons.Default.Error to MaterialTheme.colorScheme.error
    }

    CartaoConta {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = cor,
            )
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = when (tipo) {
                    TipoMensagemConta.ERRO -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

private enum class TipoMensagemConta {
    INFORMACAO,
    SUCESSO,
    ERRO,
}

@Composable
private fun ErroAutenticacao.mensagemResId(): Int =
    when (this) {
        ErroAutenticacao.CamposObrigatorios -> R.string.autenticacao_error_required_fields
        ErroAutenticacao.EntrarEmail -> R.string.autenticacao_error_login_email
        ErroAutenticacao.CriarConta -> R.string.autenticacao_error_signup
        ErroAutenticacao.EntrarGoogle -> R.string.autenticacao_error_login_google
        ErroAutenticacao.Sair -> R.string.autenticacao_error_logout
    }
