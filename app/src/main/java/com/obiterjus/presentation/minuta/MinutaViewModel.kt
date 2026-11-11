package com.obiterjus.presentation.minuta

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obiterjus.domain.model.EstruturaPeca
import com.obiterjus.domain.model.MinutaPackage
import com.obiterjus.domain.model.MinutaSugestao
import com.obiterjus.domain.model.TipoMinuta
import com.obiterjus.domain.model.TipoPecaOrigem
import com.obiterjus.domain.repository.AuthRepository
import com.obiterjus.domain.usecase.ExcluirMinutaUC
import com.obiterjus.domain.usecase.ExtrairPecaUC
import com.obiterjus.domain.usecase.ObservarMinutasDoProcesso
import com.obiterjus.domain.usecase.ObterMinutaUC
import com.obiterjus.domain.usecase.ObterResultadoExtracaoUC
import com.obiterjus.domain.usecase.SugerirMinutaUC
import java.net.SocketTimeoutException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Passos do assistente de geração de minuta. */
enum class EtapaMinuta {
    LISTA,
    UPLOAD,
    EXTRAINDO,
    REVISAR,
    CONFIGURAR,
    GERANDO,
    RESULTADO,
}

data class EstadoMinuta(
    val numeroProcesso: String = "",
    val etapa: EtapaMinuta = EtapaMinuta.LISTA,
    val minutasSalvas: List<MinutaSugestao> = emptyList(),
    val carregandoLista: Boolean = true,
    val arquivoUri: Uri? = null,
    val arquivoNome: String? = null,
    val tipoDeclarado: TipoPecaOrigem = TipoPecaOrigem.INICIAL,
    val progressoStatus: String? = null,
    val progressoStage: String? = null,
    val progressoPct: Int = 0,
    val pacote: MinutaPackage? = null,
    val estrutura: EstruturaPeca? = null,
    val tipoMinuta: TipoMinuta = TipoMinuta.CONTESTACAO,
    val orientacoes: String = "",
    val sugestao: MinutaSugestao? = null,
    val erro: String? = null,
    /**
     * O erro em cartaz é falta de sessão (ou token recusado). A tela usa isso
     * para oferecer "Entrar" no próprio aviso, em vez de só mandar o advogado
     * procurar o Perfil.
     */
    val erroExigeLogin: Boolean = false,
)

class MinutaViewModel(
    private val context: Context,
    private val extrairPecaUC: ExtrairPecaUC,
    private val obterResultadoExtracaoUC: ObterResultadoExtracaoUC,
    private val sugerirMinutaUC: SugerirMinutaUC,
    private val observarMinutasDoProcesso: ObservarMinutasDoProcesso,
    private val obterMinutaUC: ObterMinutaUC,
    private val excluirMinutaUC: ExcluirMinutaUC,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoMinuta())
    val estado: StateFlow<EstadoMinuta> = _estado.asStateFlow()

    private var observando = false

    fun aoCarregar(numeroProcesso: String) {
        if (observando) return
        observando = true
        _estado.update { it.copy(numeroProcesso = numeroProcesso) }
        viewModelScope.launch {
            observarMinutasDoProcesso(numeroProcesso).collect { lista ->
                _estado.update { it.copy(minutasSalvas = lista, carregandoLista = false) }
            }
        }
    }

    /* ---------- Navegação entre passos ---------- */

    fun iniciarNova() {
        _estado.update {
            it.copy(
                etapa = EtapaMinuta.UPLOAD,
                arquivoUri = null,
                arquivoNome = null,
                pacote = null,
                estrutura = null,
                sugestao = null,
                orientacoes = "",
                progressoPct = 0,
                progressoStage = null,
                progressoStatus = null,
                erro = null, erroExigeLogin = false,
            )
        }
    }

    fun voltarParaLista() {
        _estado.update { it.copy(etapa = EtapaMinuta.LISTA, erro = null, erroExigeLogin = false) }
    }

    fun irParaConfigurar() {
        _estado.update { it.copy(etapa = EtapaMinuta.CONFIGURAR, erro = null, erroExigeLogin = false) }
    }

    /** Substitui a estrutura em revisão pela versão editada pelo advogado. */
    fun aoAtualizarEstrutura(nova: EstruturaPeca) {
        _estado.update { it.copy(estrutura = nova) }
    }

    fun voltarParaRevisar() {
        _estado.update { it.copy(etapa = EtapaMinuta.REVISAR, erro = null, erroExigeLogin = false) }
    }

    fun descartarErro() {
        _estado.update { it.copy(erro = null, erroExigeLogin = false) }
    }

    /* ---------- Upload ---------- */

    fun aoSelecionarArquivo(uri: Uri) {
        val (nome, tamanho) = resolverArquivo(uri)
        // O PDF é lido inteiro em memória para o multipart; sem teto, uma peça
        // digitalizada grande derruba o app antes de o servidor recusar.
        if (tamanho > MAX_PDF_BYTES) {
            _estado.update { it.copy(erro = MSG_PDF_GRANDE, erroExigeLogin = false) }
            return
        }
        _estado.update { it.copy(arquivoUri = uri, arquivoNome = nome, erro = null, erroExigeLogin = false) }
    }

    fun aoSelecionarTipoDeclarado(tipo: TipoPecaOrigem) {
        _estado.update { it.copy(tipoDeclarado = tipo) }
    }

    fun extrair() {
        val estadoAtual = _estado.value
        val uri = estadoAtual.arquivoUri ?: return
        viewModelScope.launch {
            if (semSessao()) {
                _estado.update { it.copy(erro = MSG_SEM_SESSAO, erroExigeLogin = true) }
                return@launch
            }
            _estado.update {
                it.copy(etapa = EtapaMinuta.EXTRAINDO, erro = null, erroExigeLogin = false, progressoPct = 0, progressoStage = null)
            }
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("Não foi possível abrir o arquivo selecionado.")
                var jobId: String? = null
                extrairPecaUC(
                    pdf = input,
                    nomeArquivo = estadoAtual.arquivoNome ?: "peca.pdf",
                    tipoDeclarado = estadoAtual.tipoDeclarado.codigo,
                    numeroProcesso = estadoAtual.numeroProcesso.ifBlank { null },
                ).collect { progresso ->
                    jobId = progresso.jobId
                    _estado.update {
                        it.copy(
                            progressoStatus = progresso.status,
                            progressoStage = progresso.stage,
                            progressoPct = progresso.progressoPct,
                        )
                    }
                }
                val idJob = jobId ?: error("Job de extração sem identificador.")
                val pacote = obterResultadoExtracaoUC(idJob)
                _estado.update {
                    it.copy(etapa = EtapaMinuta.REVISAR, pacote = pacote, estrutura = pacote.estrutura)
                }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(
                        etapa = EtapaMinuta.UPLOAD,
                        erro = mensagemErro(e, "Falha ao extrair a peça."),
                        erroExigeLogin = exigeLogin(e),
                    )
                }
            }
        }
    }

    /* ---------- Configuração e geração ---------- */

    fun aoSelecionarTipoMinuta(tipo: TipoMinuta) {
        _estado.update { it.copy(tipoMinuta = tipo) }
    }

    fun aoEditarOrientacoes(texto: String) {
        _estado.update { it.copy(orientacoes = texto.take(MAX_ORIENTACOES)) }
    }

    fun gerar() {
        val estadoAtual = _estado.value
        val estrutura = estadoAtual.estrutura ?: return
        // A API redige a partir do texto integral da peça (o markdown da
        // extração), não do resumo. Sem ele não há como gerar.
        val pacote = estadoAtual.pacote
        if (pacote == null || pacote.markdown.isBlank()) {
            _estado.update {
                it.copy(erro = "Texto da peça de origem indisponível. Refaça a extração.", erroExigeLogin = false)
            }
            return
        }
        viewModelScope.launch {
            if (semSessao()) {
                _estado.update { it.copy(erro = MSG_SEM_SESSAO, erroExigeLogin = true) }
                return@launch
            }
            _estado.update { it.copy(etapa = EtapaMinuta.GERANDO, erro = null, erroExigeLogin = false) }
            try {
                val sugestao = sugerirMinutaUC(
                    tipoMinuta = estadoAtual.tipoMinuta,
                    tipoPecaOrigem = pacote.tipoDeclarado,
                    textoOrigem = pacote.markdown,
                    estrutura = estrutura,
                    numeroProcesso = estadoAtual.numeroProcesso.ifBlank { null },
                    vara = pacote.vara,
                    orientacoes = estadoAtual.orientacoes.ifBlank { null },
                )
                _estado.update { it.copy(etapa = EtapaMinuta.RESULTADO, sugestao = sugestao) }
            } catch (e: Exception) {
                _estado.update {
                    it.copy(
                        etapa = EtapaMinuta.CONFIGURAR,
                        erro = mensagemErro(e, "Falha ao gerar a minuta."),
                        erroExigeLogin = exigeLogin(e),
                    )
                }
            }
        }
    }

    /* ---------- Minutas salvas ---------- */

    fun abrirMinutaSalva(sugestaoId: String) {
        viewModelScope.launch {
            val sugestao = obterMinutaUC(sugestaoId) ?: return@launch
            _estado.update { it.copy(etapa = EtapaMinuta.RESULTADO, sugestao = sugestao) }
        }
    }

    fun excluirMinuta(sugestaoId: String) {
        viewModelScope.launch { excluirMinutaUC(sugestaoId) }
    }

    /**
     * A API de minutas exige um Firebase ID token e o login anônimo está desabilitado
     * no projeto. Sem sessão (currentUser nulo/anônimo) a chamada voltaria 401
     * "Not authenticated"; bloqueamos antes e orientamos o advogado a entrar.
     */
    private suspend fun semSessao(): Boolean {
        val usuario = authRepository.currentUser.firstOrNull()
        return usuario == null || usuario.isAnonymous
    }

    /**
     * 401 da API de minutas: ou nenhum token foi enviado, ou o que foi enviado
     * já não vale. Nos dois casos o caminho de saída é entrar na conta.
     */
    private fun exigeLogin(e: Throwable): Boolean = e is HttpException && e.code() == 401

    private fun mensagemErro(e: Throwable, padrao: String): String = when {
        e is HttpException -> {
            // O detalhe do servidor distingue os dois 401 da API (FastAPI):
            //  - "Not authenticated"          -> nenhum token foi enviado (cliente)
            //  - "Token invalido ou expirado" -> token enviado mas recusado (servidor)
            val detalhe = detalheDoServidor(e)
            val base = when (e.code()) {
                401 -> "Sessão inválida ou expirada. Verifique se você está autenticado e tente novamente."
                413 -> "PDF grande demais para o servidor."
                422 -> "A peça enviada não pôde ser interpretada. Confira o arquivo."
                502, 503 -> "O servidor de IA está indisponível no momento. Tente novamente em instantes."
                else -> "Erro do servidor (HTTP ${e.code()})."
            }
            if (detalhe != null) "$base ($detalhe)" else base
        }
        e is SocketTimeoutException -> "Tempo de resposta esgotado. Tente novamente."
        else -> e.message ?: padrao
    }

    /** Extrai o campo `detail` do corpo de erro do FastAPI para diagnóstico. */
    private fun detalheDoServidor(e: HttpException): String? = runCatching {
        val corpo = e.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: return null
        Regex("\"detail\"\\s*:\\s*\"([^\"]*)\"").find(corpo)?.groupValues?.get(1) ?: corpo.take(200)
    }.getOrNull()

    /** Nome e tamanho do arquivo escolhido, numa única consulta ao provider. */
    private fun resolverArquivo(uri: Uri): Pair<String, Long> {
        var nome = "peca.pdf"
        var tamanho = 0L
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idxNome = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idxNome >= 0) cursor.getString(idxNome)?.let { nome = it }
                    val idxTamanho = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idxTamanho >= 0 && !cursor.isNull(idxTamanho)) {
                        tamanho = cursor.getLong(idxTamanho)
                    }
                }
            }
        }
        return nome to tamanho
    }

    private companion object {
        const val MAX_ORIENTACOES = 4000
        const val MAX_PDF_BYTES = 20L * 1024 * 1024
        const val MSG_PDF_GRANDE =
            "O PDF passa de 20 MB. Reduza o arquivo ou envie apenas a peça a ser respondida."
        const val MSG_SEM_SESSAO =
            "A minuta é gerada no servidor do ObiterJus e precisa de uma conta. Entre para continuar."
    }
}
