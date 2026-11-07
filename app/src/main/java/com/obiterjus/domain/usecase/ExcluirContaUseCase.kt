package com.obiterjus.domain.usecase

import com.obiterjus.data.settings.SessaoLocalRepository
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.SincronizacaoRepository
import kotlinx.coroutines.flow.first

/**
 * Exclusão da conta a pedido do titular, exigida pela Play Store para
 * aplicativo que cria conta e pelo art. 18 da LGPD.
 *
 * A ordem não é arbitrária: a nuvem primeiro, porque as regras do Firestore só
 * autorizam `users/{uid}` enquanto a sessão existe — apagar a conta antes
 * deixaria o acervo lá para sempre, sem titular que possa pedir sua remoção.
 * O aparelho por último, porque só depois de a conta ir embora o acervo local
 * perde qualquer razão de ser.
 */
class ExcluirContaUseCase(
    private val authRepository: AuthRepository,
    private val repositorioSincronizacao: SincronizacaoRepository,
    private val sessaoLocal: SessaoLocalRepository,
) {
    suspend operator fun invoke(senha: String): Resultado {
        val usuario = authRepository.currentUser.first()
            ?: return Resultado.SemSessao
        if (usuario.isAnonymous) return Resultado.SemSessao

        if (authRepository.temProvedorSenha()) {
            if (senha.isBlank()) return Resultado.SenhaInvalida
            authRepository.reautenticarComSenha(senha)
                .onFailure { return Resultado.SenhaInvalida }
        }

        repositorioSincronizacao.excluirTudo(usuario.uid)
            .onFailure { return Resultado.FalhaNaNuvem(it) }

        authRepository.excluirConta()
            .onFailure { return Resultado.FalhaNaConta(it) }

        sessaoLocal.limparTudo()
        return Resultado.Concluida
    }

    sealed interface Resultado {
        data object Concluida : Resultado

        /** Nenhuma conta para excluir: sessão anônima ou aparelho deslogado. */
        data object SemSessao : Resultado

        data object SenhaInvalida : Resultado

        /**
         * A conta continua de pé e o acervo na nuvem pode ter sido apagado em
         * parte. É seguro repetir: a exclusão é idempotente.
         */
        data class FalhaNaNuvem(val causa: Throwable) : Resultado

        /** A nuvem foi limpa, mas a conta de autenticação resistiu. */
        data class FalhaNaConta(val causa: Throwable) : Resultado
    }
}
