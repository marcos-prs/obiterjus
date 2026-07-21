package com.obiterjus.presentation.participantes

import java.text.Normalizer
import java.util.Locale

/** Grupo de polo ao qual um papel processual pertence. */
enum class GrupoPolo { ADVOGADO, ATIVO, PASSIVO, OUTRO }

/** Definição de um papel/tipo de participação processual. */
data class TipoParticipacaoDef(
    val rotulo: String,
    val grupo: GrupoPolo,
)

/**
 * Catálogo de papéis processuais e fonte única para mapear um papel (ou um polo
 * cru vindo da captura, ex.: "AT"/"ATIVO") ao seu [GrupoPolo]. Usado tanto pelo
 * seletor de edição quanto pela resolução de partes, advogados e clientes.
 *
 * O mapeamento papel→polo é um default sensato: papéis recursais (recorrente/
 * recorrido, apelante/apelado…) podem divergir do polo de origem — o usuário
 * pode ajustar via "Outro (especificar)".
 */
object TiposParticipacao {

    // Declarados antes de qualquer uso: [normalizar] é chamado já na
    // inicialização de [porRotulo], então estes Regex precisam existir primeiro.
    private val MARCAS_DIACRITICAS = Regex("""\p{M}+""")
    private val ESPACOS = Regex("""\s+""")

    val advogado = TipoParticipacaoDef("Advogado", GrupoPolo.ADVOGADO)

    val ativos: List<TipoParticipacaoDef> = listOf(
        "Autor", "Requerente", "Exequente", "Embargante", "Apelante", "Agravante",
        "Recorrente", "Impetrante", "Querelante", "Denunciante", "Reclamante",
        "Alimentante", "Suscitante", "Excipiente", "Chamante", "Opoente",
        "Inventariante", "Interditante",
    ).map { TipoParticipacaoDef(it, GrupoPolo.ATIVO) }

    val passivos: List<TipoParticipacaoDef> = listOf(
        "Réu", "Requerido", "Executado", "Embargado", "Apelado", "Agravado",
        "Recorrido", "Impetrado", "Paciente", "Indiciado", "Denunciado",
        "Querelado", "Reclamado", "Alimentado", "Suscitado", "Excepto",
        "Chamado", "Oposto",
    ).map { TipoParticipacaoDef(it, GrupoPolo.PASSIVO) }

    val outros: List<TipoParticipacaoDef> = listOf(
        "Vítima", "Assistente", "Amicus curiae", "Terceiro interessado",
        "Herdeiro", "Legatário", "Inventariado", "Tutor", "Curador",
    ).map { TipoParticipacaoDef(it, GrupoPolo.OUTRO) }

    /** Todos os papéis, na ordem de exibição (advogado isolado no topo). */
    val todos: List<TipoParticipacaoDef> = listOf(advogado) + ativos + passivos + outros

    private val porRotulo: Map<String, TipoParticipacaoDef> =
        todos.associateBy { normalizar(it.rotulo).orEmpty() }

    /** Papel correspondente ao rótulo informado, se catalogado. */
    fun definicao(valor: String?): TipoParticipacaoDef? =
        normalizar(valor)?.let { porRotulo[it] }

    /**
     * Grupo de um valor que pode ser um papel catalogado (ex.: "Autor") ou um
     * polo cru vindo da captura (ex.: "AT", "ATIVO", "Polo passivo"). Retorna
     * null quando não é possível classificar.
     */
    fun grupoDeTipoOuPolo(valor: String?): GrupoPolo? {
        if (valor.isNullOrBlank()) return null
        definicao(valor)?.let { return it.grupo }
        return when (poloBase(valor)) {
            "ATIVO" -> GrupoPolo.ATIVO
            "PASSIVO" -> GrupoPolo.PASSIVO
            else -> null
        }
    }

    /** Polo ("ATIVO"/"PASSIVO") derivado de um papel; null p/ advogado/outro. */
    fun poloDerivado(valor: String?): String? =
        when (definicao(valor)?.grupo) {
            GrupoPolo.ATIVO -> "ATIVO"
            GrupoPolo.PASSIVO -> "PASSIVO"
            else -> null
        }

    /**
     * Rótulo de exibição da espécie (papel) para o cabeçalho do polo, ex.:
     * "Agravante". Retorna null quando o valor é apenas um polo cru (ATIVO/AT…)
     * ou está em branco — nesse caso não há espécie distinta a exibir.
     */
    fun rotuloEspecie(valor: String?): String? {
        if (valor.isNullOrBlank()) return null
        definicao(valor)?.let { return it.rotulo }
        if (poloBase(valor) != null) return null
        return valor.trim().lowercase(Locale.ROOT)
            .replaceFirstChar { it.titlecase(Locale.ROOT) }
    }

    /** Heurística de advogado a partir do tipo de participação. */
    fun ehAdvogadoTipo(valor: String?): Boolean {
        val n = normalizar(valor) ?: return false
        return n.startsWith("advogad") || n.startsWith("procurador")
    }

    /** Minúsculas, sem acentos e com espaços colapsados; null se em branco. */
    fun normalizar(valor: String?): String? {
        if (valor.isNullOrBlank()) return null
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
            .replace(MARCAS_DIACRITICAS, "")
            .lowercase(Locale.ROOT)
            .replace(ESPACOS, " ")
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    private fun poloBase(valor: String?): String? =
        when (normalizar(valor)) {
            "a", "at", "ativo", "ativa", "polo ativo" -> "ATIVO"
            "p", "pa", "passivo", "passiva", "polo passivo" -> "PASSIVO"
            else -> null
        }
}
