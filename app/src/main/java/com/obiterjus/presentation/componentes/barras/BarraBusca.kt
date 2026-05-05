package com.obiterjus.presentation.componentes.barras

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.obiterjus.R
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun BarraBusca(
    consulta: String,
    aoMudarConsulta: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    TextField(
        value = consulta,
        onValueChange = aoMudarConsulta,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimens.screenMargin,
                vertical = dimens.chipRowGap,
            )
            .height(dimens.searchBarHeight),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = ObiterIcones.Buscar,
                contentDescription = stringResource(R.string.cd_buscar),
                tint = colors.textMuted,
                modifier = Modifier.size(dimens.iconSearchSize),
            )
        },
        trailingIcon = {
            if (consulta.isNotEmpty()) {
                IconButton(onClick = { aoMudarConsulta("") }) {
                    Icon(
                        imageVector = ObiterIcones.Limpar,
                        contentDescription = stringResource(R.string.cd_limpar_busca),
                        tint = colors.textMuted,
                    )
                }
            }
        },
        shape = RoundedCornerShape(dimens.searchBarRadius),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.divider,
            unfocusedContainerColor = colors.divider,
            focusedTextColor = colorScheme.onSurface,
            unfocusedTextColor = colorScheme.onSurface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = colorScheme.primary,
        ),
    )
}
