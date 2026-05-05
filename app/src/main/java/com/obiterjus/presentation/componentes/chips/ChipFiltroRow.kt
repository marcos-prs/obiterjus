package com.obiterjus.presentation.componentes.chips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun ChipFiltroRow(
    chips: List<String>,
    chipAtivo: String?,
    aoSelecionar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ObiterTheme.dimens

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = dimens.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(dimens.chipRowGap),
    ) {
        items(chips, key = { it }) { chip ->
            ChipFiltro(
                texto = chip,
                ativo = chip == chipAtivo,
                aoClicar = { aoSelecionar(chip) },
            )
        }
    }
}
