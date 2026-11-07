package com.obiterjus.domain.usecase

import com.obiterjus.domain.model.ExtracaoProgresso
import com.obiterjus.domain.repository.MinutaRepository
import com.obiterjus.domain.repository.ProcessosRepository
import java.io.InputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Peça de processo em segredo de justiça não é enviada para fora do aparelho. */
class ProcessoSigilosoException : IllegalStateException(
    "Este processo corre em segredo de justiça. A peça não pode ser enviada para o serviço de minutas.",
)

/** Envia o PDF da peça e acompanha a extração, emitindo o progresso do polling. */
class ExtrairPecaUC(
    private val repositorioMinuta: MinutaRepository,
    private val repositorioProcessos: ProcessosRepository,
) {
    operator fun invoke(
        pdf: InputStream,
        nomeArquivo: String,
        tipoDeclarado: String,
        numeroProcesso: String? = null,
        vara: String? = null,
    ): Flow<ExtracaoProgresso> = flow {
        val nivelSigilo = numeroProcesso
            ?.let { repositorioProcessos.obterProcesso(it)?.nivelSigilo }
        if ((nivelSigilo ?: 0) > 0) {
            pdf.close()
            throw ProcessoSigilosoException()
        }
        emitAll(
            repositorioMinuta.extrairPeca(pdf, nomeArquivo, tipoDeclarado, numeroProcesso, vara),
        )
    }
}
