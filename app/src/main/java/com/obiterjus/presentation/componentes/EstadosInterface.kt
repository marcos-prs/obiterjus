package com.obiterjus.presentation.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun CarregandoCentral(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun EstadoVazioObiter(
    titulo: String,
    corpo: String,
    icone: ImageVector,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(ObiterTheme.dimens.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ObiterTheme.dimens.space2),
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = ObiterTheme.colors.textMuted,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = titulo,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = corpo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SnackbarErroEffect(
    mensagem: String?,
    snackbarHostState: SnackbarHostState,
    rotuloRetry: String,
    aoTentarNovamente: () -> Unit,
) {
    LaunchedEffect(mensagem) {
        val texto = mensagem ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = texto,
            actionLabel = rotuloRetry,
            withDismissAction = true,
        )
        if (result == SnackbarResult.ActionPerformed) {
            aoTentarNovamente()
        }
    }
}
