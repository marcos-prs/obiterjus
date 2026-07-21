package com.obiterjus.presentation.detalhepublicacao

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.R
import com.obiterjus.core.parser.NumeroProcessoNormalizer
import com.obiterjus.core.time.CalculadoraPrazos
import com.obiterjus.core.time.FormatadorData
import com.obiterjus.core.time.ResultadoCalculoPrazo
import com.obiterjus.data.agenda.local.PrazoSugeridoDao
import com.obiterjus.data.agenda.local.PrazoSugeridoEntity
import com.obiterjus.domain.model.ConfiancaCalculo
import com.obiterjus.domain.model.ConfirmacaoPrazoResultado
import com.obiterjus.domain.model.ParticipanteProcesso
import com.obiterjus.domain.model.ProcessoMonitorado
import com.obiterjus.domain.model.ProvedorCalendario
import com.obiterjus.domain.model.Publicacao
import com.obiterjus.domain.model.PublicacaoParticipante
import com.obiterjus.domain.model.PublicacaoPrazo
import com.obiterjus.domain.usecase.CadastrarPrazoManualUC
import com.obiterjus.domain.usecase.ObservarProcessos
import com.obiterjus.domain.usecase.ObterPublicacaoPorId
import com.obiterjus.domain.usecase.ResolverNaturezaProcessoUC
import com.obiterjus.domain.usecase.aplicarConfirmacao
import com.obiterjus.presentation.prazos.TextosPrazos
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.obiterjus.presentation.participantes.resolverPartesProcesso
import com.obiterjus.presentation.participantes.resolverPartesPublicacao
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DetalhePublicacaoViewModel internal constructor(
    private val obterPublicacaoPorId: ObterPublicacaoPorId,
    private val observarProcessos: ObservarProcessos,
    private val prazoSugeridoDao: PrazoSugeridoDao,
    private val calculadoraPrazos: CalculadoraPrazos,
    private val resolverNaturezaProcessoUC: ResolverNaturezaProcessoUC,
    private val cadastrarPrazoManualUC: CadastrarPrazoManualUC,
    private val textos: TextosPrazos,
) : ViewModel() {

    constructor(
        context: Context,
        obterPublicacaoPorId: ObterPublicacaoPorId,
        observarProcessos: ObservarProcessos,
        prazoSugeridoDao: PrazoSugeridoDao,
        calculadoraPrazos: CalculadoraPrazos,
        resolverNaturezaProcessoUC: ResolverNaturezaProcessoUC,
        cadastrarPrazoManualUC: CadastrarPrazoManualUC,
    ) : this(
        obterPublicacaoPorId = obterPublicacaoPorId,
        observarProcessos = observarProcessos,
        prazoSugeridoDao = prazoSugeridoDao,
        calculadoraPrazos = calculadoraPrazos,
        resolverNaturezaProcessoUC = resolverNaturezaProcessoUC,
        cadastrarPrazoManualUC = cadastrarPrazoManualUC,
        textos = object : TextosPrazos {
            override fun get(resId: Int): String = context.getString(resId)
            override fun get(resId: Int, vararg args: Any): String =
                context.getString(resId, *args)
        },
    )

    private val publicacaoId = MutableStateFlow<Long?>(null)

    private val fluxoCadastroPrazo =
        MutableStateFlow<FluxoCadastroPrazo>(FluxoCadastroPrazo.Fechado)
    val fluxoCadastro: StateFlow<FluxoCadastroPrazo> = fluxoCadastroPrazo.asStateFlow()

    private val resultadoCadastro = MutableStateFlow<ConfirmacaoPrazoResultado?>(null)
    val resultadoCadastroPrazo: StateFlow<ConfirmacaoPrazoResultado?> =
        resultadoCadastro.asStateFlow()

    private var ultimaSolicitacaoCadastro: SolicitacaoCadastroPrazo? = null

    val estado: StateFlow<EstadoDetalhePublicacao> = publicacaoId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(EstadoDetalhePublicacao(estaCarregando = true))
            } else {
                combine(
                    obterPublicacaoPorId(id),
                    observarProcessos(),
                    prazoSugeridoDao.observeByPublicacaoId(id),
                ) { publicacao, processos, prazoSugerido ->
                    if (publicacao == null) {
                        EstadoDetalhePublicacao(
                            estaCarregando = false,
                            naoEncontrada = publicacaoId.value != null,
                        )
                    } else {
                        mapParaEstado(publicacao, processos, prazoSugerido)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EstadoDetalhePublicacao(estaCarregando = true),
        )

    fun aoCarregar(id: Long) {
        if (publicacaoId.value != id) {
            // ViewModel compartilhado entre navegações: descarta fluxo anterior
            fluxoCadastroPrazo.value = FluxoCadastroPrazo.Fechado
            resultadoCadastro.value = null
            ultimaSolicitacaoCadastro = null
        }
        publicacaoId.value = id
    }

    fun aoAbrirCadastroPrazo() {
        val prazoAtual = estado.value.prazoAtual
        fluxoCadastroPrazo.value = FluxoCadastroPrazo.Selecionando(
            quantidadeDias = (prazoAtual?.quantidade ?: QUANTIDADE_PADRAO)
                .coerceIn(DIAS_MINIMO, DIAS_MAXIMO),
            diasUteis = prazoAtual?.diasUteis ?: true,
        )
    }

    fun aoAlterarSelecao(quantidadeDias: Int, diasUteis: Boolean) {
        val atual = fluxoCadastroPrazo.value
        if (atual is FluxoCadastroPrazo.Selecionando) {
            fluxoCadastroPrazo.value = atual.copy(
                quantidadeDias = quantidadeDias.coerceIn(DIAS_MINIMO, DIAS_MAXIMO),
                diasUteis = diasUteis,
            )
        }
    }

    fun aoCalcular() {
        val selecao = when (val atual = fluxoCadastroPrazo.value) {
            is FluxoCadastroPrazo.Selecionando ->
                atual.quantidadeDias to atual.diasUteis
            is FluxoCadastroPrazo.ErroCalculo ->
                atual.quantidadeDias to atual.diasUteis
            else -> return
        }
        val (quantidadeDias, diasUteis) = selecao
        val estadoAtual = estado.value
        val dataBase = estadoAtual.dataExpediente ?: return

        fluxoCadastroPrazo.value = FluxoCadastroPrazo.Calculando(quantidadeDias, diasUteis)

        viewModelScope.launch {
            val natureza = resolverNaturezaProcessoUC(estadoAtual.numeroProcesso)
            val resultado = calculadoraPrazos.calcularDataLimite(
                dataBase = dataBase,
                quantidade = quantidadeDias,
                unidade = CadastrarPrazoManualUC.UNIDADE_DIAS,
                diasUteis = diasUteis,
                tribunal = estadoAtual.tribunal,
                natureza = natureza,
            )
            fluxoCadastroPrazo.value = when (resultado) {
                is ResultadoCalculoPrazo.Confiavel -> FluxoCadastroPrazo.Resultado(
                    quantidadeDias = quantidadeDias,
                    diasUteis = diasUteis,
                    dataCalculada = resultado.data,
                    confianca = ConfiancaCalculo.CONFIAVEL,
                )

                is ResultadoCalculoPrazo.Incerto -> {
                    val data = resultado.data
                    if (data != null) {
                        FluxoCadastroPrazo.Resultado(
                            quantidadeDias = quantidadeDias,
                            diasUteis = diasUteis,
                            dataCalculada = data,
                            confianca = ConfiancaCalculo.INCERTO,
                        )
                    } else {
                        // BLOQUEADO_* sem data: a API recusou a combinação
                        // (ex.: prazo penal declarado em dias úteis)
                        erroCalculo(quantidadeDias, diasUteis, bloqueadoPelaApi = true)
                    }
                }

                ResultadoCalculoPrazo.Pendente -> erroCalculo(quantidadeDias, diasUteis)
            }
        }
    }

    fun aoConfirmarData() {
        val atual = fluxoCadastroPrazo.value
        if (atual is FluxoCadastroPrazo.Resultado) {
            fluxoCadastroPrazo.value = FluxoCadastroPrazo.EscolhendoProvedor(
                quantidadeDias = atual.quantidadeDias,
                diasUteis = atual.diasUteis,
                dataCalculada = atual.dataCalculada,
                confianca = atual.confianca,
            )
        }
    }

    fun aoVoltarParaSelecao() {
        val atual = fluxoCadastroPrazo.value
        val (quantidadeDias, diasUteis) = when (atual) {
            is FluxoCadastroPrazo.Resultado -> atual.quantidadeDias to atual.diasUteis
            is FluxoCadastroPrazo.ErroCalculo -> atual.quantidadeDias to atual.diasUteis
            is FluxoCadastroPrazo.EscolhendoProvedor -> atual.quantidadeDias to atual.diasUteis
            else -> return
        }
        fluxoCadastroPrazo.value = FluxoCadastroPrazo.Selecionando(quantidadeDias, diasUteis)
    }

    fun aoConfirmarProvedor(provedor: ProvedorCalendario) {
        val atual = fluxoCadastroPrazo.value as? FluxoCadastroPrazo.EscolhendoProvedor ?: return
        val id = publicacaoId.value ?: return
        cadastrar(
            SolicitacaoCadastroPrazo(
                publicacaoId = id,
                quantidadeDias = atual.quantidadeDias,
                diasUteis = atual.diasUteis,
                dataCalculada = atual.dataCalculada,
                confianca = atual.confianca,
                provedor = provedor,
            ),
        )
    }

    fun aoRepetirUltimoCadastro() {
        ultimaSolicitacaoCadastro?.let(::cadastrar)
    }

    fun aoFecharCadastro() {
        // Enquanto salva, o fechamento fica bloqueado para não perder feedback
        if (fluxoCadastroPrazo.value !is FluxoCadastroPrazo.Salvando) {
            fluxoCadastroPrazo.value = FluxoCadastroPrazo.Fechado
        }
    }

    fun aoConsumirResultadoCadastro() {
        resultadoCadastro.value = null
    }

    private fun cadastrar(solicitacao: SolicitacaoCadastroPrazo) {
        ultimaSolicitacaoCadastro = solicitacao
        fluxoCadastroPrazo.value = FluxoCadastroPrazo.Salvando(
            quantidadeDias = solicitacao.quantidadeDias,
            diasUteis = solicitacao.diasUteis,
            dataCalculada = solicitacao.dataCalculada,
            confianca = solicitacao.confianca,
        )

        viewModelScope.launch {
            val estadoAtual = estado.value
            val numeroProcesso = estadoAtual.numeroProcesso
                .takeIf { it.isNotBlank() }
                ?: textos.get(R.string.prazos_sem_processo)

            val resultado = cadastrarPrazoManualUC.invoke(
                publicacaoId = solicitacao.publicacaoId,
                quantidade = solicitacao.quantidadeDias,
                diasUteis = solicitacao.diasUteis,
                dataLimite = solicitacao.dataCalculada,
                confianca = solicitacao.confianca,
                title = textos.get(R.string.prazos_confirmacao_titulo, numeroProcesso),
                description = buildDescricaoCadastro(estadoAtual, solicitacao),
                provedor = solicitacao.provedor,
            )

            fluxoCadastroPrazo.value = FluxoCadastroPrazo.Fechado
            resultadoCadastro.value = resultado.getOrNull() ?: ConfirmacaoPrazoResultado.Falha
        }
    }

    private fun erroCalculo(
        quantidadeDias: Int,
        diasUteis: Boolean,
        bloqueadoPelaApi: Boolean = false,
    ): FluxoCadastroPrazo.ErroCalculo =
        FluxoCadastroPrazo.ErroCalculo(
            quantidadeDias = quantidadeDias,
            diasUteis = diasUteis,
            tribunalAusente = estado.value.tribunal.isNullOrBlank(),
            bloqueadoPelaApi = bloqueadoPelaApi,
        )

    private fun buildDescricaoCadastro(
        estado: EstadoDetalhePublicacao,
        solicitacao: SolicitacaoCadastroPrazo,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Processo: ${estado.numeroProcesso.ifBlank { "Não informado" }}")
        sb.appendLine(
            "Prazo: ${solicitacao.quantidadeDias} ${CadastrarPrazoManualUC.UNIDADE_DIAS}" +
                " — ${CadastrarPrazoManualUC.TEXTO_PRAZO_MANUAL}",
        )
        sb.appendLine("Data limite: ${FormatadorData.formatarData(solicitacao.dataCalculada)}")
        sb.appendLine("Tipo do ato: ${estado.nomeAto.ifBlank { "Não informado" }}")
        val conteudo = estado.conteudoCompleto.trim()
        if (conteudo.isNotBlank()) {
            sb.appendLine()
            sb.append(conteudo)
        }
        return sb.toString()
    }

    private fun mapParaEstado(
        publicacao: Publicacao,
        processos: List<ProcessoMonitorado>,
        prazoSugerido: PrazoSugeridoEntity?,
    ): EstadoDetalhePublicacao {
        val processoDataJud = NumeroProcessoNormalizer.normalize(publicacao.numeroProcesso)
            ?.let { numero ->
                processos.firstOrNull { it.numeroProcesso == numero }
            }

        val partesDataJud = processoDataJud?.participantes?.resolverPartesProcesso()
        val partesPublicacao = publicacao.participantes.resolverPartesPublicacao()

        val parteAtiva = partesDataJud?.ativa ?: partesPublicacao.ativa
        val partePassiva = partesDataJud?.passiva ?: partesPublicacao.passiva

        return EstadoDetalhePublicacao(
            estaCarregando = false,
            numeroProcesso = publicacao.numeroProcesso.orEmpty(),
            parteAtivaNome = parteAtiva?.nomes?.joinToString("\n"),
            parteAtivaTipo = parteAtiva?.tipo,
            partePassivaNome = partePassiva?.nomes?.joinToString("\n"),
            partePassivaTipo = partePassiva?.tipo,
            advogados = partesPublicacao.advogados,
            dataExpediente = publicacao.dataDisponibilizacao,
            nomeAto = publicacao.tipoComunicacao.orEmpty(),
            conteudoCompleto = publicacao.textoLimpo.orEmpty(),
            tribunal = publicacao.tribunal,
            prazoAtual = publicacao.prazo?.aplicarConfirmacao(prazoSugerido)
                ?: prazoSugerido?.toPublicacaoPrazo(),
            podeCadastrarPrazo = publicacao.dataDisponibilizacao != null,
        )
    }

    private fun PrazoSugeridoEntity.toPublicacaoPrazo(): PublicacaoPrazo =
        PublicacaoPrazo(
            quantidade = quantidade,
            unidade = unidade,
            diasUteis = diasUteis,
            textoOriginal = textoOriginal,
            dataLimiteEstimada = dataLimite,
            isConfirmado = isConfirmado,
            idExternoCalendario = idExternoCalendario,
            provedorCalendario = provedorCalendario,
        )

    private companion object {
        const val QUANTIDADE_PADRAO = 15
        const val DIAS_MINIMO = 1
        const val DIAS_MAXIMO = 120
    }
}

data class EstadoDetalhePublicacao(
    val estaCarregando: Boolean = false,
    val naoEncontrada: Boolean = false,
    val numeroProcesso: String = "",
    val parteAtivaNome: String? = null,
    val parteAtivaTipo: String? = null,
    val partePassivaNome: String? = null,
    val partePassivaTipo: String? = null,
    val advogados: List<String> = emptyList(),
    val dataExpediente: LocalDate? = null,
    val nomeAto: String = "",
    val conteudoCompleto: String = "",
    val tribunal: String? = null,
    val prazoAtual: PublicacaoPrazo? = null,
    val podeCadastrarPrazo: Boolean = false,
)

/** Estados do bottom sheet de cadastro manual de prazo. */
sealed interface FluxoCadastroPrazo {
    data object Fechado : FluxoCadastroPrazo

    data class Selecionando(
        val quantidadeDias: Int,
        val diasUteis: Boolean,
    ) : FluxoCadastroPrazo

    data class Calculando(
        val quantidadeDias: Int,
        val diasUteis: Boolean,
    ) : FluxoCadastroPrazo

    data class Resultado(
        val quantidadeDias: Int,
        val diasUteis: Boolean,
        val dataCalculada: LocalDate,
        val confianca: ConfiancaCalculo,
    ) : FluxoCadastroPrazo

    data class ErroCalculo(
        val quantidadeDias: Int,
        val diasUteis: Boolean,
        val tribunalAusente: Boolean,
        val bloqueadoPelaApi: Boolean = false,
    ) : FluxoCadastroPrazo

    data class EscolhendoProvedor(
        val quantidadeDias: Int,
        val diasUteis: Boolean,
        val dataCalculada: LocalDate,
        val confianca: ConfiancaCalculo,
    ) : FluxoCadastroPrazo

    data class Salvando(
        val quantidadeDias: Int,
        val diasUteis: Boolean,
        val dataCalculada: LocalDate,
        val confianca: ConfiancaCalculo,
    ) : FluxoCadastroPrazo
}

private data class SolicitacaoCadastroPrazo(
    val publicacaoId: Long,
    val quantidadeDias: Int,
    val diasUteis: Boolean,
    val dataCalculada: LocalDate,
    val confianca: ConfiancaCalculo,
    val provedor: ProvedorCalendario,
)
