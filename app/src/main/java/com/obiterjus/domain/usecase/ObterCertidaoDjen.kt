package com.obiterjus.domain.usecase

import android.net.Uri
import com.obiterjus.domain.repository.CertidaoDjenRepository

class ObterCertidaoDjen(
    private val repository: CertidaoDjenRepository,
) {
    suspend operator fun invoke(hash: String): Uri = repository.obterCertidao(hash)
}
