package com.obiterjus.presentation.principal

import com.obiterjus.presentation.auditoria.AuditoriaViewModel
import com.obiterjus.presentation.autenticacao.ModeloAutenticacao
import com.obiterjus.presentation.detalheprocesso.ModeloDetalheProcesso
import com.obiterjus.presentation.inicio.ModeloInicio
import com.obiterjus.presentation.monitoramento.MonitoramentoViewModel
import com.obiterjus.presentation.perfil.ModeloPerfil
import com.obiterjus.presentation.processos.ModeloProcessos
import com.obiterjus.presentation.prazos.ModeloPrazos
import com.obiterjus.presentation.publicacoes.PublicacoesViewModel

data class ObiterViewModels(
    val inicio: ModeloInicio,
    val publicacoes: PublicacoesViewModel,
    val prazos: ModeloPrazos,
    val processos: ModeloProcessos,
    val perfil: ModeloPerfil,
    val autenticacao: ModeloAutenticacao,
    val monitoramento: MonitoramentoViewModel,
    val detalheProcesso: ModeloDetalheProcesso,
    val auditoria: AuditoriaViewModel,
)
