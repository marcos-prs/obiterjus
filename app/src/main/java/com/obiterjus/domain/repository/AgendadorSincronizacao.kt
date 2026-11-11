package com.obiterjus.domain.repository

/**
 * Abstrai o agendamento da sincronização diária para quem decide sobre ela.
 *
 * Existe para que a preferência `sincronizacaoAutomatica` e o agendamento nunca
 * possam divergir: ver [com.obiterjus.domain.usecase.AplicarPreferenciaSincronizacaoUC].
 *
 * A distinção entre [garantirDiaria] e [agendarDiaria] é a correção do bug de
 * 2026-07-15 e precisa ser preservada — o detalhe está em
 * [com.obiterjus.core.worker.DjenSyncScheduler].
 */
interface AgendadorSincronizacao {
    /**
     * Garante que existe agendamento, sem mexer no horário já ancorado (KEEP).
     * É o que a inicialização do app deve chamar.
     */
    fun garantirDiaria()

    /**
     * Agenda re-ancorando no próximo horário alvo (UPDATE). Só ao final de uma
     * execução do worker ou quando o usuário liga a sincronização automática.
     */
    fun agendarDiaria()

    fun cancelarDiaria()
}
