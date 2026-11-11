package com.obiterjus.data.push

import com.obiterjus.core.security.CofreLocal

/**
 * O pouco de estado que o registro de push guarda entre aberturas.
 *
 * Existe como interface para o [PushRegistroRepository] ser testável: o
 * [CofreLocal] depende de Context e do Android Keystore, que não existem em
 * teste unitário.
 */
interface MemoriaPush {
    fun instalacao(): String?

    fun guardarInstalacao(id: String)

    /** Tópicos em que o aparelho está inscrito hoje. */
    fun topicos(): Set<String>

    fun guardarTopicos(topicos: Set<String>)
}

class CofreMemoriaPush(
    private val cofre: CofreLocal,
) : MemoriaPush {

    override fun instalacao(): String? =
        cofre.ler(CofreLocal.SEGREDO_INSTALACAO_PUSH)?.takeIf { it.isNotBlank() }

    override fun guardarInstalacao(id: String) {
        cofre.guardar(CofreLocal.SEGREDO_INSTALACAO_PUSH, id)
    }

    override fun topicos(): Set<String> =
        cofre.ler(CofreLocal.SEGREDO_TOPICOS_PUSH)
            ?.split(SEPARADOR)
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()

    override fun guardarTopicos(topicos: Set<String>) {
        cofre.guardar(
            CofreLocal.SEGREDO_TOPICOS_PUSH,
            topicos.takeIf { it.isNotEmpty() }?.joinToString(SEPARADOR),
        )
    }

    private companion object {
        const val SEPARADOR = ","
    }
}
