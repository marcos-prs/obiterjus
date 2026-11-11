@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.obiterjus.presentation.minuta

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obiterjus.R
import com.obiterjus.core.texto.formatarCnj
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.domain.model.CampoParte
import com.obiterjus.domain.model.EstruturaPeca
import com.obiterjus.domain.model.FatoPeca
import com.obiterjus.domain.model.MinutaSugestao
import com.obiterjus.domain.model.PedidoPeca
import com.obiterjus.domain.model.TesePeca
import com.obiterjus.domain.model.TipoMinuta
import com.obiterjus.domain.model.TipoPecaOrigem
import com.obiterjus.presentation.componentes.EstadoVazioObiter
import com.obiterjus.presentation.componentes.ObiterIcones
import com.obiterjus.presentation.componentes.barras.BarraSuperiorSecundaria
import com.obiterjus.ui.theme.ObiterTheme

@Composable
fun MinutaScreen(
    viewModel: MinutaViewModel,
    numeroProcesso: String,
    onVoltar: () -> Unit,
    aoEntrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(numeroProcesso) { viewModel.aoCarregar(numeroProcesso) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val dimens = ObiterTheme.dimens

    fun aoVoltar() {
        when (estado.etapa) {
            EtapaMinuta.LISTA -> onVoltar()
            EtapaMinuta.CONFIGURAR -> viewModel.voltarParaRevisar()
            EtapaMinuta.GERANDO, EtapaMinuta.EXTRAINDO -> Unit // não interrompe operação em curso
            else -> viewModel.voltarParaLista()
        }
    }
    BackHandler { aoVoltar() }

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperiorSecundaria(
            titulo = tituloEtapa(estado.etapa),
            subtitulo = numeroProcesso.formatarCnj(),
            onVoltar = ::aoVoltar,
        )

        estado.erro?.let { erro ->
            BannerErro(
                mensagem = erro,
                aoDescartar = viewModel::descartarErro,
                aoEntrar = if (estado.erroExigeLogin) {
                    {
                        // Descarta o aviso antes de sair: ao voltar da autenticação a
                        // tela não deve reabrir com um erro de sessão já resolvido.
                        viewModel.descartarErro()
                        aoEntrar()
                    }
                } else {
                    null
                },
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(horizontal = dimens.screenMargin)) {
            when (estado.etapa) {
                EtapaMinuta.LISTA -> EtapaLista(estado, viewModel)
                EtapaMinuta.UPLOAD -> EtapaUpload(estado, viewModel)
                EtapaMinuta.EXTRAINDO -> EtapaCarregando(
                    titulo = stringResource(R.string.minuta_extraindo_titulo),
                    detalhe = estado.progressoStage?.let { rotuloStage(it) }
                        ?: stringResource(R.string.minuta_extraindo_enviando),
                    pct = estado.progressoPct,
                )
                EtapaMinuta.REVISAR -> EtapaRevisar(estado, viewModel)
                EtapaMinuta.CONFIGURAR -> EtapaConfigurar(estado, viewModel)
                EtapaMinuta.GERANDO -> EtapaCarregando(
                    titulo = stringResource(R.string.minuta_gerando_titulo),
                    detalhe = stringResource(R.string.minuta_gerando_detalhe),
                    pct = 0,
                )
                EtapaMinuta.RESULTADO -> estado.sugestao?.let { EtapaResultado(it, viewModel) }
            }
        }
    }
}

/* ---------- LISTA ---------- */

@Composable
private fun EtapaLista(estado: EstadoMinuta, viewModel: MinutaViewModel) {
    val dimens = ObiterTheme.dimens
    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = viewModel::iniciarNova,
            modifier = Modifier.fillMaxWidth().padding(top = dimens.cardGap),
        ) {
            Icon(ObiterIcones.Adicionar, contentDescription = null, modifier = Modifier.size(dimens.iconStarSize))
            Spacer(Modifier.width(dimens.space1))
            Text(stringResource(R.string.minuta_nova))
        }

        if (!estado.carregandoLista && estado.minutasSalvas.isEmpty()) {
            EstadoVazioObiter(
                titulo = stringResource(R.string.minuta_vazio_titulo),
                corpo = stringResource(R.string.minuta_vazio_corpo),
                icone = ObiterIcones.Documento,
                modifier = Modifier.padding(top = dimens.sectionGap),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = dimens.cardGap, bottom = dimens.screenMargin),
                verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
            ) {
                items(estado.minutasSalvas, key = { it.sugestaoId }) { minuta ->
                    CardMinutaSalva(
                        minuta = minuta,
                        aoAbrir = { viewModel.abrirMinutaSalva(minuta.sugestaoId) },
                        aoExcluir = { viewModel.excluirMinuta(minuta.sugestaoId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CardMinutaSalva(
    minuta: MinutaSugestao,
    aoAbrir: () -> Unit,
    aoExcluir: () -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    CartaoObiter(onClick = aoAbrir) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = minuta.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(dimens.space1))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.space1),
                ) {
                    Text(
                        text = minuta.tipoMinuta.rotulo,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent,
                    )
                    Text("·", color = colors.textMuted)
                    Text(
                        text = FormatadorData.formatarDataHora(minuta.geradoEm),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                    )
                }
            }
            BadgeConfianca(minuta.confiancaGeral)
            IconButton(onClick = aoExcluir) {
                Icon(ObiterIcones.Excluir, contentDescription = stringResource(R.string.minuta_cd_excluir), tint = colors.danger)
            }
        }
    }
}

/* ---------- UPLOAD ---------- */

@Composable
private fun EtapaUpload(estado: EstadoMinuta, viewModel: MinutaViewModel) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val seletorPdf = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::aoSelecionarArquivo) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = dimens.cardGap, bottom = dimens.screenMargin),
        verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
    ) {
        item {
            Text(
                stringResource(R.string.minuta_upload_passo1),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        item {
            OutlinedButton(
                onClick = { seletorPdf.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(ObiterIcones.Pasta, contentDescription = null, modifier = Modifier.size(dimens.iconStarSize))
                Spacer(Modifier.width(dimens.space1))
                Text(estado.arquivoNome ?: stringResource(R.string.minuta_upload_escolher))
            }
        }
        item {
            Text(
                stringResource(R.string.minuta_upload_passo2),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = dimens.space1),
            )
        }
        item {
            LinhaChips {
                TipoPecaOrigem.entries.forEach { tipo ->
                    ChipSelecionavel(
                        rotulo = tipo.rotulo,
                        selecionado = estado.tipoDeclarado == tipo,
                        onClick = { viewModel.aoSelecionarTipoDeclarado(tipo) },
                    )
                }
            }
        }
        item {
            Button(
                onClick = viewModel::extrair,
                enabled = estado.arquivoUri != null,
                modifier = Modifier.fillMaxWidth().padding(top = dimens.space1),
            ) {
                Text(stringResource(R.string.minuta_upload_extrair))
                Spacer(Modifier.width(dimens.space1))
                Icon(ObiterIcones.Proximo, contentDescription = null, modifier = Modifier.size(dimens.iconStarSize))
            }
            Text(
                stringResource(R.string.minuta_upload_aviso_tempo),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                modifier = Modifier.padding(top = dimens.space1),
            )
        }
    }
}

/* ---------- EXTRAINDO / GERANDO ---------- */

@Composable
private fun EtapaCarregando(titulo: String, detalhe: String, pct: Int) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = colors.accent)
        Spacer(Modifier.height(dimens.cardGap))
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(dimens.space1))
        Text(detalhe, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        if (pct in 1..100) {
            Spacer(Modifier.height(dimens.cardGap))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.sectionGap),
                color = colors.accent,
            )
            Spacer(Modifier.height(dimens.space1))
            Text("$pct%", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
        }
    }
}

/* ---------- REVISAR ---------- */

@Composable
private fun EtapaRevisar(estado: EstadoMinuta, viewModel: MinutaViewModel) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val estrutura = estado.estrutura ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = dimens.cardGap, bottom = dimens.screenMargin),
        verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
    ) {
        item {
            Text(
                stringResource(R.string.minuta_revisar_intro),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
        estado.pacote?.qualidade?.lacunas?.takeIf { it.isNotEmpty() }?.let { lacunas ->
            item { CartaoAviso(titulo = stringResource(R.string.minuta_revisar_lacunas), itens = lacunas) }
        }
        secaoRevisaoEditavel(estrutura, viewModel::aoAtualizarEstrutura)
        item {
            Button(onClick = viewModel::irParaConfigurar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.minuta_revisar_avancar))
                Spacer(Modifier.width(dimens.space1))
                Icon(ObiterIcones.Proximo, contentDescription = null, modifier = Modifier.size(dimens.iconStarSize))
            }
        }
    }
}

private fun LazyListScope.secaoRevisaoEditavel(
    estrutura: EstruturaPeca,
    onAtualizar: (EstruturaPeca) -> Unit,
) {
    // Partes — sempre exibidas; permitem preencher quando não foram extraídas.
    item {
        CampoParteEditavel(
            rotulo = stringResource(R.string.minuta_revisar_requerente),
            valor = estrutura.partes.requerente?.valor.orEmpty(),
            confianca = estrutura.partes.requerente?.confianca,
            onChange = { novo ->
                onAtualizar(
                    estrutura.copy(
                        partes = estrutura.partes.copy(
                            requerente = novo.paraCampoParte(estrutura.partes.requerente),
                        ),
                    ),
                )
            },
        )
    }
    item {
        CampoParteEditavel(
            rotulo = stringResource(R.string.minuta_revisar_requerido),
            valor = estrutura.partes.requerido?.valor.orEmpty(),
            confianca = estrutura.partes.requerido?.confianca,
            onChange = { novo ->
                onAtualizar(
                    estrutura.copy(
                        partes = estrutura.partes.copy(
                            requerido = novo.paraCampoParte(estrutura.partes.requerido),
                        ),
                    ),
                )
            },
        )
    }

    if (estrutura.preliminares.isNotEmpty()) {
        item { CabecalhoSecao(stringResource(R.string.minuta_revisar_preliminares, estrutura.preliminares.size)) }
        itemsIndexed(estrutura.preliminares) { i, prelim ->
            ItemEstruturaEditavel(
                rotulo = prelim.titulo,
                valor = prelim.texto,
                confianca = prelim.confianca,
                onChange = { novo ->
                    onAtualizar(estrutura.copy(preliminares = estrutura.preliminares.substituir(i, prelim.copy(texto = novo, confianca = 1.0))))
                },
                onRemover = { onAtualizar(estrutura.copy(preliminares = estrutura.preliminares.remover(i))) },
            )
        }
    }

    item { CabecalhoSecao(stringResource(R.string.minuta_revisar_fatos, estrutura.fatos.size)) }
    itemsIndexed(estrutura.fatos) { i, fato ->
        ItemEstruturaEditavel(
            rotulo = stringResource(R.string.minuta_revisar_fato, fato.sequencia),
            valor = fato.texto,
            confianca = fato.confianca,
            onChange = { novo ->
                onAtualizar(estrutura.copy(fatos = estrutura.fatos.substituir(i, fato.copy(texto = novo, confianca = 1.0))))
            },
            onRemover = { onAtualizar(estrutura.copy(fatos = estrutura.fatos.remover(i))) },
        )
    }
    item {
        BotaoAdicionar(stringResource(R.string.minuta_revisar_add_fato)) {
            val proxima = (estrutura.fatos.maxOfOrNull { it.sequencia } ?: 0) + 1
            onAtualizar(estrutura.copy(fatos = estrutura.fatos + FatoPeca(sequencia = proxima, texto = "", confianca = 1.0)))
        }
    }

    item { CabecalhoSecao(stringResource(R.string.minuta_revisar_teses, estrutura.teses.size)) }
    itemsIndexed(estrutura.teses) { i, tese ->
        ItemEstruturaEditavel(
            rotulo = tese.titulo,
            valor = tese.texto,
            confianca = tese.confianca,
            chips = tese.fundamentoLegal,
            onChange = { novo ->
                onAtualizar(estrutura.copy(teses = estrutura.teses.substituir(i, tese.copy(texto = novo, confianca = 1.0))))
            },
            onRemover = { onAtualizar(estrutura.copy(teses = estrutura.teses.remover(i))) },
        )
    }
    item {
        BotaoAdicionar(stringResource(R.string.minuta_revisar_add_tese)) {
            onAtualizar(estrutura.copy(teses = estrutura.teses + TesePeca(titulo = "Nova tese", texto = "", confianca = 1.0)))
        }
    }

    item { CabecalhoSecao(stringResource(R.string.minuta_revisar_pedidos, estrutura.pedidos.size)) }
    itemsIndexed(estrutura.pedidos) { i, pedido ->
        ItemEstruturaEditavel(
            rotulo = pedido.tipo.ifBlank { "Pedido" },
            valor = pedido.texto,
            confianca = pedido.confianca,
            onChange = { novo ->
                onAtualizar(estrutura.copy(pedidos = estrutura.pedidos.substituir(i, pedido.copy(texto = novo, confianca = 1.0))))
            },
            onRemover = { onAtualizar(estrutura.copy(pedidos = estrutura.pedidos.remover(i))) },
        )
    }
    item {
        BotaoAdicionar(stringResource(R.string.minuta_revisar_add_pedido)) {
            onAtualizar(estrutura.copy(pedidos = estrutura.pedidos + PedidoPeca(tipo = "Pedido", texto = "", confianca = 1.0)))
        }
    }

    if (estrutura.provas.isNotEmpty()) {
        item { CabecalhoSecao(stringResource(R.string.minuta_revisar_provas, estrutura.provas.size)) }
        itemsIndexed(estrutura.provas) { i, prova ->
            ItemEstruturaEditavel(
                rotulo = prova.tipo,
                valor = prova.descricao,
                confianca = prova.confianca,
                onChange = { novo ->
                    onAtualizar(estrutura.copy(provas = estrutura.provas.substituir(i, prova.copy(descricao = novo, confianca = 1.0))))
                },
                onRemover = { onAtualizar(estrutura.copy(provas = estrutura.provas.remover(i))) },
            )
        }
    }
}

/** Converte o texto editado em [CampoParte]; mantém nulo se vazio e inexistente. */
private fun String.paraCampoParte(anterior: CampoParte?): CampoParte? =
    if (isBlank() && anterior == null) null
    else CampoParte(valor = this, confianca = 1.0, origemTrecho = anterior?.origemTrecho)

private fun <T> List<T>.substituir(index: Int, novo: T): List<T> =
    toMutableList().also { it[index] = novo }

private fun <T> List<T>.remover(index: Int): List<T> =
    toMutableList().also { it.removeAt(index) }

@Composable
private fun CampoParteEditavel(
    rotulo: String,
    valor: String,
    confianca: Double?,
    onChange: (String) -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    CartaoObiter {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = rotulo,
                style = MaterialTheme.typography.labelMedium,
                color = colors.accent,
                modifier = Modifier.weight(1f),
            )
            confianca?.let { BadgeConfianca(it) }
        }
        Spacer(Modifier.height(dimens.space1))
        OutlinedTextField(
            value = valor,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall,
            placeholder = { Text(stringResource(R.string.minuta_revisar_parte_vazia)) },
        )
    }
}

@Composable
private fun ItemEstruturaEditavel(
    rotulo: String,
    valor: String,
    confianca: Double,
    onChange: (String) -> Unit,
    onRemover: () -> Unit,
    chips: List<String> = emptyList(),
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    CartaoObiter {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = rotulo,
                style = MaterialTheme.typography.labelMedium,
                color = colors.accent,
                modifier = Modifier.weight(1f),
            )
            BadgeConfianca(confianca)
            IconButton(onClick = onRemover) {
                Icon(
                    ObiterIcones.Excluir,
                    contentDescription = stringResource(R.string.minuta_revisar_cd_remover),
                    tint = colors.danger,
                    modifier = Modifier.size(dimens.iconStarSize),
                )
            }
        }
        Spacer(Modifier.height(dimens.space1))
        OutlinedTextField(
            value = valor,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall,
        )
        if (chips.isNotEmpty()) {
            Spacer(Modifier.height(dimens.space1))
            LinhaChips { chips.forEach { ChipEstatico(it) } }
        }
    }
}

@Composable
private fun BotaoAdicionar(texto: String, onClick: () -> Unit) {
    val dimens = ObiterTheme.dimens
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(ObiterIcones.Adicionar, contentDescription = null, modifier = Modifier.size(dimens.iconStarSize))
        Spacer(Modifier.width(dimens.space1))
        Text(texto)
    }
}

/* ---------- CONFIGURAR ---------- */

@Composable
private fun EtapaConfigurar(estado: EstadoMinuta, viewModel: MinutaViewModel) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = dimens.cardGap, bottom = dimens.screenMargin),
        verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
    ) {
        item {
            Text(
                stringResource(R.string.minuta_config_tipo),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        item {
            LinhaChips {
                TipoMinuta.entries.forEach { tipo ->
                    ChipSelecionavel(
                        rotulo = tipo.rotulo,
                        selecionado = estado.tipoMinuta == tipo,
                        onClick = { viewModel.aoSelecionarTipoMinuta(tipo) },
                    )
                }
            }
        }
        item {
            Text(
                stringResource(R.string.minuta_config_orientacoes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = dimens.space1),
            )
        }
        item {
            OutlinedTextField(
                value = estado.orientacoes,
                onValueChange = viewModel::aoEditarOrientacoes,
                modifier = Modifier.fillMaxWidth().height(140.dp),
                placeholder = { Text(stringResource(R.string.minuta_config_orientacoes_hint)) },
                supportingText = { Text(stringResource(R.string.minuta_config_contador, estado.orientacoes.length)) },
            )
        }
        item {
            Button(onClick = viewModel::gerar, modifier = Modifier.fillMaxWidth()) {
                Icon(ObiterIcones.Artigo, contentDescription = null, modifier = Modifier.size(dimens.iconStarSize))
                Spacer(Modifier.width(dimens.space1))
                Text(stringResource(R.string.minuta_config_gerar))
            }
            Text(
                stringResource(R.string.minuta_config_aviso_revisao),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                modifier = Modifier.padding(top = dimens.space1),
            )
        }
    }
}

/* ---------- RESULTADO ---------- */

@Composable
private fun EtapaResultado(sugestao: MinutaSugestao, viewModel: MinutaViewModel) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = dimens.cardGap, bottom = dimens.screenMargin),
        verticalArrangement = Arrangement.spacedBy(dimens.cardGap),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sugestao.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                BadgeConfianca(sugestao.confiancaGeral)
            }
        }
        if (sugestao.requerRevisao) {
            item {
                CartaoAviso(
                    titulo = stringResource(R.string.minuta_resultado_requer_revisao_titulo),
                    itens = listOf(stringResource(R.string.minuta_resultado_requer_revisao_corpo)),
                )
            }
        }
        if (sugestao.pontosAtencao.isNotEmpty()) {
            item { CartaoAviso(titulo = stringResource(R.string.minuta_resultado_pontos_atencao), itens = sugestao.pontosAtencao) }
        }
        if (sugestao.lacunas.isNotEmpty()) {
            item { CartaoAviso(titulo = stringResource(R.string.minuta_resultado_lacunas), itens = sugestao.lacunas) }
        }
        if (sugestao.secoes.isNotEmpty()) {
            item { CabecalhoSecao(stringResource(R.string.minuta_resultado_secoes)) }
            items(sugestao.secoes) { secao ->
                CartaoObiter {
                    Text(secao.titulo, style = MaterialTheme.typography.titleSmall, color = colors.accent)
                    Spacer(Modifier.height(dimens.space1))
                    Text(secao.texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    if (secao.fundamentoLegal.isNotEmpty()) {
                        Spacer(Modifier.height(dimens.space1))
                        LinhaChips { secao.fundamentoLegal.forEach { ChipEstatico(it) } }
                    }
                }
            }
        } else {
            item {
                CartaoObiter {
                    TextoMarkdownLeve(sugestao.markdown)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.cardGap)) {
                OutlinedButton(
                    onClick = { compartilharMinuta(context, sugestao) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(ObiterIcones.Compartilhar, contentDescription = null, modifier = Modifier.size(dimens.iconStarSize))
                    Spacer(Modifier.width(dimens.space1))
                    Text(stringResource(R.string.minuta_resultado_compartilhar))
                }
                Button(onClick = viewModel::voltarParaLista, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.minuta_resultado_concluir))
                }
            }
        }
    }
}

private fun compartilharMinuta(context: android.content.Context, sugestao: MinutaSugestao) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, sugestao.titulo)
        putExtra(Intent.EXTRA_TEXT, sugestao.markdown)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.minuta_compartilhar_titulo)),
    )
}

/* ---------- Blocos reutilizáveis ---------- */

@Composable
private fun CartaoObiter(
    onClick: (() -> Unit)? = null,
    conteudo: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(dimens.cardRadius),
        modifier = Modifier
            .fillMaxWidth()
            .border(dimens.borderWidth, colors.divider, RoundedCornerShape(dimens.cardRadius))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(modifier = Modifier.padding(dimens.cardPaddingH), content = conteudo)
    }
}

@Composable
private fun CartaoAviso(titulo: String, itens: List<String>) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    Surface(
        color = colors.warningPale,
        shape = RoundedCornerShape(dimens.cardRadius),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(dimens.cardPaddingH)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(ObiterIcones.Aviso, contentDescription = null, tint = colors.warning, modifier = Modifier.size(dimens.iconStarSize))
                Spacer(Modifier.width(dimens.space1))
                Text(titulo, style = MaterialTheme.typography.labelMedium, color = colors.warning)
            }
            itens.forEach { item ->
                Spacer(Modifier.height(dimens.space1))
                Text("• $item", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/**
 * [aoEntrar] só vem preenchido quando o erro é falta de sessão: a saída fica no
 * próprio aviso, e não numa instrução para ir procurar a tela de acesso.
 */
@Composable
private fun BannerErro(
    mensagem: String,
    aoDescartar: () -> Unit,
    aoEntrar: (() -> Unit)? = null,
) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    Surface(color = colors.dangerPale, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = dimens.screenMargin, vertical = dimens.cardPaddingV),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(ObiterIcones.Erro, contentDescription = null, tint = colors.danger, modifier = Modifier.size(dimens.iconStarSize))
            Spacer(Modifier.width(dimens.space1))
            Text(mensagem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (aoEntrar != null) {
                Button(onClick = aoEntrar) { Text(stringResource(R.string.minuta_erro_entrar)) }
                Spacer(Modifier.width(dimens.space1))
            }
            TextButton(onClick = aoDescartar) { Text(stringResource(R.string.minuta_erro_ok)) }
        }
    }
}

@Composable
private fun CabecalhoSecao(titulo: String) {
    Text(
        text = titulo.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = ObiterTheme.colors.accent,
        modifier = Modifier.padding(top = ObiterTheme.dimens.space1),
    )
}

@Composable
private fun BadgeConfianca(confianca: Double) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val (cor, fundo) = when {
        confianca >= 0.75 -> colors.success to colors.successPale
        confianca >= 0.5 -> colors.warning to colors.warningPale
        else -> colors.danger to colors.dangerPale
    }
    Box(
        modifier = Modifier
            .background(fundo, RoundedCornerShape(dimens.badgeRadius))
            .padding(horizontal = dimens.badgePaddingH, vertical = dimens.badgePaddingV),
    ) {
        Text(
            text = "${(confianca * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = cor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LinhaChips(conteudo: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ObiterTheme.dimens.chipRowGap),
        verticalArrangement = Arrangement.spacedBy(ObiterTheme.dimens.space1),
        content = conteudo,
    )
}

@Composable
private fun ChipSelecionavel(rotulo: String, selecionado: Boolean, onClick: () -> Unit) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    val corBorda = if (selecionado) colors.accent else colors.divider
    val corFundo = if (selecionado) colors.accentPale else Color.Transparent
    val corTexto = if (selecionado) colors.accent else colors.textMuted
    Box(
        modifier = Modifier
            .background(corFundo, RoundedCornerShape(dimens.chipRadius))
            .border(dimens.borderWidth, corBorda, RoundedCornerShape(dimens.chipRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.chipPaddingH, vertical = dimens.badgePaddingV),
    ) {
        Text(rotulo, style = MaterialTheme.typography.labelMedium, color = corTexto)
    }
}

@Composable
private fun ChipEstatico(rotulo: String) {
    val dimens = ObiterTheme.dimens
    val colors = ObiterTheme.colors
    Box(
        modifier = Modifier
            .background(colors.primaryPale, RoundedCornerShape(dimens.chipRadius))
            .padding(horizontal = dimens.chipPaddingH, vertical = dimens.badgePaddingV),
    ) {
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = colors.primaryDark)
    }
}

@Composable
private fun tituloEtapa(etapa: EtapaMinuta): String = stringResource(
    when (etapa) {
        EtapaMinuta.LISTA -> R.string.minuta_titulo_lista
        EtapaMinuta.UPLOAD -> R.string.minuta_titulo_upload
        EtapaMinuta.EXTRAINDO -> R.string.minuta_titulo_extraindo
        EtapaMinuta.REVISAR -> R.string.minuta_titulo_revisar
        EtapaMinuta.CONFIGURAR -> R.string.minuta_titulo_configurar
        EtapaMinuta.GERANDO -> R.string.minuta_titulo_gerando
        EtapaMinuta.RESULTADO -> R.string.minuta_titulo_resultado
    },
)

@Composable
private fun rotuloStage(stage: String): String = stringResource(
    when (stage) {
        "conversion" -> R.string.minuta_stage_conversion
        "semantic_analysis" -> R.string.minuta_stage_semantic
        "validation" -> R.string.minuta_stage_validation
        "packaging" -> R.string.minuta_stage_packaging
        "done" -> R.string.minuta_stage_done
        else -> R.string.minuta_stage_default
    },
)
