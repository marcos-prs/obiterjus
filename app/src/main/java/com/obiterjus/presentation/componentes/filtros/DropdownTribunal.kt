package com.obiterjus.presentation.componentes.filtros

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obiterjus.R
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.domain.model.GeneroTribunal
import com.obiterjus.presentation.componentes.chips.ChipFiltro
import com.obiterjus.ui.theme.ObiterTheme
import androidx.compose.ui.platform.LocalFocusManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DropdownTribunal(
    tribunalSelecionado: String,
    tribunaisPorGenero: Map<GeneroTribunal, List<String>>,
    aoSelecionarTribunal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var consulta by remember(tribunalSelecionado) { mutableStateOf(tribunalSelecionado) }
    val dimens = ObiterTheme.dimens
    val focusManager = LocalFocusManager.current
    val secoes = remember(tribunaisPorGenero, consulta) {
        val termo = consulta.normalizarBusca()
        GeneroTribunal.entries.mapNotNull { genero ->
            val especiesEstaticas = GeneroTribunal.obterEspecies(genero)
            val especiesDinâmicas = tribunaisPorGenero[genero].orEmpty()
            
            val todasEspecies = (especiesEstaticas + especiesDinâmicas)
                .distinctBy { it.uppercase() }
                .filter { tribunal ->
                    termo.isBlank() || tribunal.normalizarBusca().contains(termo)
                }
                .sortedWith(String.CASE_INSENSITIVE_ORDER)

            if (todasEspecies.isEmpty()) null else genero to todasEspecies
        }
    }

    LaunchedEffect(expanded) {
        if (expanded && tribunalSelecionado.isBlank()) {
            consulta = ""
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = consulta,
            onValueChange = { valor -> 
                consulta = valor
                expanded = true
            },
            label = { Text(stringResource(R.string.publicacoes_label_tribunal)) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                .fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (consulta.isNotBlank()) {
                        IconButton(
                            onClick = {
                                consulta = ""
                                aoSelecionarTribunal("")
                                expanded = true
                            },
                        ) {
                            Icon(
                                imageVector = ObiterIcones.Limpar,
                                contentDescription = stringResource(R.string.publicacoes_filtro_limpar),
                            )
                        }
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { 
                expanded = false
                focusManager.clearFocus()
            },
            modifier = Modifier.heightIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.cardPaddingH),
            ) {

                secoes.forEach { (genero, tribunais) ->
                    Text(
                        text = stringResource(genero.rotuloRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = dimens.space3, bottom = dimens.space1),
                    )
                    FlowRow(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(dimens.chipRowGap),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(dimens.chipRowGap),
                    ) {
                        tribunais.forEach { tribunal ->
                            ChipFiltro(
                                texto = tribunal,
                                ativo = tribunal.equals(tribunalSelecionado, ignoreCase = true),
                                aoClicar = {
                                    consulta = tribunal
                                    aoSelecionarTribunal(tribunal)
                                    expanded = false
                                    focusManager.clearFocus()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun String.normalizarBusca(): String =
    trim().lowercase(Locale.ROOT)
