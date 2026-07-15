package com.obiterjus.presentation.componentes.texto

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Regra de tipografia para textos que devem ocupar exatamente uma linha sem
 * serem cortados — como o número CNJ do processo, cujo valor perde o sentido
 * se truncado com reticências.
 *
 * Mede o texto no espaço disponível: se couber no tamanho original, exibe como
 * está. Se não couber, reduz a fonte em passos de 0.5sp e re-mede até caber ou
 * atingir [tamanhoMinimo]. Só então recorre a reticências, como último recurso.
 */
@Composable
fun TextoAjustavelUmaLinha(
    texto: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    tamanhoMinimo: TextUnit = 10.sp,
) {
    BoxWithConstraints(modifier = modifier) {
        val textMeasurer = rememberTextMeasurer()
        val larguraMax = constraints.maxWidth
        val estiloAjustado = remember(texto, larguraMax, style, tamanhoMinimo, textMeasurer) {
            if (larguraMax == Constraints.Infinity) return@remember style

            val tamanhoInicial = style.fontSize.takeIf { it.isSp } ?: FONTE_PADRAO
            var tamanho = tamanhoInicial
            while (tamanho > tamanhoMinimo) {
                val resultado = textMeasurer.measure(
                    text = AnnotatedString(texto),
                    style = style.copy(fontSize = tamanho),
                    maxLines = 1,
                    constraints = Constraints(maxWidth = larguraMax),
                )
                if (!resultado.hasVisualOverflow) break
                tamanho = (tamanho.value - PASSO_REDUCAO_SP).coerceAtLeast(tamanhoMinimo.value).sp
            }
            style.copy(fontSize = tamanho)
        }
        Text(
            text = texto,
            style = estiloAjustado,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val FONTE_PADRAO = 14.sp
private const val PASSO_REDUCAO_SP = 0.5f
