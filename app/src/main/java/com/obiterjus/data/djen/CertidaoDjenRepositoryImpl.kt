package com.obiterjus.data.djen

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.obiterjus.core.config.AppConfigRepository
import com.obiterjus.data.djen.remote.DjenRetrofitFactory
import com.obiterjus.domain.repository.CertidaoDjenRepository
import java.io.File

class CertidaoDjenRepositoryImpl(
    private val context: Context,
    private val appConfigRepository: AppConfigRepository,
) : CertidaoDjenRepository {
    override suspend fun obterCertidao(hash: String): Uri {
        val safeHash = hash.trim().takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("Hash da certidão não informado.")
        val file = File(certidoesDir(), "certidao-$safeHash.pdf")
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

    private fun certidoesDir(): File =
        File(context.cacheDir, CERTIDOES_DIR).also(File::mkdirs)

    private companion object {
        const val CERTIDOES_DIR = "certidoes-djen"
    }
}
