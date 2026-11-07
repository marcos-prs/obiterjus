package com.obiterjus.presentation.minuta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.obiterjus.ui.theme.ObiterTheme

/**
 * Render leve de markdown para o rascunho da minuta. Cobre o subconjunto que a
 * geração usa: títulos `#`/`##`/`###`, listas `-`/`*`, negrito `**…**`. Não é um
 * parser completo — o objetivo é legibilidade, não fidelidade tipográfica.
 */
@Composable
fun TextoMarkdownLeve(markdown: String, modifier: Modifier = Modifier) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(dimens.space1)) {
        markdown.lines().forEach { bruta ->
            val linha = bruta.trimEnd()
            when {
                linha.isBlank() -> Spacer(Modifier.height(dimens.space1))
                linha.startsWith("### ") -> Text(
                    text = inlineNegrito(linha.removePrefix("### ")),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                linha.startsWith("## ") -> Text(
                    text = inlineNegrito(linha.removePrefix("## ")),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.accent,
                )
                linha.startsWith("# ") -> Text(
                    text = inlineNegrito(linha.removePrefix("# ")),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                linha.startsWith("- ") || linha.startsWith("* ") -> Row {
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                    Spacer(Modifier.width(dimens.space1))
                    Text(
                        text = inlineNegrito(linha.drop(2)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                else -> Text(
                    text = inlineNegrito(linha),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Converte `**trecho**` em negrito; o resto vira texto simples. */
private fun inlineNegrito(texto: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < texto.length) {
        val abre = texto.indexOf("**", i)
        if (abre < 0) {
            append(texto.substring(i))
            break
        }
        append(texto.substring(i, abre))
        val fecha = texto.indexOf("**", abre + 2)
        if (fecha < 0) {
            append(texto.substring(abre))
            break
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(texto.substring(abre + 2, fecha))
        }
        i = fecha + 2
    }
}
