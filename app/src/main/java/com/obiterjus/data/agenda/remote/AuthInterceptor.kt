package com.obiterjus.data.agenda.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    // No cenário real, isso leria o token do repositório/AppAuth
    private val tokenProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val requestBuilder = chain.request().newBuilder()

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
