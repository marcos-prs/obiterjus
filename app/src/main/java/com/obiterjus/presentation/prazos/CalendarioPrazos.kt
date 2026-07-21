package com.obiterjus.presentation.prazos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.obiterjus.R
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.ui.theme.ObiterTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields

private val SaverYearMonth = listSaver<YearMonth, Int>(
    save = { listOf(it.year, it.monthValue) },
    restore = { YearMonth.of(it[0], it[1]) },
)

/**
 * Calendário mensal com um pin em cada data que possui prazo vencendo.
 */
@Composable
fun CalendarioPrazos(
    datasComPrazo: Set<LocalDate>,
    hoje: LocalDate,
    modifier: Modifier = Modifier,
    aoSelecionarData: (LocalDate) -> Unit = {},
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    var mesExibido by rememberSaveable(stateSaver = SaverYearMonth) {
        mutableStateOf(YearMonth.from(hoje))
    }

    val primeiroDiaSemana = WeekFields.of(locale).firstDayOfWeek
    val diasSemana = (0L until 7L).map { primeiroDiaSemana.plus(it) }
    val deslocamento = ((mesExibido.atDay(1).dayOfWeek.value - primeiroDiaSemana.value) + 7) % 7
    val totalDias = mesExibido.lengthOfMonth()
    val semanas = ((deslocamento + totalDias) + 6) / 7
    val tituloMes = mesExibido.month
        .getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.titlecase(locale) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.cardRadius),
        color = colorScheme.surface,
        border = BorderStroke(dimens.borderWidth, colors.border),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = dimens.cardPaddingH,
                vertical = dimens.cardPaddingV,
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.space1),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { mesExibido = mesExibido.minusMonths(1) }) {
                    Icon(
                        imageVector = ObiterIcones.Anterior,
                        contentDescription = stringResource(R.string.prazos_calendario_mes_anterior),
                        tint = colorScheme.primary,
                    )
                }
                Text(
                    text = "$tituloMes ${mesExibido.year}",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { mesExibido = mesExibido.plusMonths(1) }) {
                    Icon(
                        imageVector = ObiterIcones.Proximo,
                        contentDescription = stringResource(R.string.prazos_calendario_proximo_mes),
                        tint = colorScheme.primary,
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                diasSemana.forEach { dia ->
                    Text(
                        text = dia.getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            repeat(semanas) { semana ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { coluna ->
                        val dia = (semana * 7 + coluna) - deslocamento + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(dimens.space10),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (dia in 1..totalDias) {
                                val data = mesExibido.atDay(dia)
                                val temPrazo = data in datasComPrazo
                                DiaCalendario(
                                    dia = dia,
                                    isHoje = data == hoje,
                                    temPrazo = temPrazo,
                                    aoClicar = if (temPrazo) {
                                        { aoSelecionarData(data) }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaCalendario(
    dia: Int,
    isHoje: Boolean,
    temPrazo: Boolean,
    aoClicar: (() -> Unit)? = null,
) {
    val colors = ObiterTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (aoClicar != null) {
            Modifier.clickable(onClick = aoClicar)
        } else {
            Modifier
        },
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isHoje) colorScheme.primary else colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = dia.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isHoje) colorScheme.onPrimary else colorScheme.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (temPrazo) colors.danger else colorScheme.surface),
        )
    }
}
