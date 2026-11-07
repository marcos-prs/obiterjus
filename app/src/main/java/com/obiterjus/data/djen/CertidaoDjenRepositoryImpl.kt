package com.obiterjus.data.djen

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.data.djen.remote.DjenRetrofitFactory
import com.obiterjus.domain.repository.CertidaoDjenRepository
import java.io.File
import java.time.Duration

class CertidaoDjenRepositoryImpl(
    private val context: Context,
    private val appConfigRepository: AppConfigRepository,
) : CertidaoDjenRepository {
    override suspend fun obterCertidao(hash: String): Uri {
        val safeHash = hash.trim().takeIf(HASH_VALIDO::matches)
            ?: throw IllegalArgumentException("Hash da certidão inválido.")
        val file = File(diretorio(context).also(File::mkdirs), "certidao-$safeHash.pdf")
        if (!file.exists() || file.length() == 0L) {
            baixarCertidao(safeHash, file)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    private suspend fun baixarCertidao(hash: String, destino: File) {
        val config = appConfigRepository.refresh()
        val responseBody = DjenRetrofitFactory.createApi(
            baseUrl = config.djenBaseUrl,
            timeoutSeconds = config.requestTimeoutSeconds,
        ).baixarCertidao(hash)

        destino.parentFile?.mkdirs()
        destino.outputStream().use { output ->
            responseBody.byteStream().use { input ->
                input.copyTo(output)
            }
        }
    }

    companion object {
        private const val CERTIDOES_DIR = "certidoes-djen"

        /**
         * O hash vem da resposta do DJEN e entra num nome de arquivo, então não
         * pode conter separador de caminho — `../` escreveria fora do cache.
         */
        private val HASH_VALIDO = Regex("^[A-Za-z0-9_-]{1,128}$")

        private val TTL_PADRAO: Duration = Duration.ofHours(24)

        fun diretorio(context: Context): File = File(context.cacheDir, CERTIDOES_DIR)

        /**
         * Certidão é peça processual em claro no disco. Some depois de
         * [maxIdade]; com [Duration.ZERO] limpa tudo (usado na saída da conta).
         */
        fun limparCache(context: Context, maxIdade: Duration = TTL_PADRAO) {
            val limite = System.currentTimeMillis() - maxIdade.toMillis()
            diretorio(context).listFiles()?.forEach { arquivo ->
                if (arquivo.lastModified() <= limite) arquivo.delete()
            }
        }
    }
}
