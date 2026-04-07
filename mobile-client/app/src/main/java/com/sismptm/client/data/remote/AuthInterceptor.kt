package com.sismptm.client.data.remote

import com.sismptm.client.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")

        // Preferimos la sesión de Sprint 2 y usamos TokenManager como fallback.
        val token = SessionManager.accessToken.ifBlank { TokenManager.getAccessToken() }
        if (token.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
