package com.obiterjus.domain.repository

import com.obiterjus.domain.model.SincronizacaoNuvemResumo

interface SincronizacaoRepository {
    /**
     * Se o advogado autorizou o envio do acervo para a nuvem. Vale para o envio
     * automático que segue a sincronização; o botão do Perfil é ação explícita
     * e não passa por aqui.
     */
    suspend fun nuvemAutorizada(): Boolean

    suspend fun enviarTudo(userId: String): SincronizacaoNuvemResumo

    suspend fun restaurarTudo(userId: String): SincronizacaoNuvemResumo

    suspend fun enviarPerfil(userId: String): Result<Unit>

    suspend fun restaurarPerfil(userId: String): Result<Unit>

    /**
     * Apaga tudo o que a conta tem na nuvem, inclusive o documento do usuário.
     * Tem de rodar com a sessão ainda ativa: as regras do Firestore só liberam
     * `users/{uid}` para o próprio uid autenticado.
     */
    suspend fun excluirTudo(userId: String): Result<Unit>
}
