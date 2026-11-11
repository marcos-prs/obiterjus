package com.obiterjus.domain.usecase

import com.obiterjus.data.settings.PerfilPreferencesRepository
import com.obiterjus.domain.repository.AgendadorSincronizacao

/**
 * Único caminho autorizado para mudar `sincronizacaoAutomatica`.
 *
 * Antes desta classe, três pontos gravavam a preferência e só um falava com o
 * agendador: o toggle do Perfil. A etapa `PREFERENCIAS` do cadastro
 * ([com.obiterjus.presentation.autenticacao.AutenticacaoViewModel]) e a
 * restauração da nuvem
 * ([com.obiterjus.data.sincronizacao.FirestoreSincronizacaoRepository])
 * gravavam e seguiam adiante.
 *
 * A consequência era silenciosa e difícil de achar no aparelho: se a nuvem
 * restaurasse `false`, o próximo `ObiterJusApplication.onCreate` leria a
 * preferência e chamaria `cancelarDiaria()`. O job das 7h deixava de existir, e
 * nada no app dizia por quê — não há nem entrada em `sync_logs`, porque o worker
 * nunca roda.
 *
 * Por isso a gravação e o agendamento acontecem aqui, juntos, numa só operação.
 * Quem precisar mexer nessa preferência passa por aqui.
 */
class AplicarPreferenciaSincronizacaoUC(
    private val perfilPreferencesRepository: PerfilPreferencesRepository,
    private val agendador: AgendadorSincronizacao,
) {
    suspend operator fun invoke(ativo: Boolean) {
        perfilPreferencesRepository.saveSincronizacaoAutomatica(ativo)
        if (ativo) {
            agendador.agendarDiaria()
        } else {
            agendador.cancelarDiaria()
        }
    }
}
