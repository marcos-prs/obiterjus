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

/**
 * Regra de tipografia para nomes de pessoa em uma única linha.
 *
 * Mede o nome completo no espaço disponível: se couber, exibe como está. Se não couber,
 * aplica estratégias de abreviação progressivamente mais agressivas, re-medindo cada
 * candidato até encontrar o primeiro que cabe em uma linha:
 *
 * 1. Nome completo — "Marcos Paulo Rodrigues da Silva Rocha"
 * 2. Nomes do meio viram iniciais (conectivos são descartados) — "Marcos P. R. S. Rocha"
 * 3. Apenas primeiro nome + último sobrenome — "Marcos Rocha"
 * 4. Primeiro nome + inicial do último sobrenome — "Marcos R."
 * 5. Apenas o primeiro nome — "Marcos"
 *
 * Se nem o candidato mais curto couber, recorre a reticências.
 */
@Composable
fun TextoNomeUmaLinha(
    nome: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val textMeasurer = rememberTextMeasurer()
        val larguraMax = constraints.maxWidth
        val textoExibido = remember(nome, larguraMax, style, textMeasurer) {
            if (larguraMax == Constraints.Infinity) {
                nome
            } else {
                val candidatos = candidatosAbreviacaoNome(nome)
                candidatos.firstOrNull { candidato ->
                    val resultado = textMeasurer.measure(
                        text = AnnotatedString(candidato),
                        style = style,
                        maxLines = 1,
                        constraints = Constraints(maxWidth = larguraMax),
                    )
                    !resultado.hasVisualOverflow
                } ?: candidatos.last()
            }
        }
        Text(
            text = textoExibido,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val conectivosNome = setOf("da", "das", "de", "do", "dos", "e")

/**
 * Gera os candidatos de exibição do nome, do mais completo ao mais abreviado.
 */
internal fun candidatosAbreviacaoNome(nome: String): List<String> {
    val partes = nome.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (partes.isEmpty()) return listOf("")
    if (partes.size == 1) return listOf(partes.single())

    val primeiro = partes.first()
    val ultimo = partes.last()
    val meio = partes.subList(1, partes.size - 1)
        .filter { it.lowercase() !in conectivosNome }

    val candidatos = mutableListOf(partes.joinToString(" "))

    if (meio.isNotEmpty()) {
        candidatos += buildList {
            add(primeiro)
            meio.forEach { add("${it.first().uppercaseChar()}.") }
            add(ultimo)
        }.joinToString(" ")
    }

    candidatos += "$primeiro $ultimo"
    candidatos += "$primeiro ${ultimo.first().uppercaseChar()}."
    candidatos += primeiro

    return candidatos.distinct()
}
