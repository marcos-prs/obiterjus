package com.obiterjus.presentation.componentes.cards

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.obiterjus.R
import com.obiterjus.core.texto.formatarCnj
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.presentation.publicacoes.prioridadeStripe
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.domain.model.tipoAto

@Composable
fun CardPublicacaoResumo(
    publicacao: Publicacao,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeOrdem: String? = null,
    onVerDetalhes: (() -> Unit)? = null,
){
    val tituloAto = publicacao.tipoComunicacao ?: stringResource(R.string.publicacoes_sem_tipo)
    val tipoAto = stringResource(publicacao.tipoAto.rotuloRes)
    val data = publicacao.dataDisponibilizacao?.let(FormatadorData::formatarData)
        ?: stringResource(R.string.publicacoes_sem_data)
    val tribunal = publicacao.tribunal ?: stringResource(R.string.publicacoes_sem_tribunal)
    val juizo = publicacao.nomeOrgao
    val numeroProcesso = publicacao.numeroProcesso?.formatarCnj()
        ?: stringResource(R.string.publicacoes_sem_numero_processo)
    val prazoDias = publicacao.prazo?.quantidade?.let { dias ->
        stringResource(com.obiterjus.R.string.prazos_badge_dias, dias)
    }
    val trechoTexto = publicacao.textoLimpo
    val prioridade = publicacao.prioridadeStripe()

    // Reutiliza CardPublicacao com menos dependências visuais para resumo
    CardPublicacao(
        tituloAto = tituloAto,
        tipoAto = tipoAto,
        data = data,
        tribunal = tribunal,
        juizo = juizo,
        numeroProcesso = numeroProcesso,
        prazoDias = prazoDias,
        trechoTexto = trechoTexto,
        prioridade = prioridade,
        aoClicar = onClick,
        onVerDetalhes = onVerDetalhes ?: onClick,
        mostrarBotaoDetalhes = true,
        modifier = modifier,
        badgeOrdem = badgeOrdem,
        confianca = publicacao.confiancaMatch,
    )
}
