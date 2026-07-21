package com.obiterjus.presentation.principal

import com.obiterjus.presentation.adicionarprocesso.AdicionarProcessoViewModel
import com.obiterjus.presentation.auditoria.AuditoriaViewModel
import com.obiterjus.presentation.autenticacao.AutenticacaoViewModel
import com.obiterjus.presentation.clientes.ClientesViewModel
import com.obiterjus.presentation.detalhecliente.DetalheClienteViewModel
import com.obiterjus.presentation.detalheprocesso.DetalheProcessoViewModel
import com.obiterjus.presentation.detalhepublicacao.DetalhePublicacaoViewModel
import com.obiterjus.presentation.editarcliente.EditarClienteViewModel
import com.obiterjus.presentation.editarprocesso.EditarProcessoViewModel
import com.obiterjus.presentation.inicio.InicioViewModel
import com.obiterjus.presentation.monitoramento.MonitoramentoViewModel
import com.obiterjus.presentation.perfil.PerfilViewModel
import com.obiterjus.presentation.processos.ProcessosViewModel
import com.obiterjus.presentation.prazos.PrazosViewModel
import com.obiterjus.presentation.publicacoes.PublicacoesViewModel

data class ObiterViewModels(
    val inicio: InicioViewModel,
    val publicacoes: PublicacoesViewModel,
    val prazos: PrazosViewModel,
    val processos: ProcessosViewModel,
    val perfil: PerfilViewModel,
    val autenticacao: AutenticacaoViewModel,
    val monitoramento: MonitoramentoViewModel,
    val detalheProcesso: DetalheProcessoViewModel,
    val detalhePublicacao: DetalhePublicacaoViewModel,
    val auditoria: AuditoriaViewModel,
    val adicionarProcesso: AdicionarProcessoViewModel,
    val editarProcesso: EditarProcessoViewModel,
    val clientes: ClientesViewModel,
    val detalheCliente: DetalheClienteViewModel,
    val editarCliente: EditarClienteViewModel,
)
