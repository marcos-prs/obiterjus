package com.obiterjus.domain.repository

import android.net.Uri

interface CertidaoDjenRepository {
    suspend fun obterCertidao(hash: String): Uri
}
