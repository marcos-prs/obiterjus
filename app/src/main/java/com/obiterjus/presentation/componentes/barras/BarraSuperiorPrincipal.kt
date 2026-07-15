package com.obiterjus.presentation.componentes.barras

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.obiterjus.R
import com.obiterjus.ui.theme.ObiterTheme
import com.obiterjus.ui.theme.OliveGreen
import java.time.LocalTime

@Composable
fun BarraSuperiorPrincipal(
    nomeUsuario: String,
    numeroOab: String,
    ufOab: String,
    ultimaSincronizacao: String?,
    modifier: Modifier = Modifier,
    horaAtual: LocalTime = LocalTime.now(),
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val corTextoPrincipal = colors.topAppBarAccent
    val corTextoSecundario = Color(0xFFE0E7ED)
    val corBadgeTexto = colors.topAppBarAccent
    val corBadgeFundo = Color(0x23F7F9FB)
    val corBadgeBorda = colors.topAppBarAccent.copy(alpha = 0.45f)

    val nomeExibido = nomeUsuario.primeiroNome().ifBlank { stringResource(R.string.saudacao_advogado) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimens.topAppBarHeight)
            .then(
                if (isDarkTheme) {
                    Modifier.drawBehind {
                        drawLine(
                            color = colors.divider,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                } else {
                    Modifier
                }
            ),
        color = colors.topAppBarBackground,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(
                    horizontal = dimens.topAppBarPaddingH,
                    vertical = dimens.cardPaddingV,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(saudacaoResId(horaAtual.hour)),
                        style = MaterialTheme.typography.bodySmall,
                        color = corTextoSecundario,
                    )
                    Text(
                        text = nomeExibido,
                        style = MaterialTheme.typography.titleLarge,
                        color = corTextoPrincipal,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = corBadgeFundo,
                    modifier = Modifier.border(
                        width = 0.5.dp,
                        color = corBadgeBorda,
                        shape = RoundedCornerShape(5.dp),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.oab_chip_formato, ufOab, numeroOab),
                        style = MaterialTheme.typography.labelSmall,
                        color = corBadgeTexto,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.size(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
                modifier = Modifier.semantics {
                    contentDescription = ultimaSincronizacao
                        ?: ""
                },
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(OliveGreen),
                )
                Text(
                    text = ultimaSincronizacao?.let {
                        stringResource(R.string.sincronizado_formato, it)
                    } ?: stringResource(R.string.sincronizacao_pendente),
                    style = MaterialTheme.typography.bodySmall,
                    color = corTextoSecundario,
                )
            }
        }
    }
}

@StringRes
fun saudacaoResId(hora: Int): Int = when (hora) {
    in 6..11 -> R.string.saudacao_bom_dia
    in 12..17 -> R.string.saudacao_boa_tarde
    else -> R.string.saudacao_boa_noite
}

private fun String.primeiroNome(): String =
    trim().substringBefore(' ').trim()
