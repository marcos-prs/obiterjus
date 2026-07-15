package com.obiterjus.core.parser

import com.obiterjus.domain.model.PublicacaoParticipante

object DjenParticipantesExtractor {
    private val labelRegex = Regex(
        pattern = """(?i)\b(destinat[aá]rios?|advogad[oa]s?|adv\.?|polo\s+ativo|polo\s+passivo|parte(?:s)?|autor(?:a)?|r[eé]u|requerente|requerido|exequente|executado|impetrante|impetrado|agravante|agravado|apelante|apelado)\s*[:\-]\s*([^\n;]+)""",
    )
    private val oabRegex = Regex("""(?i)\bOAB\s*(?::|/)?\s*([A-Z]{2})?\s*(\d{2,8}[A-Z]?)(?:\s*/\s*([A-Z]{2}))?\b""")

    fun extract(texto: String?): List<PublicacaoParticipante> {
        if (texto.isNullOrBlank()) return emptyList()

        return labelRegex.findAll(texto)
            .mapNotNull { match ->
                val tipo = match.groupValues[1].normalizeTipo()
                val rawNome = match.groupValues[2]
                val documento = oabRegex.find(rawNome)?.value?.uppercase()
                val nome = rawNome
                    .replace(oabRegex, "")
                    .replace(Regex("""(?i)\bCPF\s*:?\s*[\d.*-]+"""), "")
                    .trim(' ', '-', ',', ':')
                    .takeIf { it.length >= MIN_NOME_LENGTH }

                nome?.let {
                    PublicacaoParticipante(
                        tipo = tipo,
                        nome = it,
                        documento = documento,
                    )
                }
            }
            .distinctBy { participante ->
                "${participante.tipo}|${participante.nome.uppercase()}|${participante.documento.orEmpty()}"
            }
            .toList()
    }

    private fun String.normalizeTipo(): String {
        val normalized = lowercase()
        return when {
            normalized.startsWith("destinat") -> "Destinatário"
            normalized.startsWith("adv") -> "Advogado"
            normalized.startsWith("polo ativo") -> "Ativo"
            normalized.startsWith("polo passivo") -> "Passivo"
            normalized.startsWith("autor") -> "Autor"
            normalized == "réu" || normalized == "reu" -> "Réu"
            normalized.startsWith("requerente") -> "Requerente"
            normalized.startsWith("requerido") -> "Requerido"
            normalized.startsWith("exequente") -> "Exequente"
            normalized.startsWith("executado") -> "Executado"
            normalized.startsWith("impetrante") -> "Impetrante"
            normalized.startsWith("impetrado") -> "Impetrado"
            normalized.startsWith("agravante") -> "Agravante"
            normalized.startsWith("agravado") -> "Agravado"
            normalized.startsWith("apelante") -> "Apelante"
            normalized.startsWith("apelado") -> "Apelado"
            else -> "Parte"
        }
    }

    private const val MIN_NOME_LENGTH = 3
}
