package com.obiterjus.presentation.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.obiterjus.ui.theme.ObiterTheme

private const val PESO_CONTEUDO_CABECALHO = 1f

@Composable
fun CabecalhoListagem(
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimens.space2),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CartaoFiltro(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = ObiterTheme.dimens

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.radiusSmall),
    ) {
        Column(
            modifier = Modifier.padding(dimens.space4),
            verticalArrangement = Arrangement.spacedBy(dimens.space3),
            content = content,
        )
    }
}

@Composable
fun CartaoFormulario(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = ObiterTheme.dimens

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.radiusSmall),
    ) {
        Column(
            modifier = Modifier.padding(dimens.space4),
            verticalArrangement = Arrangement.spacedBy(dimens.space3),
            content = content,
        )
    }
}

@Composable
fun CartaoDetalhe(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = ObiterTheme.dimens

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.radiusSmall),
    ) {
        Column(
            modifier = Modifier.padding(dimens.space4),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
            content = content,
        )
    }
}

@Composable
fun CartaoItemListagem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isHighlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val shape = RoundedCornerShape(dimens.radiusSmall)
    val border = BorderStroke(
        if (isHighlighted) dimens.strokeMedium else dimens.strokeThin,
        if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    )

    if (onClick == null) {
        OutlinedCard(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            border = border,
        ) {
            ConteudoCartaoItem(content = content)
        }
    } else {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            border = border,
        ) {
            ConteudoCartaoItem(content = content)
        }
    }
}

@Composable
private fun ConteudoCartaoItem(
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = ObiterTheme.dimens

    Column(
        modifier = Modifier.padding(dimens.space4),
        verticalArrangement = Arrangement.spacedBy(dimens.space3),
        content = content,
    )
}

@Composable
fun EstadoVazioListagem(
    titulo: String,
    corpo: String,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens

    CartaoItemListagem(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimens.space2),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = corpo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CartaoStatus(
    titulo: String,
    mensagem: String,
    isErro: Boolean,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val icone = if (isErro) Icons.Default.Warning else Icons.Default.Info
    val corDestaque = if (isErro) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.radiusSmall),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(dimens.strokeThin, corDestaque),
    ) {
        Row(
            modifier = Modifier.padding(dimens.space3),
            horizontalArrangement = Arrangement.spacedBy(dimens.space3),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = corDestaque,
            )
            Column(verticalArrangement = Arrangement.spacedBy(dimens.space1)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = mensagem,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CabecalhoDetalhe(
    titulo: String,
    subtitulo: String,
    fecharDescricao: String,
    aoFechar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(PESO_CONTEUDO_CABECALHO),
            verticalArrangement = Arrangement.spacedBy(dimens.space1),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = aoFechar) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = fecharDescricao,
            )
        }
    }
}

@Composable
fun CampoDetalheListagem(
    rotulo: String,
    valor: String,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimens.space1),
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun ChipInformativoListagem(
    texto: String,
    modifier: Modifier = Modifier,
    icone: ImageVector? = null,
) {
    AssistChip(
        modifier = modifier,
        onClick = {},
        label = { Text(text = texto) },
        leadingIcon = icone?.let { imageVector ->
            {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                )
            }
        },
    )
}
