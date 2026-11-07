package com.obiterjus.domain.usecase

import com.obiterjus.data.settings.SessaoLocalRepository
import com.obiterjus.domain.model.AuthUser
import com.obiterjus.domain.model.SincronizacaoNuvemResumo
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.repository.SincronizacaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcluirContaUseCaseTest {

    @Test
    fun `apaga a nuvem antes da conta e limpa o aparelho no fim`() = runTest {
        val auth = AuthRepositoryFake()
        val sincronizacao = SincronizacaoRepositoryFake()
        val sessao = SessaoLocalRepositoryFake()
        val ordem = mutableListOf<String>()
        auth.ordem = ordem
        sincronizacao.ordem = ordem
        sessao.ordem = ordem

        val resultado = ExcluirContaUseCase(auth, sincronizacao, sessao)(SENHA)

        assertEquals(ExcluirContaUseCase.Resultado.Concluida, resultado)
        assertEquals(listOf("reautenticar", "nuvem", "conta", "local"), ordem)
        assertEquals("uid-1", sincronizacao.userIdExcluido)
    }

    @Test
    fun `senha errada nao chega a apagar coisa alguma`() = runTest {
        val auth = AuthRepositoryFake(reautenticacaoFalha = true)
        val sincronizacao = SincronizacaoRepositoryFake()
        val sessao = SessaoLocalRepositoryFake()

        val resultado = ExcluirContaUseCase(auth, sincronizacao, sessao)(SENHA)

        assertEquals(ExcluirContaUseCase.Resultado.SenhaInvalida, resultado)
        assertEquals(null, sincronizacao.userIdExcluido)
        assertEquals(false, auth.contaExcluida)
        assertEquals(false, sessao.limpou)
    }

    /**
     * Falhar ao limpar a nuvem não pode derrubar a conta: sem sessão, as regras
     * do Firestore trancam `users/{uid}` e o acervo fica lá para sempre.
     */
    @Test
    fun `falha na nuvem preserva a conta`() = runTest {
        val auth = AuthRepositoryFake()
        val sincronizacao = SincronizacaoRepositoryFake(falha = true)
        val sessao = SessaoLocalRepositoryFake()

        val resultado = ExcluirContaUseCase(auth, sincronizacao, sessao)(SENHA)

        assertTrue(resultado is ExcluirContaUseCase.Resultado.FalhaNaNuvem)
        assertEquals(false, auth.contaExcluida)
        assertEquals(false, sessao.limpou)
    }

    @Test
    fun `conta sem senha nao pede reautenticacao`() = runTest {
        val auth = AuthRepositoryFake(temSenha = false)
        val sincronizacao = SincronizacaoRepositoryFake()
        val sessao = SessaoLocalRepositoryFake()

        val resultado = ExcluirContaUseCase(auth, sincronizacao, sessao)("")

        assertEquals(ExcluirContaUseCase.Resultado.Concluida, resultado)
        assertEquals(false, auth.reautenticou)
    }

    @Test
    fun `sessao anonima nao tem conta a excluir`() = runTest {
        val auth = AuthRepositoryFake(anonimo = true)
        val sincronizacao = SincronizacaoRepositoryFake()
        val sessao = SessaoLocalRepositoryFake()

        val resultado = ExcluirContaUseCase(auth, sincronizacao, sessao)(SENHA)

        assertEquals(ExcluirContaUseCase.Resultado.SemSessao, resultado)
        assertEquals(null, sincronizacao.userIdExcluido)
    }

    private class AuthRepositoryFake(
        private val temSenha: Boolean = true,
        private val reautenticacaoFalha: Boolean = false,
        anonimo: Boolean = false,
    ) : AuthRepository {
        var ordem: MutableList<String> = mutableListOf()
        var reautenticou = false
        var contaExcluida = false

        override val currentUser: Flow<AuthUser?> = flowOf(
            AuthUser(uid = "uid-1", email = "advogado@exemplo.com", isAnonymous = anonimo),
        )

        override suspend fun temProvedorSenha(): Boolean = temSenha

        override suspend fun reautenticarComSenha(senha: String): Result<Unit> {
            reautenticou = true
            if (reautenticacaoFalha) return Result.failure(IllegalStateException("senha errada"))
            ordem += "reautenticar"
            return Result.success(Unit)
        }

        override suspend fun excluirConta(): Result<Unit> {
            contaExcluida = true
            ordem += "conta"
            return Result.success(Unit)
        }

        override suspend fun linkWithGoogle(idToken: String): Result<AuthUser> =
            Result.failure(NotImplementedError())

        override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> =
            Result.failure(NotImplementedError())

        override suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser> =
            Result.failure(NotImplementedError())

        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
            Result.failure(NotImplementedError())

        override suspend fun updatePassword(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> = Result.failure(NotImplementedError())

        override suspend fun signOut() = Unit
    }

    private class SincronizacaoRepositoryFake(
        private val falha: Boolean = false,
    ) : SincronizacaoRepository {
        var ordem: MutableList<String> = mutableListOf()
        var userIdExcluido: String? = null

        override suspend fun excluirTudo(userId: String): Result<Unit> {
            if (falha) return Result.failure(IllegalStateException("sem rede"))
            userIdExcluido = userId
            ordem += "nuvem"
            return Result.success(Unit)
        }

        override suspend fun nuvemAutorizada(): Boolean = true

        override suspend fun enviarTudo(userId: String): SincronizacaoNuvemResumo =
            SincronizacaoNuvemResumo()

        override suspend fun restaurarTudo(userId: String): SincronizacaoNuvemResumo =
            SincronizacaoNuvemResumo()

        override suspend fun enviarPerfil(userId: String): Result<Unit> = Result.success(Unit)

        override suspend fun restaurarPerfil(userId: String): Result<Unit> = Result.success(Unit)
    }

    private class SessaoLocalRepositoryFake : SessaoLocalRepository {
        var ordem: MutableList<String> = mutableListOf()
        var limpou = false

        override suspend fun prepararPara(uid: String) = Unit

        override suspend fun limparTudo() {
            limpou = true
            ordem += "local"
        }
    }

    private companion object {
        const val SENHA = "senha-correta"
    }
}
