package com.obiterjus.presentation.componentes

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun ToggleObiter(
    ativo: Boolean,
    aoAlternar: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ObiterTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Switch(
        checked = ativo,
        onCheckedChange = aoAlternar,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = colorScheme.primary,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = colors.border,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}
